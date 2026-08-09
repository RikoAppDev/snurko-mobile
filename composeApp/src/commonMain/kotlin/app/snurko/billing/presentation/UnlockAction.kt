package app.snurko.billing.presentation

import app.snurko.core.presentation.UiAction

sealed interface UnlockAction : UiAction {
    data object BuyClicked : UnlockAction
    data object RestoreClicked : UnlockAction
    data object PriceRetryClicked : UnlockAction
}
