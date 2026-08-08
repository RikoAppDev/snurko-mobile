package app.snurko.challenge.presentation.home

import app.snurko.challenge.domain.READ_ID
import app.snurko.challenge.domain.WORKOUT_ID
import app.snurko.challenge.domain.activeChallenge
import app.snurko.challenge.domain.completeDay
import app.snurko.challenge.domain.completeDaysThrough
import app.snurko.challenge.domain.day
import app.snurko.challenge.domain.partialDay
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class ChallengeHomeStateTest {

    @Test
    fun theHeaderCarriesTheChallengeNameTheDayIndexAndTheStreak() {
        val state = activeChallenge(checks = completeDaysThrough(3)).toChallengeHomeState(day(4))

        assertThat(state.challengeName).isEqualTo("90 days")
        assertThat(state.dayIndex).isEqualTo(4)
        assertThat(state.totalDays).isEqualTo(90)
        assertThat(state.streak).isEqualTo(3)
    }

    @Test
    fun theGridHasOneCellPerChallengeDay() {
        val state = activeChallenge().toChallengeHomeState(day(1))

        assertThat(state.cells).hasSize(90)
    }

    @Test
    fun passedDaysThatWereFinishedAreMarkedComplete() {
        val state = activeChallenge(checks = completeDaysThrough(3)).toChallengeHomeState(day(4))

        assertThat(state.cells.take(3)).containsExactly(
            GridCellUi.COMPLETE,
            GridCellUi.COMPLETE,
            GridCellUi.COMPLETE,
        )
    }

    @Test
    fun aPassedDayThatWasNotFinishedStaysAnEmptyFailedCell() {
        val state = activeChallenge(checks = listOf(completeDay(1), partialDay(2)))
            .toChallengeHomeState(day(3))

        assertThat(state.cells[0]).isEqualTo(GridCellUi.COMPLETE)
        assertThat(state.cells[1]).isEqualTo(GridCellUi.FAILED)
    }

    @Test
    fun todayIsFramedUntilEveryHabitIsChecked() {
        val state = activeChallenge(checks = completeDaysThrough(2) + partialDay(3))
            .toChallengeHomeState(day(3))

        assertThat(state.cells[2]).isEqualTo(GridCellUi.TODAY)
    }

    @Test
    fun todayGainsItsMarkAsSoonAsEveryHabitIsChecked() {
        val state = activeChallenge(checks = completeDaysThrough(3)).toChallengeHomeState(day(3))

        assertThat(state.cells[2]).isEqualTo(GridCellUi.TODAY_COMPLETE)
    }

    @Test
    fun daysAfterTodayArePlainFutureCells() {
        val state = activeChallenge(checks = completeDaysThrough(2)).toChallengeHomeState(day(3))

        assertThat(state.cells[3]).isEqualTo(GridCellUi.FUTURE)
        assertThat(state.cells.last()).isEqualTo(GridCellUi.FUTURE)
    }

    @Test
    fun theMarkedDayCountCoversFinishedDaysIncludingToday() {
        val state = activeChallenge(checks = completeDaysThrough(3)).toChallengeHomeState(day(3))

        assertThat(state.markedDays).isEqualTo(3)
    }

    @Test
    fun theChecklistListsEveryHabitWithItsFailCondition() {
        val state = activeChallenge().toChallengeHomeState(day(1))

        assertThat(state.habits).containsExactly(
            HabitRowUi(
                id = WORKOUT_ID,
                name = "Workout",
                failCondition = "less than 30 minutes",
                isChecked = false,
            ),
            HabitRowUi(
                id = READ_ID,
                name = "Read",
                failCondition = "fewer than 10 pages",
                isChecked = false,
            ),
        )
    }

    @Test
    fun theChecklistReflectsWhatWasCheckedToday() {
        val state = activeChallenge(checks = completeDaysThrough(2) + partialDay(3))
            .toChallengeHomeState(day(3))

        assertThat(state.habits.map { it.isChecked }).containsExactly(true, false)
    }

    @Test
    fun anOngoingChallengeIsNeitherLoadingNorCompleted() {
        val state = activeChallenge().toChallengeHomeState(day(1))

        assertThat(state.isLoading).isFalse()
        assertThat(state.isCompleted).isFalse()
    }

    @Test
    fun theLastDayFinishedCompletesTheChallengeAndFillsTheGrid() {
        val state = activeChallenge(checks = completeDaysThrough(90)).toChallengeHomeState(day(90))

        assertThat(state.isCompleted).isTrue()
        assertThat(state.markedDays).isEqualTo(90)
        assertThat(state.cells.count { it == GridCellUi.COMPLETE }).isEqualTo(89)
        assertThat(state.cells.last()).isEqualTo(GridCellUi.TODAY_COMPLETE)
    }

    @Test
    fun aCompletedChallengeHasNoChecklistLeftAndStopsAtDayNinety() {
        val state = activeChallenge(checks = completeDaysThrough(90)).toChallengeHomeState(day(94))

        assertThat(state.isCompleted).isTrue()
        assertThat(state.dayIndex).isEqualTo(90)
        assertThat(state.habits).isEmpty()
        assertThat(state.cells.all { it == GridCellUi.COMPLETE }).isTrue()
    }

    @Test
    fun theGridIsReadFromTheAttemptStartNotTheChallengeStart() {
        val state = activeChallenge(
            checks = listOf(completeDay(1)),
            attemptNumber = 2,
            attemptStart = day(10),
        ).toChallengeHomeState(day(11))

        assertThat(state.dayIndex).isEqualTo(2)
        assertThat(state.cells[0]).isEqualTo(GridCellUi.COMPLETE)
        assertThat(state.cells[1]).isEqualTo(GridCellUi.TODAY)
    }

    @Test
    fun everyPencilMarkKeepsTheSameRotationForTheSameDay() {
        val first = (1..90).map(::markRotationDegrees)
        val second = (1..90).map(::markRotationDegrees)

        assertThat(first).isEqualTo(second)
        assertThat(first.all { it >= -MAX_MARK_ROTATION && it <= MAX_MARK_ROTATION }).isTrue()
        assertThat(first.distinct().size > 1).isTrue()
    }
}
