package app.snurko.reminders.presentation

import androidx.lifecycle.viewModelScope
import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.core.domain.onFailure
import app.snurko.core.presentation.MviViewModel
import app.snurko.reminders.domain.NotificationPermission
import app.snurko.reminders.domain.NotificationPermissionGateway
import app.snurko.reminders.domain.Reminder
import app.snurko.reminders.domain.ReminderRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

private val DEFAULT_REMINDER_TIME = LocalTime(20, 0)

class RemindersViewModel(
    challengeRepository: ChallengeRepository,
    private val reminderRepository: ReminderRepository,
    private val permissions: NotificationPermissionGateway,
) : MviViewModel<RemindersState, RemindersAction, RemindersEvent>(initialState = RemindersState()) {

    private var wasRefused = false

    init {
        combine(
            challengeRepository.observeActiveChallenge().filterNotNull(),
            reminderRepository.observeReminders(),
        ) { active, reminders -> toRows(active, reminders) }
            .onEach { rows ->
                updateState { it.copy(rows = rows, isLoading = false) }
                readPermission()
            }
            .launchIn(viewModelScope)
    }

    override fun onAction(action: RemindersAction) {
        when (action) {
            is RemindersAction.RowClicked -> openTimePicker(action.habitId)
            is RemindersAction.TimePicked -> setTime(action.hour, action.minute)
            RemindersAction.TimePickerDismissed -> updateState { it.copy(timePicker = null) }
            is RemindersAction.ReminderSwitchedOff -> switchOff(action.habitId)
            RemindersAction.SettingsClicked -> permissions.openSettings()
            RemindersAction.ScreenResumed -> readPermission()
        }
    }

    private fun openTimePicker(habitId: Long) {
        val row = state.value.rows.firstOrNull { it.habitId == habitId } ?: return
        viewModelScope.launch {
            if (permissions.request() == NotificationPermission.DENIED) {
                wasRefused = true
                readPermission()
                return@launch
            }
            wasRefused = false
            readPermission()
            updateState { state ->
                state.copy(timePicker = TimePickerUi(habitId, row.time ?: DEFAULT_REMINDER_TIME))
            }
        }
    }

    private fun setTime(hour: Int, minute: Int) {
        val picker = state.value.timePicker ?: return
        updateState { it.copy(timePicker = null) }
        viewModelScope.launch {
            reminderRepository.setReminderTime(picker.habitId, LocalTime(hour, minute))
                .onFailure { sendEvent(RemindersEvent.UpdateFailed) }
        }
    }

    private fun switchOff(habitId: Long) {
        viewModelScope.launch {
            reminderRepository.setReminderTime(habitId, null)
                .onFailure { sendEvent(RemindersEvent.UpdateFailed) }
        }
    }

    /**
     * Blocked notifications are only worth explaining once the user has asked for a reminder —
     * either just now, or by leaving one switched on.
     */
    private fun readPermission() {
        val isBlocked = permissions.current() == NotificationPermission.DENIED
        updateState { state ->
            val isWanted = wasRefused || state.rows.any { it.time != null }
            state.copy(isNotificationPermissionDenied = isBlocked && isWanted)
        }
    }
}

private fun toRows(active: ActiveChallenge, reminders: List<Reminder>): List<ReminderRowUi> {
    val timeByHabitId = reminders.associate { it.habitId to it.time }
    return active.challenge.habits.map { habit ->
        ReminderRowUi(habitId = habit.id, name = habit.name, time = timeByHabitId[habit.id])
    }
}
