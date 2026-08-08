package app.snurko.challenge.presentation.history

import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.attemptRecord
import app.snurko.challenge.domain.completeDaysThrough
import app.snurko.challenge.domain.day
import app.snurko.challenge.domain.notebook
import app.snurko.challenge.domain.partialDay
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class HistoryRowsTest {

    @Test
    fun theRunningAttemptReadsAsInProgressOnTodaysDay() {
        val rows = notebook(
            attempts = listOf(attemptRecord(number = 1, checks = completeDaysThrough(3))),
        ).toAttemptRows(today = day(4))

        assertThat(rows.single().outcome).isEqualTo(AttemptOutcomeUi.InProgress(day = 4))
    }

    @Test
    fun theRunningAttemptHasNoEndDateYet() {
        val rows = notebook().toAttemptRows(today = day(4))

        assertThat(rows.single().startDate).isEqualTo(day(1))
        assertThat(rows.single().endDate).isNull()
    }

    @Test
    fun aLostAttemptNamesTheDayAndTheHabitsThatStayedUnchecked() {
        val rows = notebook(
            attempts = listOf(
                attemptRecord(
                    number = 1,
                    status = AttemptStatus.FAILED,
                    failedOnDay = 23,
                    checks = completeDaysThrough(22) + partialDay(23),
                ),
            ),
        ).toAttemptRows(today = day(30))

        assertThat(rows.single().outcome).isEqualTo(
            AttemptOutcomeUi.Failed(failedOnDay = 23, missedHabits = listOf("Read")),
        )
    }

    @Test
    fun aLostAttemptNamesEveryHabitWhenTheDayWasNeverOpened() {
        val rows = notebook(
            attempts = listOf(
                attemptRecord(
                    number = 1,
                    status = AttemptStatus.FAILED,
                    failedOnDay = 5,
                    checks = completeDaysThrough(4),
                ),
            ),
        ).toAttemptRows(today = day(30))

        val outcome = rows.single().outcome as AttemptOutcomeUi.Failed
        assertThat(outcome.missedHabits).containsExactly("Workout", "Read")
    }

    @Test
    fun aLostAttemptEndsOnTheDayItWasLost() {
        val rows = notebook(
            attempts = listOf(
                attemptRecord(number = 1, status = AttemptStatus.FAILED, failedOnDay = 23),
            ),
        ).toAttemptRows(today = day(30))

        assertThat(rows.single().startDate).isEqualTo(day(1))
        assertThat(rows.single().endDate).isEqualTo(day(23))
    }

    @Test
    fun aFinishedAttemptEndsOnTheLastDayAndCarriesNoMissedHabits() {
        val rows = notebook(
            attempts = listOf(
                attemptRecord(
                    number = 1,
                    status = AttemptStatus.COMPLETED,
                    checks = completeDaysThrough(90),
                ),
            ),
        ).toAttemptRows(today = day(95))

        assertThat(rows.single().outcome).isEqualTo(AttemptOutcomeUi.Completed)
        assertThat(rows.single().endDate).isEqualTo(day(90))
    }

    @Test
    fun attemptsAreListedNewestFirst() {
        val rows = notebook(
            attempts = listOf(
                attemptRecord(number = 1, status = AttemptStatus.FAILED, failedOnDay = 3),
                attemptRecord(number = 2, status = AttemptStatus.FAILED, failedOnDay = 7),
                attemptRecord(number = 3, startDate = day(11)),
            ),
        ).toAttemptRows(today = day(12))

        assertThat(rows.map { it.number }).containsExactly(3, 2, 1)
    }

    @Test
    fun theMiniGridFillsOnlyTheDaysThatWereFinished() {
        val rows = notebook(
            attempts = listOf(
                attemptRecord(
                    number = 1,
                    status = AttemptStatus.FAILED,
                    failedOnDay = 3,
                    checks = completeDaysThrough(2) + partialDay(3),
                ),
            ),
        ).toAttemptRows(today = day(10))

        val markedDays = rows.single().markedDays
        assertThat(markedDays.size).isEqualTo(90)
        assertThat(markedDays.take(4)).containsExactly(true, true, false, false)
    }
}
