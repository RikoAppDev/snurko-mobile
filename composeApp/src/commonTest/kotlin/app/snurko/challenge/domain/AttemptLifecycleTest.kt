package app.snurko.challenge.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class AttemptLifecycleTest {

    @Test
    fun attemptIsOngoingWhileTodayIsStillPending() {
        val challenge = activeChallenge(checks = completeDaysThrough(3) + partialDay(4))

        assertThat(challenge.evaluate(today = day(4))).isEqualTo(AttemptOutcome.Ongoing)
    }

    @Test
    fun attemptFailsOnTheFirstMissedDay() {
        val challenge = activeChallenge(checks = completeDaysThrough(3))

        assertThat(challenge.evaluate(today = day(5))).isEqualTo(AttemptOutcome.Failed(failedOnDay = 4))
    }

    @Test
    fun attemptFailureReportsTheEarliestMissedDay() {
        val challenge = activeChallenge(
            checks = listOf(completeDay(1), partialDay(2), completeDay(3), partialDay(4)),
        )

        assertThat(challenge.evaluate(today = day(5))).isEqualTo(AttemptOutcome.Failed(failedOnDay = 2))
    }

    @Test
    fun attemptFailsOnDayOneWhenTheFirstDayIsMissed() {
        val challenge = activeChallenge()

        assertThat(challenge.evaluate(today = day(2))).isEqualTo(AttemptOutcome.Failed(failedOnDay = 1))
    }

    @Test
    fun attemptIsOngoingOnDayNinetyUntilTheDayPasses() {
        val challenge = activeChallenge(checks = completeDaysThrough(89) + partialDay(90))

        assertThat(challenge.evaluate(today = day(90))).isEqualTo(AttemptOutcome.Ongoing)
    }

    @Test
    fun attemptFailsOnDayNinetyWhenTheDayPassesIncomplete() {
        val challenge = activeChallenge(checks = completeDaysThrough(89))

        assertThat(challenge.evaluate(today = day(91))).isEqualTo(AttemptOutcome.Failed(failedOnDay = 90))
    }

    @Test
    fun attemptIsCompletedWhenDayNinetyIsFullyChecked() {
        val challenge = activeChallenge(checks = completeDaysThrough(90))

        assertThat(challenge.evaluate(today = day(90))).isEqualTo(AttemptOutcome.Completed)
    }

    @Test
    fun attemptIsOngoingOnTheDayBeforeTheLastDay() {
        val challenge = activeChallenge(checks = completeDaysThrough(89))

        assertThat(challenge.evaluate(today = day(89))).isEqualTo(AttemptOutcome.Ongoing)
    }

    @Test
    fun failedAttemptRecordsTheDayItWasLostOn() {
        val attempt = activeChallenge().attempt

        val failed = attempt.failedOn(day = 4)

        assertThat(failed.status).isEqualTo(AttemptStatus.FAILED)
        assertThat(failed.failedOnDay).isEqualTo(4)
        assertThat(failed.number).isEqualTo(attempt.number)
    }

    @Test
    fun completedAttemptHasNoFailedDay() {
        val completed = activeChallenge().attempt.completed()

        assertThat(completed.status).isEqualTo(AttemptStatus.COMPLETED)
        assertThat(completed.failedOnDay).isNull()
    }

    @Test
    fun restartStartsANewActiveAttemptOnItsOwnDayOne() {
        val failed = activeChallenge().attempt.failedOn(day = 4)

        val next = failed.nextAttempt(startDate = day(5))

        assertThat(next.number).isEqualTo(2)
        assertThat(next.status).isEqualTo(AttemptStatus.ACTIVE)
        assertThat(next.failedOnDay).isNull()
        assertThat(next.dayIndexOn(day(5))).isEqualTo(1)
    }

    @Test
    fun restartedAttemptIsOngoingOnItsFirstDay() {
        val challenge = activeChallenge(attemptNumber = 2, attemptStart = day(5))

        assertThat(challenge.evaluate(today = day(5))).isEqualTo(AttemptOutcome.Ongoing)
    }
}
