package app.snurko.reminders.presentation

import app.snurko.core.presentation.UiEvent

sealed interface RemindersEvent : UiEvent {
    data object UpdateFailed : RemindersEvent
}
