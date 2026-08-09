package app.snurko.billing.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.snurko.billing.domain.TipProduct
import app.snurko.core.domain.DataError
import app.snurko.core.presentation.UiState

@Immutable
data class TipAmountUi(
    val product: TipProduct,
    val price: String,
)

@Stable
data class TipJarState(
    val amounts: List<TipAmountUi> = emptyList(),
    val isPriceLoading: Boolean = true,
    val tippingProduct: TipProduct? = null,
    val isThanksVisible: Boolean = false,
    val isTipPending: Boolean = false,
    val error: DataError.Billing? = null,
) : UiState {

    val isTipping: Boolean
        get() = tippingProduct != null

    /** An amount the store gave no price for is never offered, so nothing left means no prices. */
    val isPriceUnavailable: Boolean
        get() = !isPriceLoading && amounts.isEmpty()
}
