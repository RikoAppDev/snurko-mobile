package app.snurko.billing.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeEntitlementRepository(isUnlocked: Boolean = false) : EntitlementRepository {

    private val unlocked = MutableStateFlow(isUnlocked)

    var purchaseCalls = 0
        private set
    var restoreCalls = 0
        private set

    var priceResult: Result<String, DataError.Billing> = Result.Success("€4.99")
    var purchaseResult: Result<PurchaseOutcome, DataError.Billing> =
        Result.Success(PurchaseOutcome.UNLOCKED)
    var restoreResult: EmptyResult<DataError.Billing> = Result.Success(Unit)

    /** Runs when [purchase] is called, so a test can let the store answer mid-flow. */
    var onPurchase: () -> Unit = {}

    fun setUnlocked(isUnlocked: Boolean) {
        unlocked.value = isUnlocked
    }

    override fun observeUnlocked(): Flow<Boolean> = unlocked

    override suspend fun unlockPrice(): Result<String, DataError.Billing> = priceResult

    override suspend fun purchase(): Result<PurchaseOutcome, DataError.Billing> {
        purchaseCalls++
        onPurchase()
        return purchaseResult
    }

    override suspend fun restore(): EmptyResult<DataError.Billing> {
        restoreCalls++
        return restoreResult
    }
}
