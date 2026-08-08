package app.snurko.challenge.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

const val WORKOUT_ID = 1L
const val READ_ID = 2L

val challengeStart = LocalDate(2026, 1, 1)

val testHabits = listOf(
    Habit(id = WORKOUT_ID, name = "Workout", failCondition = "less than 30 minutes"),
    Habit(id = READ_ID, name = "Read", failCondition = "fewer than 10 pages"),
)

fun day(index: Int): LocalDate = challengeStart.plus(index - 1, DateTimeUnit.DAY)

fun completeDay(index: Int) = DayCheck(day = index, checkedHabitIds = setOf(WORKOUT_ID, READ_ID))

fun partialDay(index: Int) = DayCheck(day = index, checkedHabitIds = setOf(WORKOUT_ID))

fun completeDaysThrough(lastDay: Int): List<DayCheck> = (1..lastDay).map(::completeDay)

fun activeChallenge(
    checks: List<DayCheck> = emptyList(),
    attemptNumber: Int = 1,
    attemptStart: LocalDate = challengeStart,
    lengthDays: Int = CHALLENGE_LENGTH_DAYS,
): ActiveChallenge = ActiveChallenge(
    challenge = Challenge(
        id = 1,
        name = "90 days",
        habits = testHabits,
        startDate = challengeStart,
        lengthDays = lengthDays,
    ),
    attempt = Attempt(
        id = 1,
        number = attemptNumber,
        startDate = attemptStart,
        status = AttemptStatus.ACTIVE,
    ),
    checks = checks,
)
