package app.snurko.billing.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import app.snurko.billing.domain.BillingGateway
import app.snurko.billing.domain.PurchaseState
import app.snurko.billing.domain.UNLOCK_PRODUCT_ID
import app.snurko.billing.domain.UnlockPurchase
import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private const val CONNECT_ATTEMPTS = 3
private const val FIRST_RETRY_DELAY_MILLIS = 500L

class PlayBillingGateway(
    context: Context,
) : BillingGateway {

    private val purchaseResults =
        MutableSharedFlow<Result<List<UnlockPurchase>, DataError.Billing>>(extraBufferCapacity = 8)
    private val connectionLock = Mutex()
    private var activity: Activity? = null

    private val listener = PurchasesUpdatedListener { billingResult, purchases ->
        purchaseResults.tryEmit(
            if (billingResult.isOk()) {
                Result.Success(purchases.orEmpty().unlockPurchases())
            } else {
                Result.Error(billingResult.toBillingError())
            },
        )
    }

    private val client = BillingClient.newBuilder(context)
        .setListener(listener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** The sheet needs a window to open in; only a foreground activity has one. */
    fun attach(activity: Activity) {
        this.activity = activity
    }

    fun detach() {
        activity = null
    }

    fun close() {
        client.endConnection()
    }

    override fun purchaseUpdates(): Flow<List<UnlockPurchase>> =
        purchaseResults.mapNotNull { (it as? Result.Success)?.data }

    override suspend fun unlockPrice(): Result<String, DataError.Billing> =
        when (val details = unlockProduct()) {
            is Result.Error -> details
            is Result.Success -> details.data.unlockOffer()?.formattedPrice
                ?.let { Result.Success(it) }
                ?: Result.Error(DataError.Billing.PRODUCT_UNAVAILABLE)
        }

    override suspend fun queryPurchases(): Result<List<UnlockPurchase>, DataError.Billing> {
        connect().let { if (it is Result.Error) return it }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = withContext(Dispatchers.IO) { client.queryPurchasesAsync(params) }
        return if (result.billingResult.isOk()) {
            Result.Success(result.purchasesList.unlockPurchases())
        } else {
            Result.Error(result.billingResult.toBillingError())
        }
    }

    override suspend fun acknowledge(purchaseToken: String): EmptyResult<DataError.Billing> {
        connect().let { if (it is Result.Error) return it }
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        val result = withContext(Dispatchers.IO) { client.acknowledgePurchase(params) }
        return result.asEmptyResult()
    }

    override suspend fun launchPurchase(): Result<List<UnlockPurchase>, DataError.Billing> {
        val host = activity ?: return Result.Error(DataError.Billing.UNAVAILABLE)
        val details = when (val product = unlockProduct()) {
            is Result.Error -> return product
            is Result.Success -> product.data
        }
        val offer = details.unlockOffer()
            ?: return Result.Error(DataError.Billing.PRODUCT_UNAVAILABLE)
        val product = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply { offer.offerToken?.takeIf { it.isNotBlank() }?.let(::setOfferToken) }
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(product))
            .build()

        return coroutineScope {
            val answer = async(start = CoroutineStart.UNDISPATCHED) { purchaseResults.first() }
            val launched = client.launchBillingFlow(host, params)
            when {
                launched.isOk() -> answer.await()

                /* Owned already means the store has it — reading it back is the honest answer. */
                launched.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    answer.cancel()
                    queryPurchases()
                }

                else -> {
                    answer.cancel()
                    Result.Error(launched.toBillingError())
                }
            }
        }
    }

    private suspend fun unlockProduct(): Result<ProductDetails, DataError.Billing> {
        connect().let { if (it is Result.Error) return it }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(UNLOCK_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val result = withContext(Dispatchers.IO) { client.queryProductDetails(params) }
        if (!result.billingResult.isOk()) return Result.Error(result.billingResult.toBillingError())
        return result.productDetailsList?.firstOrNull()
            ?.let { Result.Success(it) }
            ?: Result.Error(DataError.Billing.PRODUCT_UNAVAILABLE)
    }

    /** One connection for the whole process, retried with a widening gap while Play warms up. */
    private suspend fun connect(): EmptyResult<DataError.Billing> = connectionLock.withLock {
        if (client.isReady) return@withLock Result.Success(Unit)

        var delayMillis = FIRST_RETRY_DELAY_MILLIS
        var last: EmptyResult<DataError.Billing> = Result.Error(DataError.Billing.DISCONNECTED)
        repeat(CONNECT_ATTEMPTS) { attempt ->
            last = awaitConnection()
            if (last is Result.Success) return@withLock last
            if ((last as Result.Error).error == DataError.Billing.UNAVAILABLE) return@withLock last
            if (attempt < CONNECT_ATTEMPTS - 1) {
                delay(delayMillis)
                delayMillis *= 2
            }
        }
        last
    }

    private suspend fun awaitConnection(): EmptyResult<DataError.Billing> =
        suspendCancellableCoroutine { continuation ->
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (continuation.isActive) continuation.resume(billingResult.asEmptyResult())
                    }

                    override fun onBillingServiceDisconnected() {
                        if (continuation.isActive) {
                            continuation.resume(Result.Error(DataError.Billing.DISCONNECTED))
                        }
                    }
                },
            )
        }
}

/** A product with a single offer fills the singular field and leaves the list empty. */
private fun ProductDetails.unlockOffer(): ProductDetails.OneTimePurchaseOfferDetails? =
    oneTimePurchaseOfferDetailsList?.firstOrNull() ?: oneTimePurchaseOfferDetails

private fun List<Purchase>.unlockPurchases(): List<UnlockPurchase> =
    filter { UNLOCK_PRODUCT_ID in it.products }.map { it.toUnlockPurchase() }

private fun Purchase.toUnlockPurchase(): UnlockPurchase = UnlockPurchase(
    token = purchaseToken,
    state = if (purchaseState == Purchase.PurchaseState.PURCHASED) {
        PurchaseState.PURCHASED
    } else {
        PurchaseState.PENDING
    },
    isAcknowledged = isAcknowledged,
)

private fun BillingResult.isOk(): Boolean =
    responseCode == BillingClient.BillingResponseCode.OK

private fun BillingResult.asEmptyResult(): EmptyResult<DataError.Billing> =
    if (isOk()) Result.Success(Unit) else Result.Error(toBillingError())

private fun BillingResult.toBillingError(): DataError.Billing = when (responseCode) {
    BillingClient.BillingResponseCode.USER_CANCELED -> DataError.Billing.CANCELLED
    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
    BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
    -> DataError.Billing.UNAVAILABLE

    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
    BillingClient.BillingResponseCode.NETWORK_ERROR,
    -> DataError.Billing.DISCONNECTED

    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> DataError.Billing.PRODUCT_UNAVAILABLE
    else -> DataError.Billing.UNKNOWN
}
