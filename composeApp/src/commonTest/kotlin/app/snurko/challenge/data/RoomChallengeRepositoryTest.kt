package app.snurko.challenge.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import app.snurko.challenge.domain.Attempt
import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.CHALLENGE_LENGTH_DAYS
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.DayCheck
import app.snurko.challenge.domain.Habit
import app.snurko.core.database.AttemptDao
import app.snurko.core.database.SnurkoDatabase
import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class RoomChallengeRepositoryTest {

    private lateinit var database: SnurkoDatabase
    private lateinit var attemptDao: AttemptDao
    private lateinit var repository: ChallengeRepository

    private val startDate = LocalDate(2026, 1, 1)
    private val habits = listOf(
        Habit(name = "Workout", failCondition = "less than 30 minutes"),
        Habit(name = "Read", failCondition = "fewer than 10 pages"),
        Habit(name = "No alcohol"),
    )

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<SnurkoDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        attemptDao = database.attemptDao()
        repository = RoomChallengeRepository(
            challengeDao = database.challengeDao(),
            attemptDao = attemptDao,
            dayCheckDao = database.dayCheckDao(),
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun activeChallengeIsNullBeforeAnyChallengeIsCreated() = runTest {
        assertThat(repository.observeActiveChallenge().first()).isNull()
    }

    @Test
    fun createdChallengeIsReadBackWithItsHabitsInOrder() = runTest {
        repository.createChallenge(name = "90 days", startDate = startDate, habits = habits)

        val active = repository.observeActiveChallenge().first()

        assertThat(active).isNotNull()
        assertThat(active!!.challenge.name).isEqualTo("90 days")
        assertThat(active.challenge.startDate).isEqualTo(startDate)
        assertThat(active.challenge.lengthDays).isEqualTo(CHALLENGE_LENGTH_DAYS)
        assertThat(active.challenge.habits.map { it.name }).containsExactly("Workout", "Read", "No alcohol")
        assertThat(active.challenge.habits.map { it.failCondition })
            .containsExactly("less than 30 minutes", "fewer than 10 pages", null)
    }

    @Test
    fun createdChallengeStartsWithAnActiveFirstAttemptAndNoChecks() = runTest {
        repository.createChallenge(name = "90 days", startDate = startDate, habits = habits)

        val active = repository.observeActiveChallenge().first()!!

        assertThat(active.attempt.number).isEqualTo(1)
        assertThat(active.attempt.status).isEqualTo(AttemptStatus.ACTIVE)
        assertThat(active.attempt.startDate).isEqualTo(startDate)
        assertThat(active.attempt.failedOnDay).isNull()
        assertThat(active.checks).isEmpty()
    }

    @Test
    fun checkedHabitsArePersistedPerDay() = runTest {
        val active = createChallenge()
        val workoutId = active.challenge.habits[0].id
        val readId = active.challenge.habits[1].id

        repository.setHabitChecked(active.attempt.id, day = 1, habitId = workoutId, checked = true)
        repository.setHabitChecked(active.attempt.id, day = 1, habitId = readId, checked = true)
        repository.setHabitChecked(active.attempt.id, day = 2, habitId = workoutId, checked = true)

        assertThat(repository.observeActiveChallenge().first()!!.checks).containsExactly(
            DayCheck(day = 1, checkedHabitIds = setOf(workoutId, readId)),
            DayCheck(day = 2, checkedHabitIds = setOf(workoutId)),
        )
    }

    @Test
    fun uncheckingAHabitRemovesItFromTheDay() = runTest {
        val active = createChallenge()
        val workoutId = active.challenge.habits[0].id
        val readId = active.challenge.habits[1].id
        repository.setHabitChecked(active.attempt.id, day = 1, habitId = workoutId, checked = true)
        repository.setHabitChecked(active.attempt.id, day = 1, habitId = readId, checked = true)

        repository.setHabitChecked(active.attempt.id, day = 1, habitId = readId, checked = false)

        assertThat(repository.observeActiveChallenge().first()!!.checks).containsExactly(
            DayCheck(day = 1, checkedHabitIds = setOf(workoutId)),
        )
    }

    @Test
    fun checkingTheSameHabitTwiceIsRecordedOnce() = runTest {
        val active = createChallenge()
        val workoutId = active.challenge.habits[0].id

        repository.setHabitChecked(active.attempt.id, day = 1, habitId = workoutId, checked = true)
        val secondCheck =
            repository.setHabitChecked(active.attempt.id, day = 1, habitId = workoutId, checked = true)

        assertThat(secondCheck).isEqualTo(Result.Success(Unit))
        assertThat(repository.observeActiveChallenge().first()!!.checks).containsExactly(
            DayCheck(day = 1, checkedHabitIds = setOf(workoutId)),
        )
    }

    @Test
    fun activeChallengeFlowEmitsWhenAHabitIsChecked() = runTest {
        createChallenge()

        repository.observeActiveChallenge().test {
            val initial = awaitItem()!!
            assertThat(initial.checks).isEmpty()

            val habitId = initial.challenge.habits.first().id
            repository.setHabitChecked(initial.attempt.id, day = 1, habitId = habitId, checked = true)

            assertThat(awaitItem()!!.checks).containsExactly(
                DayCheck(day = 1, checkedHabitIds = setOf(habitId)),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aCreatedChallengeReachesTheFlowWithoutAPerceptibleDelay() = runTest {
        repository.observeActiveChallenge().test {
            assertThat(awaitItem()).isNull()

            repository.createChallenge(name = "90 days", startDate = startDate, habits = habits)
            val afterWrite = TimeSource.Monotonic.markNow()
            assertThat(awaitItem()).isNotNull()

            assertThat(afterWrite.elapsedNow()).isLessThan(MAX_EMISSION_DELAY)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aCreatedChallengeIsEmittedAsASingleConsistentState() = runTest {
        repository.observeActiveChallenge().test {
            assertThat(awaitItem()).isNull()

            repository.createChallenge(name = "90 days", startDate = startDate, habits = habits)

            val active = awaitItem()
            assertThat(active).isNotNull()
            assertThat(active!!.challenge.habits).hasSize(habits.size)
            expectNoEvents()
        }
    }

    @Test
    fun checkingAHabitForAnUnknownAttemptFails() = runTest {
        val active = createChallenge()

        val result = repository.setHabitChecked(
            attemptId = active.attempt.id + 404,
            day = 1,
            habitId = active.challenge.habits.first().id,
            checked = true,
        )

        assertThat(result).isEqualTo(Result.Error(DataError.Local.NOT_FOUND))
    }

    @Test
    fun failingAnAttemptArchivesItWithTheDayItWasLostOn() = runTest {
        val active = createChallenge()

        repository.failAttemptAndStartNext(
            attemptId = active.attempt.id,
            failedOnDay = 4,
            nextAttemptStartDate = LocalDate(2026, 1, 5),
        )

        val archived = attemptDao.getById(active.attempt.id)!!
        assertThat(archived.status).isEqualTo(AttemptStatus.FAILED)
        assertThat(archived.failedOnDay).isEqualTo(4)
    }

    @Test
    fun failingAnAttemptStartsTheNextAttemptAtDayOne() = runTest {
        val active = createChallenge()
        val restart = LocalDate(2026, 1, 5)

        repository.failAttemptAndStartNext(
            attemptId = active.attempt.id,
            failedOnDay = 4,
            nextAttemptStartDate = restart,
        )

        val next = repository.observeActiveChallenge().first()!!.attempt
        assertThat(next.number).isEqualTo(2)
        assertThat(next.status).isEqualTo(AttemptStatus.ACTIVE)
        assertThat(next.startDate).isEqualTo(restart)
        assertThat(next.failedOnDay).isNull()
    }

    @Test
    fun checksOfAFailedAttemptStayWithThatAttempt() = runTest {
        val active = createChallenge()
        val habitId = active.challenge.habits.first().id
        repository.setHabitChecked(active.attempt.id, day = 1, habitId = habitId, checked = true)

        repository.failAttemptAndStartNext(
            attemptId = active.attempt.id,
            failedOnDay = 2,
            nextAttemptStartDate = LocalDate(2026, 1, 3),
        )

        val restarted = repository.observeActiveChallenge().first()!!
        assertThat(restarted.checks).isEmpty()
        assertThat(restarted.challenge.habits.map { it.id }).isEqualTo(active.challenge.habits.map { it.id })
    }

    @Test
    fun failingAnUnknownAttemptFails() = runTest {
        createChallenge()

        val result = repository.failAttemptAndStartNext(
            attemptId = 404,
            failedOnDay = 4,
            nextAttemptStartDate = startDate,
        )

        assertThat(result).isEqualTo(Result.Error(DataError.Local.NOT_FOUND))
    }

    @Test
    fun completingAnAttemptArchivesIt() = runTest {
        val active = createChallenge()

        repository.completeAttempt(active.attempt.id)

        val completed: Attempt? = attemptDao.getById(active.attempt.id)?.toAttempt()
        assertThat(completed!!.status).isEqualTo(AttemptStatus.COMPLETED)
        assertThat(completed.failedOnDay).isNull()
    }

    @Test
    fun aCompletedChallengeStaysOnScreenWithItsFinishedAttempt() = runTest {
        val active = createChallenge()

        repository.completeAttempt(active.attempt.id)

        val finished = repository.observeActiveChallenge().first()!!
        assertThat(finished.attempt.id).isEqualTo(active.attempt.id)
        assertThat(finished.attempt.status).isEqualTo(AttemptStatus.COMPLETED)
    }

    @Test
    fun completingAnUnknownAttemptFails() = runTest {
        createChallenge()

        assertThat(repository.completeAttempt(attemptId = 404))
            .isEqualTo(Result.Error(DataError.Local.NOT_FOUND))
    }

    private suspend fun createChallenge() = repository
        .createChallenge(name = "90 days", startDate = startDate, habits = habits)
        .let { repository.observeActiveChallenge().first()!! }

    private companion object {
        val MAX_EMISSION_DELAY = 500.milliseconds
    }
}
