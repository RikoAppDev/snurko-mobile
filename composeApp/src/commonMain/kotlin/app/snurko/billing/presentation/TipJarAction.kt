package app.snurko.billing.presentation

import app.snurko.billing.domain.TipProduct
import app.snurko.core.presentation.UiAction

sealed interface TipJarAction : UiAction {
    data class TipClicked(val product: TipProduct) : TipJarAction
    data object PriceRetryClicked : TipJarAction
}
