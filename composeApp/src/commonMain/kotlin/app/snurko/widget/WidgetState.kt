package app.snurko.widget

import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.domain.AttemptOutcome
import app.snurko.challenge.domain.CHALLENGE_LENGTH_DAYS
import app.snurko.challenge.domain.DayState
import app.snurko.challenge.domain.checkedHabitIdsOn
import app.snurko.challenge.domain.dayIndexOn
import app.snurko.challenge.domain.dayState
import app.snurko.challenge.domain.evaluate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.LocalDate

const val WIDGET_GRID_COLUMNS = 10

enum class WidgetCell {
    MARKED,
    TODAY,
    MISSED,
    FUTURE,
}

data class WidgetHabit(
    val name: String,
    val checked: Boolean,
)

sealed interface WidgetState {

    data object NoChallenge : WidgetState

    /**
     * [failedOnDay] is set once a past day went unfinished: the attempt is over even though the
     * app records the restart only when it is next opened.
     */
    data class Challenge(
        val dayIndex: Int,
        val totalDays: Int,
        val checkedHabits: Int,
        val totalHabits: Int,
        val habits: List<WidgetHabit>,
        val cells: List<WidgetCell>,
        val isCompleted: Boolean,
        val failedOnDay: Int? = null,
    ) : WidgetState
}

fun widgetState(active: ActiveChallenge?, today: LocalDate): WidgetState {
    if (active == null) return WidgetState.NoChallenge

    val totalDays = active.challenge.lengthDays
    val todayIndex = active.dayIndexOn(today)
    val outcome = active.evaluate(today)
    val checkedToday = active.checkedHabitIdsOn(todayIndex)

    return WidgetState.Challenge(
        dayIndex = todayIndex.coerceIn(1, totalDays),
        totalDays = totalDays,
        checkedHabits = checkedToday.size,
        totalHabits = active.challenge.habits.size,
        habits = active.challenge.habits.map { WidgetHabit(name = it.name, checked = it.id in checkedToday) },
        cells = (1..totalDays).map { day -> active.cellFor(day, todayIndex, today) },
        isCompleted = outcome == AttemptOutcome.Completed,
        failedOnDay = (outcome as? AttemptOutcome.Failed)?.failedOnDay,
    )
}

/** Follows the notebook and the calendar, so a widget on screen never keeps stale marks or a past day. */
fun widgetStates(activeChallenge: Flow<ActiveChallenge?>, dates: Flow<LocalDate>): Flow<WidgetState> =
    combine(activeChallenge, dates) { active, today -> widgetState(active, today) }
        .distinctUntilChanged()

private fun ActiveChallenge.cellFor(day: Int, todayIndex: Int, today: LocalDate): WidgetCell = when {
    day == todayIndex -> WidgetCell.TODAY
    dayState(day, today) == DayState.COMPLETE -> WidgetCell.MARKED
    day < todayIndex -> WidgetCell.MISSED
    else -> WidgetCell.FUTURE
}

/** Rows of [WIDGET_GRID_COLUMNS] cells, so the grid reads 10 across and 9 down at 90 days. */
fun WidgetState.Challenge.cellRows(): List<List<WidgetCell>> = cells.chunked(WIDGET_GRID_COLUMNS)

val WIDGET_GRID_ROWS = CHALLENGE_LENGTH_DAYS / WIDGET_GRID_COLUMNS
