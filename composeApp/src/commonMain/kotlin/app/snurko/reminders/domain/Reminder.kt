package app.snurko.reminders.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

data class Reminder(
    val habitId: Long,
    val time: LocalTime,
)

data class ScheduledReminder(
    val habitId: Long,
    val fireAt: LocalDateTime,
)

data class ReminderSchedule(
    val scheduled: List<ScheduledReminder> = emptyList(),
    val cancelledHabitIds: List<Long> = emptyList(),
)

/** Everything a fired reminder needs to name itself and to check its habit off. */
data class ReminderFiring(
    val habitId: Long,
    val habitName: String,
    val attemptId: Long,
    val day: Int,
)
