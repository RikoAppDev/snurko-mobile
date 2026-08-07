package app.snurko.challenge.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class StreakTest {

    @Test
    fun streakIsZeroOnDayOneBeforeAnythingIsChecked() {
        assertThat(activeChallenge().currentStreak(today = day(1))).isEqualTo(0)
    }

    @Test
    fun streakIsOneWhenDayOneIsComplete() {
        val challenge = activeChallenge(checks = listOf(completeDay(1)))

        assertThat(challenge.currentStreak(today = day(1))).isEqualTo(1)
    }

    @Test
    fun pendingTodayDoesNotBreakTheStreak() {
        val challenge = activeChallenge(checks = completeDaysThrough(6) + partialDay(7))

        assertThat(challenge.currentStreak(today = day(7))).isEqualTo(6)
    }

    @Test
    fun streakCountsConsecutiveCompleteDaysIncludingToday() {
        val challenge = activeChallenge(checks = completeDaysThrough(7))

        assertThat(challenge.currentStreak(today = day(7))).isEqualTo(7)
    }

    @Test
    fun streakStopsAtTheFirstIncompleteDay() {
        val challenge = activeChallenge(
            checks = listOf(completeDay(1), completeDay(2), partialDay(3), completeDay(4), completeDay(5)),
        )

        assertThat(challenge.currentStreak(today = day(5))).isEqualTo(2)
    }

    @Test
    fun streakIsZeroWhenTheVeryFirstDayWasMissed() {
        val challenge = activeChallenge(checks = listOf(completeDay(2)))

        assertThat(challenge.currentStreak(today = day(2))).isEqualTo(0)
    }

    @Test
    fun streakIsNinetyWhenTheWholeChallengeIsComplete() {
        val challenge = activeChallenge(checks = completeDaysThrough(90))

        assertThat(challenge.currentStreak(today = day(90))).isEqualTo(90)
    }

    @Test
    fun streakNeverExceedsTheChallengeLength() {
        val challenge = activeChallenge(checks = completeDaysThrough(90))

        assertThat(challenge.currentStreak(today = day(95))).isEqualTo(90)
    }

    @Test
    fun streakIsZeroBeforeTheAttemptHasStarted() {
        val challenge = activeChallenge(attemptNumber = 2, attemptStart = day(10))

        assertThat(challenge.currentStreak(today = day(9))).isEqualTo(0)
    }

    @Test
    fun restartedAttemptStreakIsZeroOnItsFirstDay() {
        val challenge = activeChallenge(attemptNumber = 2, attemptStart = day(10))

        assertThat(challenge.currentStreak(today = day(10))).isEqualTo(0)
    }
}
