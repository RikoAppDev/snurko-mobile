package app.snurko.widget

import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.domain.AttemptOutcome
import app.snurko.challenge.domain.CHALLENGE_LENGTH_DAYS
import app.snurko.challenge.domain.DayState
import app.snurko.challenge.domain.checkedHabitIdsOn
import app.snurko.challenge.domain.dayIndexOn
import app.snurko.challenge.domain.dayState
import app.snurko.challenge.domain.evaluate
import kotlinx.datetime.LocalDate

const val WIDGET_GRID_COLUMNS = 10

enum class WidgetCell {
    MARKED,
    TODAY,
    MISSED,
    FUTURE,
}

sealed interface WidgetState {

    data object NoChallenge : WidgetState

    data class Challenge(
        val dayIndex: Int,
        val totalDays: Int,
        val checkedHabits: Int,
        val totalHabits: Int,
        val cells: List<WidgetCell>,
        val isCompleted: Boolean,
    ) : WidgetState
}

fun widgetState(active: ActiveChallenge?, today: LocalDate): WidgetState {
    if (active == null) return WidgetState.NoChallenge

    val totalDays = active.challenge.lengthDays
    val todayIndex = active.dayIndexOn(today)
    val isCompleted = active.evaluate(today) == AttemptOutcome.Completed

    return WidgetState.Challenge(
        dayIndex = todayIndex.coerceIn(1, totalDays),
        totalDays = totalDays,
        checkedHabits = active.checkedHabitIdsOn(todayIndex).size,
        totalHabits = active.challenge.habits.size,
        cells = (1..totalDays).map { day -> active.cellFor(day, todayIndex, today) },
        isCompleted = isCompleted,
    )
}

private fun ActiveChallenge.cellFor(day: Int, todayIndex: Int, today: LocalDate): WidgetCell = when {
    day == todayIndex -> WidgetCell.TODAY
    dayState(day, today) == DayState.COMPLETE -> WidgetCell.MARKED
    day < todayIndex -> WidgetCell.MISSED
    else -> WidgetCell.FUTURE
}

/** Rows of [WIDGET_GRID_COLUMNS] cells, so the grid reads 10 across and 9 down at 90 days. */
fun WidgetState.Challenge.cellRows(): List<List<WidgetCell>> = cells.chunked(WIDGET_GRID_COLUMNS)

val WIDGET_GRID_ROWS = CHALLENGE_LENGTH_DAYS / WIDGET_GRID_COLUMNS
