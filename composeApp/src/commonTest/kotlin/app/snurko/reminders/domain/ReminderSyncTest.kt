package app.snurko.reminders.domain

import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.challenge.domain.READ_ID
import app.snurko.challenge.domain.WORKOUT_ID
import app.snurko.challenge.domain.activeChallenge
import app.snurko.challenge.domain.completeDay
import app.snurko.challenge.domain.day
import app.snurko.challenge.domain.partialDay
import app.snurko.core.domain.DateProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlin.test.BeforeTest
import kotlin.test.Test

class ReminderSyncTest {

    private lateinit var challengeRepository: FakeChallengeRepository
    private lateinit var reminderRepository: FakeReminderRepository
    private lateinit var scheduler: FakeReminderScheduler
    private var now: LocalDateTime = day(3).atTime(8, 0)

    @BeforeTest
    fun setUp() {
        challengeRepository = FakeChallengeRepository()
        reminderRepository = FakeReminderRepository()
        scheduler = FakeReminderScheduler(rechecksAtFireTime = false)
        now = day(3).atTime(8, 0)
    }

    @Test
    fun theSetIsArmedAsSoonAsSyncStarts() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(partialDay(3))))
        reminderRepository.setReminders(listOf(Reminder(READ_ID, LocalTime(21, 0))))

        start()

        assertThat(scheduler.lastSchedule.scheduled).containsExactly(
            ScheduledReminder(habitId = READ_ID, fireAt = day(3).atTime(21, 0)),
        )
    }

    @Test
    fun checkingAHabitOffDropsItsReminderForTheRestOfTheDay() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(partialDay(3))))
        reminderRepository.setReminders(listOf(Reminder(READ_ID, LocalTime(21, 0))))
        start()

        challengeRepository.setHabitChecked(attemptId = 1, day = 3, habitId = READ_ID, checked = true)
        runCurrent()

        assertThat(scheduler.lastSchedule.scheduled).isEmpty()
        assertThat(scheduler.lastSchedule.cancelledHabitIds).containsExactly(READ_ID)
    }

    @Test
    fun uncheckingAHabitArmsItsReminderAgain() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(completeDay(3))))
        reminderRepository.setReminders(listOf(Reminder(WORKOUT_ID, LocalTime(21, 0))))
        start()

        challengeRepository.setHabitChecked(attemptId = 1, day = 3, habitId = WORKOUT_ID, checked = false)
        runCurrent()

        assertThat(scheduler.lastSchedule.scheduled).containsExactly(
            ScheduledReminder(habitId = WORKOUT_ID, fireAt = day(3).atTime(21, 0)),
        )
    }

    @Test
    fun aNewReminderTimeReachesTheScheduler() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(partialDay(3))))
        start()

        reminderRepository.setReminderTime(habitId = READ_ID, time = LocalTime(20, 0))
        runCurrent()

        assertThat(scheduler.lastSchedule.scheduled).containsExactly(
            ScheduledReminder(habitId = READ_ID, fireAt = day(3).atTime(20, 0)),
        )
    }

    @Test
    fun comingBackToTheForegroundReReadsTheSetAgainstTheCurrentTime() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(partialDay(3))))
        reminderRepository.setReminders(listOf(Reminder(READ_ID, LocalTime(21, 0))))
        val sync = start()

        now = day(4).atTime(8, 0)
        sync.onForeground()
        runCurrent()

        assertThat(scheduler.lastSchedule.scheduled).containsExactly(
            ScheduledReminder(habitId = READ_ID, fireAt = day(4).atTime(21, 0)),
        )
    }

    @Test
    fun aForegroundBeforeAnythingElseHappenedStillArmsTheSet() = runTest {
        challengeRepository.setActiveChallenge(activeChallenge(checks = listOf(partialDay(3))))
        reminderRepository.setReminders(listOf(Reminder(READ_ID, LocalTime(21, 0))))
        val sync = sync()

        sync.onForeground()
        sync.start(backgroundScope)
        runCurrent()

        assertThat(scheduler.lastSchedule.scheduled).containsExactly(
            ScheduledReminder(habitId = READ_ID, fireAt = day(3).atTime(21, 0)),
        )
    }

    private fun TestScope.start(): ReminderSync {
        val sync = sync()
        sync.start(backgroundScope)
        runCurrent()
        return sync
    }

    private fun sync(): ReminderSync = ReminderSync(
        challengeRepository = challengeRepository,
        reminderRepository = reminderRepository,
        planner = ReminderPlanner(
            challengeRepository = challengeRepository,
            reminderRepository = reminderRepository,
            scheduler = scheduler,
            dateProvider = DateProvider { now },
        ),
    )
}
