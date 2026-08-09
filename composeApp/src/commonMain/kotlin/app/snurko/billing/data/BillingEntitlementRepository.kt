package app.snurko.billing.data

import app.snurko.billing.domain.BillingGateway
import app.snurko.billing.domain.EntitlementRepository
import app.snurko.billing.domain.EntitlementStore
import app.snurko.billing.domain.PurchaseOutcome
import app.snurko.billing.domain.PurchaseState
import app.snurko.billing.domain.UnlockPurchase
import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class BillingEntitlementRepository(
    private val gateway: BillingGateway,
    private val entitlementStore: EntitlementStore,
) : EntitlementRepository {

    override fun observeUnlocked(): Flow<Boolean> = entitlementStore.observeUnlocked()

    /** Follows the store for as long as the app lives, and re-checks it once on start. */
    fun start(scope: CoroutineScope) {
        gateway.purchaseUpdates()
            .onEach(::grant)
            .launchIn(scope)
        scope.launch { restore() }
    }

    override suspend fun unlockPrice(): Result<String, DataError.Billing> = gateway.unlockPrice()

    override suspend fun purchase(): Result<PurchaseOutcome, DataError.Billing> =
        when (val purchases = gateway.launchPurchase()) {
            is Result.Error -> purchases
            is Result.Success -> when {
                purchases.data.isEmpty() -> Result.Error(DataError.Billing.CANCELLED)
                purchases.data.acknowledgeAndOwn() -> {
                    entitlementStore.setUnlocked(true)
                    Result.Success(PurchaseOutcome.UNLOCKED)
                }

                else -> Result.Success(PurchaseOutcome.PENDING)
            }
        }

    override suspend fun restore(): EmptyResult<DataError.Billing> =
        when (val purchases = gateway.queryPurchases()) {
            is Result.Error -> purchases
            is Result.Success -> {
                val isUnlocked = purchases.data.acknowledgeAndOwn()
                entitlementStore.setUnlocked(isUnlocked)
                if (isUnlocked) Result.Success(Unit) else Result.Error(DataError.Billing.NOTHING_TO_RESTORE)
            }
        }

    private suspend fun grant(purchases: List<UnlockPurchase>) {
        if (purchases.acknowledgeAndOwn()) entitlementStore.setUnlocked(true)
    }

    /**
     * Acknowledges what the store has not been told about yet — an unacknowledged purchase
     * is refunded after three days — and answers whether any of it is paid for.
     */
    private suspend fun List<UnlockPurchase>.acknowledgeAndOwn(): Boolean {
        val owned = filter { it.state == PurchaseState.PURCHASED }
        owned.filterNot { it.isAcknowledged }.forEach { gateway.acknowledge(it.token) }
        return owned.isNotEmpty()
    }
}
