package app.snurko.billing.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeBillingGateway : BillingGateway {

    private val updates = MutableSharedFlow<List<BillingPurchase>>(replay = 1)
    private val prices = mutableMapOf<String, Result<String, DataError.Billing>>()
    private val launchResults =
        mutableMapOf<String, Result<List<BillingPurchase>, DataError.Billing>>()
    private val owned = mutableMapOf<String, List<BillingPurchase>>()
    private var purchaseCount = 0

    val acknowledgedTokens = mutableListOf<String>()
    val consumedTokens = mutableListOf<String>()
    val launchedProducts = mutableListOf<String>()
    val queriedProducts = mutableListOf<String>()

    var defaultPrice: Result<String, DataError.Billing> = Result.Success("€2.99")
    var acknowledgeResult: EmptyResult<DataError.Billing> = Result.Success(Unit)
    var consumeResult: EmptyResult<DataError.Billing> = Result.Success(Unit)

    fun setPrice(productId: String, price: Result<String, DataError.Billing>) {
        prices[productId] = price
    }

    fun setLaunchResult(
        productId: String,
        result: Result<List<BillingPurchase>, DataError.Billing>,
    ) {
        launchResults[productId] = result
    }

    fun setOwned(productId: String, purchases: List<BillingPurchase>) {
        owned[productId] = purchases
    }

    fun ownedOf(productId: String): List<BillingPurchase> = owned[productId].orEmpty()

    suspend fun emitPurchases(purchases: List<BillingPurchase>) {
        updates.emit(purchases)
    }

    override fun purchaseUpdates(productId: String): Flow<List<BillingPurchase>> = updates

    override suspend fun price(productId: String): Result<String, DataError.Billing> =
        prices[productId] ?: defaultPrice

    override suspend fun queryPurchases(
        productId: String,
    ): Result<List<BillingPurchase>, DataError.Billing> {
        queriedProducts += productId
        return Result.Success(ownedOf(productId))
    }

    override suspend fun acknowledge(purchaseToken: String): EmptyResult<DataError.Billing> {
        acknowledgedTokens += purchaseToken
        return acknowledgeResult
    }

    override suspend fun consume(purchaseToken: String): EmptyResult<DataError.Billing> {
        consumedTokens += purchaseToken
        if (consumeResult is Result.Success) {
            owned.keys.toList().forEach { productId ->
                owned[productId] = ownedOf(productId).filterNot { it.token == purchaseToken }
            }
        }
        return consumeResult
    }

    override suspend fun launchPurchase(
        productId: String,
    ): Result<List<BillingPurchase>, DataError.Billing> {
        launchedProducts += productId
        val result = launchResults[productId]
            ?: Result.Success(listOf(purchased(token = "$productId-${++purchaseCount}")))
        (result as? Result.Success)?.data?.let { owned[productId] = ownedOf(productId) + it }
        return result
    }
}

fun purchased(token: String = "token", isAcknowledged: Boolean = false) = BillingPurchase(
    token = token,
    state = PurchaseState.PURCHASED,
    isAcknowledged = isAcknowledged,
)

fun pending(token: String = "token") = BillingPurchase(
    token = token,
    state = PurchaseState.PENDING,
    isAcknowledged = false,
)
