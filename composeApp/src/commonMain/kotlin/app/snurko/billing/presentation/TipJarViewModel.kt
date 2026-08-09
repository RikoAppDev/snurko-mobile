package app.snurko.billing.presentation

import androidx.lifecycle.viewModelScope
import app.snurko.billing.domain.BillingGateway
import app.snurko.billing.domain.BillingPurchase
import app.snurko.billing.domain.PurchaseState
import app.snurko.billing.domain.TipProduct
import app.snurko.core.domain.Result
import app.snurko.core.presentation.MviViewModel
import kotlinx.coroutines.launch

/**
 * Nothing here is remembered or unlocked: a tip is taken, handed straight back to the
 * store so it can be given again, thanked for, and forgotten.
 */
class TipJarViewModel(
    private val billingGateway: BillingGateway,
) : MviViewModel<TipJarState, TipJarAction, Nothing>(initialState = TipJarState()) {

    init {
        loadPrices()
        consumeLeftovers()
    }

    override fun onAction(action: TipJarAction) {
        when (action) {
            is TipJarAction.TipClicked -> tip(action.product)
            TipJarAction.PriceRetryClicked -> loadPrices()
        }
    }

    private fun loadPrices() {
        updateState { it.copy(isPriceLoading = true) }
        viewModelScope.launch {
            val amounts = TipProduct.entries.mapNotNull { product ->
                (billingGateway.price(product.productId) as? Result.Success)
                    ?.let { TipAmountUi(product = product, price = it.data) }
            }
            updateState { it.copy(amounts = amounts, isPriceLoading = false) }
        }
    }

    /** A tip the store took while the app was gone still blocks the next one until it is given back. */
    private fun consumeLeftovers() {
        viewModelScope.launch {
            TipProduct.entries.forEach { product ->
                (billingGateway.queryPurchases(product.productId) as? Result.Success)
                    ?.data
                    ?.let { consumeAll(it) }
            }
        }
    }

    private fun tip(product: TipProduct) {
        if (state.value.isTipping) return
        updateState {
            it.copy(
                tippingProduct = product,
                isThanksVisible = false,
                isTipPending = false,
                error = null,
            )
        }
        viewModelScope.launch {
            when (val purchases = billingGateway.launchPurchase(product.productId)) {
                is Result.Error -> updateState { it.copy(error = purchases.error) }
                is Result.Success -> settle(purchases.data)
            }
            updateState { it.copy(tippingProduct = null) }
        }
    }

    private suspend fun settle(purchases: List<BillingPurchase>) {
        val taken = consumeAll(purchases)
        when {
            taken -> updateState { it.copy(isThanksVisible = true) }
            purchases.any { it.state == PurchaseState.PENDING } ->
                updateState { it.copy(isTipPending = true) }
        }
    }

    private suspend fun consumeAll(purchases: List<BillingPurchase>): Boolean {
        val taken = purchases.filter { it.state == PurchaseState.PURCHASED }
        taken.forEach { billingGateway.consume(it.token) }
        return taken.isNotEmpty()
    }
}
