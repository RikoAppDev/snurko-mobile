package app.snurko.billing.presentation

import app.snurko.core.presentation.UiEvent

sealed interface UnlockEvent : UiEvent {
    data object Unlocked : UnlockEvent
}
