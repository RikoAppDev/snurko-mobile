package app.snurko.challenge.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

sealed interface AttemptOutcome {
    data object Ongoing : AttemptOutcome
    data class Failed(val failedOnDay: Int) : AttemptOutcome
    data object Completed : AttemptOutcome
}

fun Attempt.dayIndexOn(date: LocalDate): Int = startDate.daysUntil(date) + 1

fun Attempt.failedOn(day: Int): Attempt = copy(status = AttemptStatus.FAILED, failedOnDay = day)

fun Attempt.completed(): Attempt = copy(status = AttemptStatus.COMPLETED, failedOnDay = null)

fun Attempt.nextAttempt(startDate: LocalDate): Attempt = Attempt(
    number = number + 1,
    startDate = startDate,
    status = AttemptStatus.ACTIVE,
)

fun ActiveChallenge.dayIndexOn(date: LocalDate): Int = attempt.dayIndexOn(date)

/**
 * A day is lost only once it has passed: an incomplete day is still PENDING for as
 * long as it is today, so the user can finish it before local midnight.
 */
fun ActiveChallenge.dayState(day: Int, today: LocalDate): DayState {
    val checkedHabitIds = checks.firstOrNull { it.day == day }?.checkedHabitIds.orEmpty()
    return when {
        challenge.habits.all { it.id in checkedHabitIds } -> DayState.COMPLETE
        day < dayIndexOn(today) -> DayState.FAILED
        else -> DayState.PENDING
    }
}

fun ActiveChallenge.evaluate(today: LocalDate): AttemptOutcome {
    val todayIndex = dayIndexOn(today)
    val lastPassedDay = minOf(todayIndex - 1, challenge.lengthDays)
    for (day in 1..lastPassedDay) {
        if (dayState(day, today) == DayState.FAILED) return AttemptOutcome.Failed(failedOnDay = day)
    }
    val lastDayIsComplete = todayIndex >= challenge.lengthDays &&
        dayState(challenge.lengthDays, today) == DayState.COMPLETE
    return if (lastDayIsComplete) AttemptOutcome.Completed else AttemptOutcome.Ongoing
}

fun ActiveChallenge.currentStreak(today: LocalDate): Int {
    val lastDay = minOf(dayIndexOn(today), challenge.lengthDays)
    var streak = 0
    for (day in 1..lastDay) {
        if (dayState(day, today) != DayState.COMPLETE) break
        streak++
    }
    return streak
}
