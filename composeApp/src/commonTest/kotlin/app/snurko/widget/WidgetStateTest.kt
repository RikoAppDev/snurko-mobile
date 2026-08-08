package app.snurko.widget

import app.snurko.challenge.domain.DayCheck
import app.snurko.challenge.domain.activeChallenge
import app.snurko.challenge.domain.completeDay
import app.snurko.challenge.domain.completeDaysThrough
import app.snurko.challenge.domain.day
import app.snurko.challenge.domain.partialDay
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import kotlinx.datetime.LocalDate
import kotlin.test.Test

class WidgetStateTest {

    @Test
    fun withoutAChallengeTheWidgetHasNothingToShow() {
        assertThat(widgetState(active = null, today = day(1))).isEqualTo(WidgetState.NoChallenge)
    }

    @Test
    fun theWidgetCountsTodaysCheckedHabits() {
        val state = challengeState(checks = listOf(partialDay(3)), today = day(3))

        assertThat(state.dayIndex).isEqualTo(3)
        assertThat(state.checkedHabits).isEqualTo(1)
        assertThat(state.totalHabits).isEqualTo(2)
    }

    @Test
    fun aDayWithNothingCheckedCountsZero() {
        val state = challengeState(today = day(3))

        assertThat(state.checkedHabits).isEqualTo(0)
    }

    @Test
    fun theGridHasOneCellPerDay() {
        val state = challengeState(today = day(3))

        assertThat(state.cells).hasSize(90)
        assertThat(state.cellRows()).hasSize(9)
        assertThat(state.cellRows().first()).hasSize(10)
    }

    @Test
    fun finishedDaysAreMarkedAndUnfinishedPastDaysAreMissed() {
        val state = challengeState(
            checks = completeDaysThrough(2) + partialDay(3),
            today = day(5),
        )

        assertThat(state.cells[0]).isEqualTo(WidgetCell.MARKED)
        assertThat(state.cells[1]).isEqualTo(WidgetCell.MARKED)
        assertThat(state.cells[2]).isEqualTo(WidgetCell.MISSED)
        assertThat(state.cells[3]).isEqualTo(WidgetCell.MISSED)
    }

    @Test
    fun todayIsItsOwnCellEvenBeforeItIsFinished() {
        val state = challengeState(checks = completeDaysThrough(2), today = day(3))

        assertThat(state.cells[2]).isEqualTo(WidgetCell.TODAY)
    }

    @Test
    fun todayStaysTheTodayCellOnceItIsFinished() {
        val state = challengeState(checks = completeDaysThrough(3), today = day(3))

        assertThat(state.cells[2]).isEqualTo(WidgetCell.TODAY)
    }

    @Test
    fun daysAheadAreStillEmptyPaper() {
        val state = challengeState(today = day(3))

        assertThat(state.cells[3]).isEqualTo(WidgetCell.FUTURE)
        assertThat(state.cells[89]).isEqualTo(WidgetCell.FUTURE)
    }

    @Test
    fun anOngoingChallengeIsNotCompleted() {
        assertThat(challengeState(checks = completeDaysThrough(3), today = day(3)).isCompleted).isFalse()
    }

    @Test
    fun aFinishedNinetiethDayCompletesTheChallenge() {
        val state = challengeState(checks = completeDaysThrough(90), today = day(90))

        assertThat(state.isCompleted).isTrue()
        assertThat(state.cells.count { it == WidgetCell.MARKED || it == WidgetCell.TODAY }).isEqualTo(90)
    }

    @Test
    fun theDayNumberNeverRunsPastTheLastDay() {
        val state = challengeState(checks = completeDaysThrough(90), today = day(95))

        assertThat(state.dayIndex).isEqualTo(90)
        assertThat(state.isCompleted).isTrue()
    }

    @Test
    fun aDayCheckedOnAnotherDayDoesNotCountForToday() {
        val state = challengeState(checks = listOf(completeDay(2)), today = day(3))

        assertThat(state.checkedHabits).isEqualTo(0)
    }

    private fun challengeState(
        checks: List<DayCheck> = emptyList(),
        today: LocalDate,
    ): WidgetState.Challenge {
        val state = widgetState(active = activeChallenge(checks = checks), today = today)
        assertThat(state).isInstanceOf(WidgetState.Challenge::class)
        return state as WidgetState.Challenge
    }
}
