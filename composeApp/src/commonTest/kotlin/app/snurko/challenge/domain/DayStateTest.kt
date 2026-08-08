package app.snurko.challenge.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class DayStateTest {

    @Test
    fun dayIndexIsOneOnTheStartDate() {
        assertThat(activeChallenge().dayIndexOn(challengeStart)).isEqualTo(1)
    }

    @Test
    fun dayIndexCountsCalendarDaysFromTheStartDate() {
        val challenge = activeChallenge()

        assertThat(challenge.dayIndexOn(day(2))).isEqualTo(2)
        assertThat(challenge.dayIndexOn(day(90))).isEqualTo(90)
    }

    @Test
    fun dayIsCompleteWhenEveryHabitIsChecked() {
        val challenge = activeChallenge(checks = listOf(completeDay(1)))

        assertThat(challenge.dayState(day = 1, today = day(1))).isEqualTo(DayState.COMPLETE)
    }

    @Test
    fun dayIsPendingWhenTodayIsPartiallyChecked() {
        val challenge = activeChallenge(checks = listOf(partialDay(1)))

        assertThat(challenge.dayState(day = 1, today = day(1))).isEqualTo(DayState.PENDING)
    }

    @Test
    fun dayIsPendingWhenTodayHasNoChecks() {
        val challenge = activeChallenge(checks = completeDaysThrough(4))

        assertThat(challenge.dayState(day = 5, today = day(5))).isEqualTo(DayState.PENDING)
    }

    @Test
    fun dayIsFailedWhenAPassedDayIsPartiallyChecked() {
        val challenge = activeChallenge(checks = listOf(partialDay(1)))

        assertThat(challenge.dayState(day = 1, today = day(2))).isEqualTo(DayState.FAILED)
    }

    @Test
    fun dayIsFailedWhenAPassedDayHasNoChecks() {
        val challenge = activeChallenge()

        assertThat(challenge.dayState(day = 1, today = day(2))).isEqualTo(DayState.FAILED)
    }

    @Test
    fun passedDayStaysCompleteWhenFullyChecked() {
        val challenge = activeChallenge(checks = completeDaysThrough(3))

        assertThat(challenge.dayState(day = 1, today = day(4))).isEqualTo(DayState.COMPLETE)
    }

    @Test
    fun futureDayIsPending() {
        val challenge = activeChallenge(checks = completeDaysThrough(1))

        assertThat(challenge.dayState(day = 7, today = day(1))).isEqualTo(DayState.PENDING)
    }
}
