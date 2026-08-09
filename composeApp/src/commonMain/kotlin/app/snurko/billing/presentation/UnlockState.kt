package app.snurko.billing.presentation

import androidx.compose.runtime.Stable
import app.snurko.core.domain.DataError
import app.snurko.core.presentation.UiState

@Stable
data class UnlockState(
    val isUnlocked: Boolean = false,
    val price: String? = null,
    val isPriceLoading: Boolean = true,
    val isPurchasing: Boolean = false,
    val isPurchasePending: Boolean = false,
    val isRestoring: Boolean = false,
    val error: DataError.Billing? = null,
) : UiState {

    val isBusy: Boolean
        get() = isPurchasing || isRestoring

    val isPriceUnavailable: Boolean
        get() = !isPriceLoading && price == null

    val canBuy: Boolean
        get() = !isUnlocked && !isBusy && price != null
}
