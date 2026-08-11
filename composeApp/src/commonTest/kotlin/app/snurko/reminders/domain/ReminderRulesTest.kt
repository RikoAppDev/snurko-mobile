package app.snurko.reminders.domain

import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.READ_ID
import app.snurko.challenge.domain.WORKOUT_ID
import app.snurko.challenge.domain.activeChallenge
import app.snurko.challenge.domain.completeDay
import app.snurko.challenge.domain.day
import app.snurko.challenge.domain.partialDay
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlin.test.Test

class NextFireAtTest {

    @Test
    fun aTimeStillAheadTodayFiresToday() {
        val fireAt = nextFireAt(time = LocalTime(20, 30), now = LocalDateTime(2026, 3, 17, 8, 0))

        assertThat(fireAt).isEqualTo(LocalDateTime(2026, 3, 17, 20, 30))
    }

    @Test
    fun aTimeAlreadyPassedTodayFiresTomorrow() {
        val fireAt = nextFireAt(time = LocalTime(7, 15), now = LocalDateTime(2026, 3, 17, 8, 0))

        assertThat(fireAt).isEqualTo(LocalDateTime(2026, 3, 18, 7, 15))
    }

    @Test
    fun aTimeThatIsExactlyNowFiresTomorrow() {
        val fireAt = nextFireAt(time = LocalTime(8, 0), now = LocalDateTime(2026, 3, 17, 8, 0))

        assertThat(fireAt).isEqualTo(LocalDateTime(2026, 3, 18, 8, 0))
    }

    @Test
    fun tomorrowCanBeInTheNextMonth() {
        val fireAt = nextFireAt(time = LocalTime(6, 0), now = LocalDateTime(2026, 1, 31, 22, 0))

        assertThat(fireAt).isEqualTo(LocalDateTime(2026, 2, 1, 6, 0))
    }

    @Test
    fun secondsWithinTheCurrentMinuteDoNotPushTheReminderToTomorrow() {
        val fireAt = nextFireAt(time = LocalTime(8, 1), now = LocalDateTime(2026, 3, 17, 8, 0, 59))

        assertThat(fireAt).isEqualTo(LocalDateTime(2026, 3, 17, 8, 1))
    }
}

class ShouldRemindTest {

    @Test
    fun anUncheckedHabitOnAnActiveDayIsWorthReminding() {
        val challenge = activeChallenge(checks = listOf(partialDay(3)))

        assertThat(challenge.shouldRemind(habitId = READ_ID, date = day(3))).isTrue()
    }

    @Test
    fun anAlreadyCheckedHabitIsNotWorthReminding() {
        val challenge = activeChallenge(checks = listOf(partialDay(3)))

        assertThat(challenge.shouldRemind(habitId = WORKOUT_ID, date = day(3))).isFalse()
    }

    @Test
    fun aHabitCheckedOnAnotherDayIsStillWorthRemindingToday() {
        val challenge = activeChallenge(checks = listOf(completeDay(2)))

        assertThat(challenge.shouldRemind(habitId = WORKOUT_ID, date = day(3))).isTrue()
    }

    @Test
    fun nothingIsWorthRemindingAfterTheLastDay() {
        val challenge = activeChallenge()

        assertThat(challenge.shouldRemind(habitId = WORKOUT_ID, date = day(91))).isFalse()
    }

    @Test
    fun nothingIsWorthRemindingBeforeTheAttemptStarted() {
        val challenge = activeChallenge()

        assertThat(challenge.shouldRemind(habitId = WORKOUT_ID, date = day(0))).isFalse()
    }

    @Test
    fun aCompletedAttemptRemindsOfNothing() {
        val challenge = activeChallenge().let { it.copy(attempt = it.attempt.copy(status = AttemptStatus.COMPLETED)) }

        assertThat(challenge.shouldRemind(habitId = WORKOUT_ID, date = day(3))).isFalse()
    }

    @Test
    fun aFailedAttemptRemindsOfNothing() {
        val challenge = activeChallenge().let { it.copy(attempt = it.attempt.copy(status = AttemptStatus.FAILED)) }

        assertThat(challenge.shouldRemind(habitId = WORKOUT_ID, date = day(3))).isFalse()
    }

    @Test
    fun aHabitThatIsNotOnTheListIsNeverWorthReminding() {
        val challenge = activeChallenge()

        assertThat(challenge.shouldRemind(habitId = 404L, date = day(3))).isFalse()
    }
}

class ReminderScheduleTest {

    private val morning = LocalTime(7, 30)
    private val evening = LocalTime(21, 0)

    @Test
    fun withoutAnActiveChallengeEveryReminderIsCancelled() {
        val schedule = reminderSchedule(
            active = null,
            reminders = listOf(Reminder(WORKOUT_ID, morning), Reminder(READ_ID, evening)),
            now = day(3).atTime(8, 0),
        )

        assertThat(schedule.scheduled).isEmpty()
        assertThat(schedule.cancelledHabitIds).containsExactlyInAnyOrder(WORKOUT_ID, READ_ID)
    }

    @Test
    fun aReminderStillAheadTodayIsScheduledForToday() {
        val schedule = reminderSchedule(
            active = activeChallenge(),
            reminders = listOf(Reminder(READ_ID, evening)),
            now = day(3).atTime(8, 0),
        )

        assertThat(schedule.scheduled).containsExactly(
            ScheduledReminder(habitId = READ_ID, fireAt = day(3).atTime(21, 0)),
        )
        assertThat(schedule.cancelledHabitIds).isEmpty()
    }

