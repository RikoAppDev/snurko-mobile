package app.snurko.challenge.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.DayCheck
import app.snurko.challenge.domain.Habit
import app.snurko.challenge.domain.NOTEBOOK_SCHEMA_VERSION
import app.snurko.challenge.domain.NotebookExport
import app.snurko.challenge.domain.NotebookRepository
import app.snurko.core.database.SnurkoDatabase
import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RoomNotebookRepositoryTest {

    private lateinit var database: SnurkoDatabase
    private lateinit var challengeRepository: ChallengeRepository
    private lateinit var notebookRepository: NotebookRepository

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
        challengeRepository = RoomChallengeRepository(
            challengeDao = database.challengeDao(),
            attemptDao = database.attemptDao(),
            dayCheckDao = database.dayCheckDao(),
        )
        notebookRepository = RoomNotebookRepository(challengeDao = database.challengeDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun anEmptyNotebookHasNothingToExport() = runTest {
        assertThat(notebookRepository.exportNotebook())
            .isEqualTo(Result.Error(DataError.Backup.NOTHING_TO_EXPORT))
    }

    @Test
    fun theExportCarriesTheChallengeItsHabitsAndEveryAttemptWithItsChecks() = runTest {
        seedLivedInNotebook()

        val export = successfulExport()

        assertThat(export.schemaVersion).isEqualTo(NOTEBOOK_SCHEMA_VERSION)
        assertThat(export.challenge.name).isEqualTo("90 days")
        assertThat(export.challenge.startDate).isEqualTo(startDate)
        assertThat(export.challenge.habits.map { it.name })
            .containsExactly("Workout", "Read", "No alcohol")
        assertThat(export.attempts).hasSize(2)
        assertThat(export.attempts.map { it.attempt.number }).containsExactly(1, 2)
        assertThat(export.attempts[0].attempt.status).isEqualTo(AttemptStatus.FAILED)
        assertThat(export.attempts[0].attempt.failedOnDay).isEqualTo(2)
        assertThat(export.attempts[0].checks).containsExactly(
            DayCheck(day = 1, checkedHabitIds = setOf(export.challenge.habits[0].id)),
        )
        assertThat(export.attempts[1].attempt.status).isEqualTo(AttemptStatus.ACTIVE)
        assertThat(export.attempts[1].checks).containsExactly(
            DayCheck(day = 1, checkedHabitIds = setOf(export.challenge.habits[1].id)),
        )
    }

    @Test
    fun aWipedNotebookIsRestoredExactlyByImportingItsExport() = runTest {
        seedLivedInNotebook()
        val export = successfulExport()

        database.challengeDao().deleteAllChallenges()
        assertThat(challengeRepository.observeActiveChallenge().first()).isNull()

        assertThat(notebookRepository.importNotebook(export)).isEqualTo(Result.Success(Unit))

        assertThat(successfulExport()).isEqualTo(export)
    }

    @Test
    fun importingReplacesWhateverWasThereBefore() = runTest {
        seedLivedInNotebook()
        val export = successfulExport()
        database.challengeDao().deleteAllChallenges()
        challengeRepository.createChallenge(
            name = "another challenge",
            startDate = LocalDate(2026, 5, 5),
            habits = listOf(Habit(name = "Meditate")),
        )

        notebookRepository.importNotebook(export)

        val restored = challengeRepository.observeActiveChallenge().first()
        assertThat(restored).isNotNull()
        assertThat(restored!!.challenge.name).isEqualTo("90 days")
        assertThat(restored.attempt.number).isEqualTo(2)
        assertThat(successfulExport().attempts).hasSize(2)
    }

    @Test
    fun aNotebookWithChecksForUnknownHabitsIsRejectedWithoutTouchingTheStoredOne() = runTest {
        seedLivedInNotebook()
        val stored = successfulExport()
        val foreign = stored.copy(
            attempts = stored.attempts.map { record ->
                record.copy(checks = listOf(DayCheck(day = 1, checkedHabitIds = setOf(9_999L))))
            },
        )

        assertThat(notebookRepository.importNotebook(foreign))
            .isEqualTo(Result.Error(DataError.Backup.MALFORMED_NOTEBOOK))
        assertThat(successfulExport()).isEqualTo(stored)
    }

    @Test
    fun aNotebookWithoutAttemptsIsRejectedWithoutTouchingTheStoredOne() = runTest {
        seedLivedInNotebook()
        val stored = successfulExport()

        assertThat(notebookRepository.importNotebook(stored.copy(attempts = emptyList())))
            .isEqualTo(Result.Error(DataError.Backup.MALFORMED_NOTEBOOK))
        assertThat(successfulExport()).isEqualTo(stored)
    }

    @Test
    fun aNotebookFromAnotherSchemaVersionIsRejectedWithoutTouchingTheStoredOne() = runTest {
        seedLivedInNotebook()
        val stored = successfulExport()

        assertThat(notebookRepository.importNotebook(stored.copy(schemaVersion = NOTEBOOK_SCHEMA_VERSION + 1)))
            .isEqualTo(Result.Error(DataError.Backup.UNSUPPORTED_SCHEMA_VERSION))
        assertThat(successfulExport()).isEqualTo(stored)
    }

    /** A challenge that has been failed once, with a check on either side of the restart. */
    private suspend fun seedLivedInNotebook() {
        challengeRepository.createChallenge(name = "90 days", startDate = startDate, habits = habits)
        val first = challengeRepository.observeActiveChallenge().first()!!
        challengeRepository.setHabitChecked(
            attemptId = first.attempt.id,
            day = 1,
            habitId = first.challenge.habits[0].id,
            checked = true,
        )
        challengeRepository.failAttemptAndStartNext(
            attemptId = first.attempt.id,
            failedOnDay = 2,
            nextAttemptStartDate = LocalDate(2026, 1, 3),
        )
        val second = challengeRepository.observeActiveChallenge().first()!!
        challengeRepository.setHabitChecked(
            attemptId = second.attempt.id,
            day = 1,
            habitId = second.challenge.habits[1].id,
            checked = true,
        )
    }

    private suspend fun successfulExport(): NotebookExport =
        (notebookRepository.exportNotebook() as Result.Success).data
}
