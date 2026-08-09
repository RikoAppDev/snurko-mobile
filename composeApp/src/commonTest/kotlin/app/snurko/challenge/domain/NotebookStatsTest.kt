package app.snurko.challenge.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class NotebookStatsTest {

    @Test
    fun anUntouchedNotebookCountsNothing() {
        val stats = notebook().stats()

        assertThat(stats).isEqualTo(
            NotebookStats(bestCompletionPercent = 0, longestStreak = 0, totalMarks = 0),
        )
    }

    @Test
    fun onlyFullyFinishedDaysCountAsAMark() {
        val stats = notebook(
            attempts = listOf(attemptRecord(checks = completeDaysThrough(2) + partialDay(3))),
        ).stats()

        assertThat(stats.totalMarks).isEqualTo(2)
    }

    @Test
    fun theMarksOfEveryAttemptAddUp() {
        val stats = notebook(
            attempts = listOf(
                attemptRecord(number = 1, status = AttemptStatus.FAILED, checks = completeDaysThrough(5)),
                attemptRecord(number = 2, checks = completeDaysThrough(3)),
            ),
        ).stats()

        assertThat(stats.totalMarks).isEqualTo(8)
    }

    @Test
    fun aFinishedChallengeIsAHundredPerCent() {
        val stats = notebook(
            attempts = listOf(attemptRecord(status = AttemptStatus.COMPLETED, checks = completeDaysThrough(90))),
        ).stats()

        assertThat(stats.bestCompletionPercent).isEqualTo(100)
    }

    @Test
    fun aCompletionShortOfEveryDayNeverRoundsUpToAHundred() {
        val stats = notebook(
            attempts = listOf(attemptRecord(checks = completeDaysThrough(89))),
        ).stats()

        assertThat(stats.bestCompletionPercent).isEqualTo(98)
    }

    @Test
    fun theBestAttemptIsTheOneTheCompletionSpeaksFor() {
        val stats = notebook(
            attempts = listOf(
                attemptRecord(number = 1, status = AttemptStatus.FAILED, checks = completeDaysThrough(45)),
                attemptRecord(number = 2, checks = completeDaysThrough(9)),
            ),
        ).stats()

        assertThat(stats.bestCompletionPercent).isEqualTo(50)
    }

    @Test
    fun theLongestStreakIsTheLongestUnbrokenRunInAnAttempt() {
        val attempt = attemptRecord(
            checks = completeDaysThrough(3) + completeDay(5) + completeDay(6) + completeDay(7) + completeDay(8),
        )

        assertThat(notebook(attempts = listOf(attempt)).stats().longestStreak).isEqualTo(4)
    }

    @Test
    fun aStreakNeverRunsAcrossTwoAttempts() {
        val stats = notebook(
            attempts = listOf(
                attemptRecord(number = 1, status = AttemptStatus.FAILED, checks = completeDaysThrough(4)),
                attemptRecord(number = 2, checks = completeDaysThrough(3)),
            ),
        ).stats()

        assertThat(stats.longestStreak).isEqualTo(4)
    }

    @Test
    fun aGapOnTheFirstDayDoesNotHideTheStreakBehindIt() {
        val attempt = attemptRecord(checks = listOf(completeDay(2), completeDay(3)))

        assertThat(notebook(attempts = listOf(attempt)).stats().longestStreak).isEqualTo(2)
    }

    @Test
    fun daysCheckedBeyondTheChallengeLengthAreNotCounted() {
        val attempt = attemptRecord(checks = completeDaysThrough(12))

        val stats = notebook(attempts = listOf(attempt), lengthDays = 10).stats()

        assertThat(stats.totalMarks).isEqualTo(10)
        assertThat(stats.bestCompletionPercent).isEqualTo(100)
    }

    @Test
    fun oneAttemptCountsItsOwnMarksAndCompletion() {
        val challenge = notebook().challenge
        val attempt = attemptRecord(checks = completeDaysThrough(18))

        assertThat(attempt.markedDays(challenge)).isEqualTo(18)
        assertThat(attempt.completionPercent(challenge)).isEqualTo(20)
        assertThat(attempt.longestStreak(challenge)).isEqualTo(18)
    }
}
