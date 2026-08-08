package app.snurko.reminders.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import app.snurko.challenge.data.RoomChallengeRepository
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.Habit
import app.snurko.core.database.SnurkoDatabase
import app.snurko.reminders.domain.Reminder
import app.snurko.reminders.domain.ReminderRepository
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RoomReminderRepositoryTest {

    private lateinit var database: SnurkoDatabase
    private lateinit var challengeRepository: ChallengeRepository
    private lateinit var repository: ReminderRepository

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<SnurkoDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        challengeRepository = RoomChallengeRepository(
            challengeDao = database.challengeDao(),
            attemptDao = database.attemptDao(),
            dayCheckDao = database.dayCheckDao(),
        )
        repository = RoomReminderRepository(database.reminderDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun everyHabitStartsWithoutAReminder() = runTest {
        habitIds()

        assertThat(repository.getReminders()).isEmpty()
    }

    @Test
    fun aReminderTimeIsReadBackForItsHabit() = runTest {
        val workoutId = habitIds().first()

        repository.setReminderTime(workoutId, LocalTime(6, 45))

        assertThat(repository.getReminders())
            .containsExactly(Reminder(habitId = workoutId, time = LocalTime(6, 45)))
    }

    @Test
    fun settingATimeTwiceKeepsOneReminderPerHabit() = runTest {
        val workoutId = habitIds().first()

        repository.setReminderTime(workoutId, LocalTime(6, 45))
        repository.setReminderTime(workoutId, LocalTime(20, 15))

        assertThat(repository.getReminders())
            .containsExactly(Reminder(habitId = workoutId, time = LocalTime(20, 15)))
    }

    @Test
    fun aNullTimeSwitchesTheReminderOff() = runTest {
        val workoutId = habitIds().first()
        repository.setReminderTime(workoutId, LocalTime(6, 45))

        repository.setReminderTime(workoutId, null)

        assertThat(repository.getReminders()).isEmpty()
    }

    @Test
    fun theReminderListIsObservedAsItChanges() = runTest {
        val workoutId = habitIds().first()

        repository.observeReminders().test {
            assertThat(awaitItem()).isEmpty()

            repository.setReminderTime(workoutId, LocalTime(6, 45))

            assertThat(awaitItem())
                .containsExactly(Reminder(habitId = workoutId, time = LocalTime(6, 45)))
        }
    }

    @Test
    fun midnightSurvivesTheRoundTrip() = runTest {
        val workoutId = habitIds().first()

        repository.setReminderTime(workoutId, LocalTime(0, 0))

        assertThat(repository.getReminders())
            .containsExactly(Reminder(habitId = workoutId, time = LocalTime(0, 0)))
    }

    private suspend fun habitIds(): List<Long> {
        challengeRepository.createChallenge(
            name = "90 days",
            startDate = LocalDate(2026, 1, 1),
            habits = listOf(Habit(name = "Workout"), Habit(name = "Read")),
        )
        return challengeRepository.observeActiveChallenge().first()!!.challenge.habits.map { it.id }
    }
}
