package app.snurko.reminders.presentation

import app.snurko.core.presentation.UiAction

sealed interface RemindersAction : UiAction {
    data class RowClicked(val habitId: Long) : RemindersAction
    data class TimePicked(val hour: Int, val minute: Int) : RemindersAction
    data object TimePickerDismissed : RemindersAction
    data class ReminderSwitchedOff(val habitId: Long) : RemindersAction
    data object SettingsClicked : RemindersAction
    data object ScreenResumed : RemindersAction
}
