package app.snurko.reminders.domain

import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.challenge.domain.READ_ID
import app.snurko.challenge.domain.WORKOUT_ID
import app.snurko.challenge.domain.activeChallenge
import app.snurko.challenge.domain.completeDay
import app.snurko.challenge.domain.day
import app.snurko.challenge.domain.partialDay
import app.snurko.core.domain.DataError
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.Result
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlin.test.BeforeTest
import kotlin.test.Test

class ReminderPlannerTest {

    private lateinit var challengeRepository: FakeChallengeRepository
    private lateinit var reminderRepository: FakeReminderRepository
    private lateinit var scheduler: FakeReminderScheduler

    @BeforeTest
    fun setUp() {
        challengeRepository = FakeChallengeRepository()
        reminderRepository = FakeReminderRepository()
        scheduler = FakeReminderScheduler()
    }

    @Test
    fun reschedulingHandsTheDerivedSetToTheScheduler() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge())
        reminderRepository.setReminders(listOf(Reminder(READ_ID, LocalTime(21, 0))))

        planner(now = day(3).atTime(8, 0)).rescheduleAll()

        assertThat(scheduler.lastSchedule.scheduled).isEqualTo(
            listOf(ScheduledReminder(habitId = READ_ID, fireAt = day(3).atTime(21, 0))),
        )
    }

    @Test
    fun reschedulingWithoutAChallengeCancelsEveryReminder() = runTest {
        challengeRepository.setActiveChallenge(null)
        reminderRepository.setReminders(listOf(Reminder(READ_ID, LocalTime(21, 0))))

        planner(now = day(3).atTime(8, 0)).rescheduleAll()

        assertThat(scheduler.lastSchedule.scheduled).isEmpty()
        assertThat(scheduler.lastSchedule.cancelledHabitIds).containsExactly(READ_ID)
    }

    @Test
    fun reschedulingWithNoRemindersAtAllStillReachesTheScheduler() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge())

        planner(now = day(3).atTime(8, 0)).rescheduleAll()

        assertThat(scheduler.lastSchedule).isEqualTo(ReminderSchedule())
    }

    @Test
    fun aSchedulerThatRechecksAtFireTimeStillGetsAnAlreadyCheckedHabit() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(completeDay(3))))
        reminderRepository.setReminders(listOf(Reminder(WORKOUT_ID, LocalTime(21, 0))))

        planner(now = day(3).atTime(8, 0)).rescheduleAll()

        assertThat(scheduler.lastSchedule.scheduled).isEqualTo(
            listOf(ScheduledReminder(habitId = WORKOUT_ID, fireAt = day(3).atTime(21, 0))),
        )
    }

    @Test
    fun aSchedulerThatCannotRecheckAtFireTimeLosesAnAlreadyCheckedHabit() = runTest {
        scheduler.rechecksAtFireTime = false
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(completeDay(3))))
        reminderRepository.setReminders(listOf(Reminder(WORKOUT_ID, LocalTime(21, 0))))

        planner(now = day(3).atTime(8, 0)).rescheduleAll()

        assertThat(scheduler.lastSchedule.scheduled).isEmpty()
        assertThat(scheduler.lastSchedule.cancelledHabitIds).containsExactly(WORKOUT_ID)
    }

    @Test
    fun aFiringReminderCarriesTheHabitNameTheAttemptAndTheDay() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(partialDay(3))))

        val firing = planner(now = day(3).atTime(21, 0)).firingFor(READ_ID)

        assertThat(firing).isNotNull().isEqualTo(
            ReminderFiring(habitId = READ_ID, habitName = "Read", attemptId = 1, day = 3),
        )
    }

    @Test
    fun anAlreadyCheckedHabitDoesNotFire() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(completeDay(3))))

        assertThat(planner(now = day(3).atTime(21, 0)).firingFor(WORKOUT_ID)).isNull()
    }

    @Test
    fun nothingFiresWithoutAnActiveChallenge() = runTest {
        challengeRepository.setActiveChallenge(null)

        assertThat(planner(now = day(3).atTime(21, 0)).firingFor(WORKOUT_ID)).isNull()
    }

    @Test
    fun nothingFiresAfterTheLastDay() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge())

        assertThat(planner(now = day(91).atTime(21, 0)).firingFor(WORKOUT_ID)).isNull()
    }

    @Test
    fun checkingOffFromTheNotificationMarksTheHabitOnTodaysDay() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(partialDay(3))))

        val checked = planner(now = day(3).atTime(21, 0)).checkOff(READ_ID)

        assertThat(checked).isTrue()
        assertThat(challengeRepository.checkCalls).containsExactly(
            FakeChallengeRepository.CheckCall(attemptId = 1, day = 3, habitId = READ_ID, checked = true),
        )
    }

    @Test
    fun checkingOffAnAlreadyCheckedHabitTouchesNothing() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(completeDay(3))))

        val checked = planner(now = day(3).atTime(21, 0)).checkOff(WORKOUT_ID)

        assertThat(checked).isFalse()
        assertThat(challengeRepository.checkCalls).isEmpty()
    }

    @Test
    fun checkingOffWithoutAnActiveChallengeTouchesNothing() = runTest {
        challengeRepository.setActiveChallenge(null)

        assertThat(planner(now = day(3).atTime(21, 0)).checkOff(WORKOUT_ID)).isFalse()
        assertThat(challengeRepository.checkCalls).isEmpty()
    }

    @Test
    fun aFailedWriteIsReportedBackToTheReceiver() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge())
        challengeRepository.checkResult = Result.Error(DataError.Local.UNKNOWN)

        assertThat(planner(now = day(3).atTime(21, 0)).checkOff(WORKOUT_ID)).isFalse()
    }

    private fun planner(now: LocalDateTime) = ReminderPlanner(
        challengeRepository = challengeRepository,
        reminderRepository = reminderRepository,
        scheduler = scheduler,
        dateProvider = DateProvider { now },
    )
}
