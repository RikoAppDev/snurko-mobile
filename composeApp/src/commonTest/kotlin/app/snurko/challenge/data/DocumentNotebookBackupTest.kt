package app.snurko.challenge.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.DayCheck
import app.snurko.challenge.domain.Habit
import app.snurko.challenge.domain.NotebookBackup
import app.snurko.challenge.domain.NotebookRepository
import app.snurko.core.database.SnurkoDatabase
import app.snurko.core.domain.DataError
import app.snurko.core.domain.DateProvider
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

class DocumentNotebookBackupTest {

    private lateinit var database: SnurkoDatabase
    private lateinit var challengeRepository: ChallengeRepository
    private lateinit var notebookRepository: NotebookRepository
    private lateinit var documents: FakeNotebookDocumentGateway
    private lateinit var backup: NotebookBackup

    private val startDate = LocalDate(2026, 1, 1)
    private val today = LocalDate(2026, 3, 17)
    private val habits = listOf(
        Habit(name = "Workout", failCondition = "less than 30 minutes"),
        Habit(name = "Read"),
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
        documents = FakeNotebookDocumentGateway()
        backup = DocumentNotebookBackup(
            notebookRepository = notebookRepository,
            documentGateway = documents,
            codec = NotebookJsonCodec(),
            dateProvider = DateProvider { today },
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun theExportedDocumentIsNamedAfterTheDayItWasTaken() = runTest {
        seedLivedInNotebook()

        assertThat(backup.exportToDocument()).isEqualTo(Result.Success(Unit))
        assertThat(documents.writtenName).isEqualTo("snurko-notebook-2026-03-17.json")
    }

    @Test
    fun aWipedNotebookComesBackWholeFromItsDocument() = runTest {
        seedLivedInNotebook()
        backup.exportToDocument()

        database.challengeDao().deleteAllChallenges()
        assertThat(challengeRepository.observeActiveChallenge().first()).isNull()

        assertThat(backup.importFromDocument()).isEqualTo(Result.Success(Unit))

        val restored = challengeRepository.observeActiveChallenge().first()
        assertThat(restored).isNotNull()
        assertThat(restored!!.challenge.name).isEqualTo("90 days")
        assertThat(restored.challenge.startDate).isEqualTo(startDate)
        assertThat(restored.challenge.habits.map { it.name }).containsExactly("Workout", "Read")
        assertThat(restored.challenge.habits.map { it.failCondition })
            .containsExactly("less than 30 minutes", null)
        assertThat(restored.attempt.number).isEqualTo(2)
        assertThat(restored.attempt.status).isEqualTo(AttemptStatus.ACTIVE)
        assertThat(restored.checks).containsExactly(
            DayCheck(day = 1, checkedHabitIds = setOf(restored.challenge.habits[1].id)),
        )

        val reExported = (notebookRepository.exportNotebook() as Result.Success).data
        assertThat(reExported.attempts).hasSize(2)
        assertThat(reExported.attempts[0].attempt.status).isEqualTo(AttemptStatus.FAILED)
        assertThat(reExported.attempts[0].attempt.failedOnDay).isEqualTo(2)
        assertThat(reExported.attempts[0].checks).containsExactly(
            DayCheck(day = 1, checkedHabitIds = setOf(restored.challenge.habits[0].id)),
        )
    }

    @Test
    fun anEmptyNotebookIsNeverWrittenToADocument() = runTest {
        assertThat(backup.exportToDocument())
            .isEqualTo(Result.Error(DataError.Backup.NOTHING_TO_EXPORT))
        assertThat(documents.document).isNull()
    }

    @Test
    fun dismissingThePickerLeavesTheNotebookAlone() = runTest {
        seedLivedInNotebook()
        documents.readResult = Result.Error(DataError.Backup.CANCELLED)

        assertThat(backup.importFromDocument())
            .isEqualTo(Result.Error(DataError.Backup.CANCELLED))
        assertThat(challengeRepository.observeActiveChallenge().first()).isNotNull()
    }

    @Test
    fun aDocumentFromSomeOtherAppLeavesTheNotebookAlone() = runTest {
        seedLivedInNotebook()
        val before = (notebookRepository.exportNotebook() as Result.Success).data
        documents.document = """{"notes":["nothing to do with snurko"]}"""

        assertThat(backup.importFromDocument())
            .isEqualTo(Result.Error(DataError.Backup.MALFORMED_NOTEBOOK))
        assertThat((notebookRepository.exportNotebook() as Result.Success).data).isEqualTo(before)
    }

    @Test
    fun aDocumentFromAnotherSchemaVersionLeavesTheNotebookAlone() = runTest {
        seedLivedInNotebook()
        val before = (notebookRepository.exportNotebook() as Result.Success).data
        backup.exportToDocument()
        documents.document = documents.document!!.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")

        assertThat(backup.importFromDocument())
            .isEqualTo(Result.Error(DataError.Backup.UNSUPPORTED_SCHEMA_VERSION))
        assertThat((notebookRepository.exportNotebook() as Result.Success).data).isEqualTo(before)
    }

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
}
