package app.snurko.reminders.domain

import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.checkedHabitIdsOn
import app.snurko.challenge.domain.dayIndexOn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus

fun nextFireAt(time: LocalTime, now: LocalDateTime): LocalDateTime {
    val date = if (time > now.time) now.date else now.date.plus(1, DateTimeUnit.DAY)
    return date.atTime(time)
}

fun ActiveChallenge.remindsOn(date: LocalDate): Boolean =
    attempt.status == AttemptStatus.ACTIVE && dayIndexOn(date) in 1..challenge.lengthDays

/** Answers for a single date, so it works both when the alarm is set and when it lands. */
fun ActiveChallenge.shouldRemind(habitId: Long, date: LocalDate): Boolean {
    if (!remindsOn(date)) return false
    if (challenge.habits.none { it.id == habitId }) return false
    return habitId !in checkedHabitIdsOn(dayIndexOn(date))
}

fun reminderSchedule(
    active: ActiveChallenge?,
    reminders: List<Reminder>,
    now: LocalDateTime,
    rechecksAtFireTime: Boolean = true,
): ReminderSchedule {
    val habitIds = active?.challenge?.habits.orEmpty().mapTo(mutableSetOf()) { it.id }
    val scheduled = mutableListOf<ScheduledReminder>()
    val cancelledHabitIds = mutableListOf<Long>()

    for (reminder in reminders) {
        val fireAt = nextFireAt(reminder.time, now)
        val keeps = when {
            active == null -> false
            rechecksAtFireTime -> reminder.habitId in habitIds && active.remindsOn(fireAt.date)
            else -> active.shouldRemind(reminder.habitId, fireAt.date)
        }
        if (keeps) {
            scheduled += ScheduledReminder(habitId = reminder.habitId, fireAt = fireAt)
        } else {
            cancelledHabitIds += reminder.habitId
        }
    }
    return ReminderSchedule(scheduled = scheduled, cancelledHabitIds = cancelledHabitIds)
}
