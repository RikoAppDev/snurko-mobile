package app.snurko.reminders.data

import app.snurko.core.database.ReminderEntity
import app.snurko.reminders.domain.Reminder
import kotlinx.datetime.LocalTime

private const val MINUTES_PER_HOUR = 60

fun ReminderEntity.toReminder(): Reminder = Reminder(
    habitId = habitId,
    time = LocalTime(hour = minuteOfDay / MINUTES_PER_HOUR, minute = minuteOfDay % MINUTES_PER_HOUR),
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    habitId = habitId,
    minuteOfDay = time.toMinuteOfDay(),
)

fun LocalTime.toMinuteOfDay(): Int = hour * MINUTES_PER_HOUR + minute
