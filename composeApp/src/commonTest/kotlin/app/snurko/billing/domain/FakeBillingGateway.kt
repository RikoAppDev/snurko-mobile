package app.snurko.billing.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeBillingGateway : BillingGateway {

    private val updates = MutableSharedFlow<List<UnlockPurchase>>(replay = 1)

    val acknowledgedTokens = mutableListOf<String>()
    var launchCalls = 0
        private set
    var queryCalls = 0
        private set

    var owned: List<UnlockPurchase> = emptyList()
    var priceResult: Result<String, DataError.Billing> = Result.Success("€4.99")
    var queryResult: Result<List<UnlockPurchase>, DataError.Billing>? = null
    var acknowledgeResult: EmptyResult<DataError.Billing> = Result.Success(Unit)
    var launchResult: Result<List<UnlockPurchase>, DataError.Billing> =
        Result.Success(listOf(purchased(isAcknowledged = false)))

    suspend fun emitPurchases(purchases: List<UnlockPurchase>) {
        updates.emit(purchases)
    }

    override fun purchaseUpdates(): Flow<List<UnlockPurchase>> = updates

    override suspend fun unlockPrice(): Result<String, DataError.Billing> = priceResult

    override suspend fun queryPurchases(): Result<List<UnlockPurchase>, DataError.Billing> {
        queryCalls++
        return queryResult ?: Result.Success(owned)
    }

    override suspend fun acknowledge(purchaseToken: String): EmptyResult<DataError.Billing> {
        acknowledgedTokens += purchaseToken
        if (acknowledgeResult is Result.Success) {
            owned = owned.map { purchase ->
                if (purchase.token == purchaseToken) purchase.copy(isAcknowledged = true) else purchase
            }
        }
        return acknowledgeResult
    }

    override suspend fun launchPurchase(): Result<List<UnlockPurchase>, DataError.Billing> {
        launchCalls++
        return launchResult
    }
}

fun purchased(token: String = "token", isAcknowledged: Boolean = true) = UnlockPurchase(
    token = token,
    state = PurchaseState.PURCHASED,
    isAcknowledged = isAcknowledged,
)

fun pending(token: String = "token") = UnlockPurchase(
    token = token,
    state = PurchaseState.PENDING,
    isAcknowledged = false,
)