    @Test
    fun aReminderAlreadyPassedTodayIsScheduledForTomorrow() {
        val schedule = reminderSchedule(
            active = activeChallenge(),
            reminders = listOf(Reminder(WORKOUT_ID, morning)),
            now = day(3).atTime(8, 0),
        )

        assertThat(schedule.scheduled).containsExactly(
            ScheduledReminder(habitId = WORKOUT_ID, fireAt = day(4).atTime(7, 30)),
        )
    }

    @Test
    fun anAlreadyCheckedHabitKeepsItsReminderBecauseTheDayCanStillBeUnchecked() {
        val schedule = reminderSchedule(
            active = activeChallenge(checks = listOf(completeDay(3))),
            reminders = listOf(Reminder(WORKOUT_ID, evening)),
            now = day(3).atTime(8, 0),
        )

        assertThat(schedule.scheduled).containsExactly(
            ScheduledReminder(habitId = WORKOUT_ID, fireAt = day(3).atTime(21, 0)),
        )
    }

    @Test
    fun aReminderLandingAfterTheLastDayIsCancelled() {
        val schedule = reminderSchedule(
            active = activeChallenge(),
            reminders = listOf(Reminder(WORKOUT_ID, morning)),
            now = day(90).atTime(8, 0),
        )

        assertThat(schedule.scheduled).isEmpty()
        assertThat(schedule.cancelledHabitIds).containsExactly(WORKOUT_ID)
    }

    @Test
    fun aReminderOnTheLastDayItselfIsKept() {
        val schedule = reminderSchedule(
            active = activeChallenge(),
            reminders = listOf(Reminder(WORKOUT_ID, evening)),
            now = day(90).atTime(8, 0),
        )

        assertThat(schedule.scheduled).containsExactly(
            ScheduledReminder(habitId = WORKOUT_ID, fireAt = day(90).atTime(21, 0)),
        )
    }

    @Test
    fun aCompletedChallengeCancelsEverything() {
        val active = activeChallenge()
            .let { it.copy(attempt = it.attempt.copy(status = AttemptStatus.COMPLETED)) }

        val schedule = reminderSchedule(
            active = active,
            reminders = listOf(Reminder(WORKOUT_ID, evening)),
            now = day(90).atTime(8, 0),
        )

        assertThat(schedule.scheduled).isEmpty()
        assertThat(schedule.cancelledHabitIds).containsExactly(WORKOUT_ID)
    }

    @Test
    fun aReminderForAHabitTheChallengeNoLongerHasIsCancelled() {
        val schedule = reminderSchedule(
            active = activeChallenge(),
            reminders = listOf(Reminder(404L, evening)),
            now = day(3).atTime(8, 0),
        )

        assertThat(schedule.scheduled).isEmpty()
        assertThat(schedule.cancelledHabitIds).containsExactly(404L)
    }

    @Test
    fun everyHabitIsScheduledOrCancelledOnceAndNeverBoth() {
        val schedule = reminderSchedule(
            active = activeChallenge(),
            reminders = listOf(Reminder(WORKOUT_ID, evening), Reminder(READ_ID, morning), Reminder(404L, evening)),
            now = day(3).atTime(8, 0),
        )

        assertThat(schedule.scheduled.map { it.habitId }).containsExactlyInAnyOrder(WORKOUT_ID, READ_ID)
        assertThat(schedule.cancelledHabitIds).containsExactly(404L)
    }

    @Test
    fun aCheckedHabitLosesItsReminderWhereNothingRechecksAtFireTime() {
        val schedule = reminderSchedule(
            active = activeChallenge(checks = listOf(completeDay(3))),
            reminders = listOf(Reminder(WORKOUT_ID, evening)),
            now = day(3).atTime(8, 0),
            rechecksAtFireTime = false,
        )

        assertThat(schedule.scheduled).isEmpty()
        assertThat(schedule.cancelledHabitIds).containsExactly(WORKOUT_ID)
    }

    @Test
    fun anUncheckedHabitKeepsItsReminderWhereNothingRechecksAtFireTime() {
        val schedule = reminderSchedule(
            active = activeChallenge(checks = listOf(partialDay(3))),
            reminders = listOf(Reminder(READ_ID, evening)),
            now = day(3).atTime(8, 0),
            rechecksAtFireTime = false,
        )

        assertThat(schedule.scheduled).containsExactly(
            ScheduledReminder(habitId = READ_ID, fireAt = day(3).atTime(21, 0)),
        )
    }

    @Test
    fun aHabitCheckedTodayKeepsTomorrowsReminderWhereNothingRechecksAtFireTime() {
        val schedule = reminderSchedule(
            active = activeChallenge(checks = listOf(completeDay(3))),
            reminders = listOf(Reminder(WORKOUT_ID, morning)),
            now = day(3).atTime(8, 0),
            rechecksAtFireTime = false,
        )

        assertThat(schedule.scheduled).containsExactly(
            ScheduledReminder(habitId = WORKOUT_ID, fireAt = day(4).atTime(7, 30)),
        )
    }

    @Test
    fun aNewAttemptStartedTodayIsRemindedFromDayOne() {
        val schedule = reminderSchedule(
            active = activeChallenge(attemptNumber = 2, attemptStart = LocalDate(2026, 6, 1)),
            reminders = listOf(Reminder(WORKOUT_ID, evening)),
            now = LocalDate(2026, 6, 1).atTime(8, 0),
        )

        assertThat(schedule.scheduled).containsExactly(
            ScheduledReminder(habitId = WORKOUT_ID, fireAt = LocalDate(2026, 6, 1).atTime(21, 0)),
        )
    }
}
