package app.snurko.billing.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.flow.Flow

interface BillingGateway {

    /**
     * Purchases of [productId] the store hands back as a flow completes. Only ever adds
     * to what is owned — a cancelled or empty flow says nothing about purchases made
     * elsewhere, so [queryPurchases] stays the only source that may take one away.
     */
    fun purchaseUpdates(productId: String): Flow<List<BillingPurchase>>

    suspend fun price(productId: String): Result<String, DataError.Billing>

    suspend fun queryPurchases(productId: String): Result<List<BillingPurchase>, DataError.Billing>

    suspend fun acknowledge(purchaseToken: String): EmptyResult<DataError.Billing>

    /** Opens the store sheet and answers with whatever it produced once it closes. */
    suspend fun launchPurchase(productId: String): Result<List<BillingPurchase>, DataError.Billing>
}
