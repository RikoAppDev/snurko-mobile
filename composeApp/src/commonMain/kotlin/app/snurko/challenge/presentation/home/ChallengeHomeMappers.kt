package app.snurko.challenge.presentation.home

import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.domain.AttemptOutcome
import app.snurko.challenge.domain.DayState
import app.snurko.challenge.domain.checkedHabitIdsOn
import app.snurko.challenge.domain.currentStreak
import app.snurko.challenge.domain.dayIndexOn
import app.snurko.challenge.domain.dayState
import app.snurko.challenge.domain.evaluate
import kotlinx.datetime.LocalDate

const val MAX_MARK_ROTATION = 6f

private const val ROTATION_STEPS = 2 * MAX_MARK_ROTATION.toInt() + 1
private const val ROTATION_STRIDE = 7

fun markRotationDegrees(day: Int): Float =
    (day * ROTATION_STRIDE % ROTATION_STEPS - MAX_MARK_ROTATION.toInt()).toFloat()

fun ActiveChallenge.toChallengeHomeState(today: LocalDate): ChallengeHomeState {
    val todayIndex = dayIndexOn(today)
    val isCompleted = evaluate(today) == AttemptOutcome.Completed
    return ChallengeHomeState(
        challengeName = challenge.name,
        dayIndex = todayIndex.coerceIn(1, challenge.lengthDays),
        totalDays = challenge.lengthDays,
        streak = currentStreak(today),
        cells = (1..challenge.lengthDays).map { day -> cellFor(day, todayIndex, today) },
        habits = if (isCompleted) emptyList() else habitRowsFor(todayIndex),
        isLoading = false,
        isCompleted = isCompleted,
    )
}

private fun ActiveChallenge.cellFor(day: Int, todayIndex: Int, today: LocalDate): GridCellUi {
    val isComplete = dayState(day, today) == DayState.COMPLETE
    return when {
        day == todayIndex && isComplete -> GridCellUi.TODAY_COMPLETE
        day == todayIndex -> GridCellUi.TODAY
        isComplete -> GridCellUi.COMPLETE
        day < todayIndex -> GridCellUi.FAILED
        else -> GridCellUi.FUTURE
    }
}

private fun ActiveChallenge.habitRowsFor(day: Int): List<HabitRowUi> {
    val checkedIds = checkedHabitIdsOn(day)
    return challenge.habits.map { habit ->
        HabitRowUi(
            id = habit.id,
            name = habit.name,
            failCondition = habit.failCondition,
            isChecked = habit.id in checkedIds,
        )
    }
}
