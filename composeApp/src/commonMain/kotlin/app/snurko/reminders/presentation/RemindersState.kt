package app.snurko.reminders.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.snurko.core.presentation.UiState
import kotlinx.datetime.LocalTime

@Immutable
data class ReminderRowUi(
    val habitId: Long,
    val name: String,
    val time: LocalTime?,
)

@Immutable
data class TimePickerUi(
    val habitId: Long,
    val startFrom: LocalTime,
)

@Stable
data class RemindersState(
    val rows: List<ReminderRowUi> = emptyList(),
    val isLoading: Boolean = true,
    val isNotificationPermissionDenied: Boolean = false,
    val timePicker: TimePickerUi? = null,
) : UiState
