package app.snurko.challenge.presentation.history

import app.cash.turbine.test
import app.snurko.billing.domain.FakeEntitlementRepository
import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.NotebookStats
import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.challenge.domain.FakeNotebookBackup
import app.snurko.challenge.domain.attemptRecord
import app.snurko.challenge.domain.completeDaysThrough
import app.snurko.challenge.domain.day
import app.snurko.challenge.domain.notebook
import app.snurko.challenge.domain.partialDay
import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class HistoryViewModelTest {

    private lateinit var repository: FakeChallengeRepository
    private lateinit var backup: FakeNotebookBackup
    private lateinit var entitlements: FakeEntitlementRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeChallengeRepository()
        backup = FakeNotebookBackup()
        entitlements = FakeEntitlementRepository(isUnlocked = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theBackPagesAreLoadingUntilTheNotebookArrives() {
        val state = viewModel(today = day(1)).state.value

        assertThat(state.isLoading).isTrue()
        assertThat(state.rows).isEmpty()
    }

    @Test
    fun theNotebookIsReadAsAttemptRowsNewestFirst() {
        repository.setNotebook(
            notebook(
                attempts = listOf(
                    attemptRecord(
                        number = 1,
                        status = AttemptStatus.FAILED,
                        failedOnDay = 3,
                        checks = completeDaysThrough(2) + partialDay(3),
                    ),
                    attemptRecord(number = 2, startDate = day(4)),
                ),
            ),
        )

        val state = viewModel(today = day(5)).state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.rows.map { it.number }).containsExactly(2, 1)
        assertThat(state.current?.outcome).isEqualTo(AttemptOutcomeUi.InProgress(day = 2))
        assertThat(state.past.map { it.number }).containsExactly(1)
    }

    @Test
    fun theFirstAttemptLeavesTheBackPagesEmpty() {
        repository.setNotebook(notebook())

        val state = viewModel(today = day(2)).state.value

        assertThat(state.current).isNotNull()
        assertThat(state.past).isEmpty()
    }

    @Test
    fun aLockedDeviceKeepsTheArchiveAndTheFiguresOffTheBackPages() {
        entitlements.setUnlocked(false)
        repository.setNotebook(pastAndPresent())

        val state = viewModel(today = day(5)).state.value

        assertThat(state.past).isEmpty()
        assertThat(state.stats).isNull()
        assertThat(state.isArchiveLocked).isTrue()
    }

    @Test
    fun aLockedDeviceStillShowsTheAttemptItIsOn() {
        entitlements.setUnlocked(false)
        repository.setNotebook(pastAndPresent())

        val state = viewModel(today = day(5)).state.value

        assertThat(state.current?.outcome).isEqualTo(AttemptOutcomeUi.InProgress(day = 2))
    }

    @Test
    fun anUnlockedDeviceShowsTheArchiveAndTheFigures() {
        repository.setNotebook(pastAndPresent())

        val state = viewModel(today = day(5)).state.value

        assertThat(state.past.map { it.number }).containsExactly(1)
        assertThat(state.stats).isEqualTo(
            NotebookStats(bestCompletionPercent = 2, longestStreak = 2, totalMarks = 3),
        )
        assertThat(state.isArchiveLocked).isFalse()
    }

    @Test
    fun anUnlockBoughtWhileTheBackPagesAreOpenOpensTheArchive() {
        entitlements.setUnlocked(false)
        repository.setNotebook(pastAndPresent())
        val viewModel = viewModel(today = day(5))
        assertThat(viewModel.state.value.past).isEmpty()

        entitlements.setUnlocked(true)

        assertThat(viewModel.state.value.past.map { it.number }).containsExactly(1)
        assertThat(viewModel.state.value.stats).isNotNull()
    }

    @Test
    fun aLockedDeviceCanStillExportItsNotebook() = runTest {
        entitlements.setUnlocked(false)
        repository.setNotebook(notebook())
        val viewModel = viewModel(today = day(2))

        viewModel.events.test {
            viewModel.onAction(HistoryAction.ExportClicked)

            assertThat(awaitItem()).isEqualTo(HistoryEvent.NotebookExported)
        }
        assertThat(backup.exportCalls).isEqualTo(1)
    }

    @Test
    fun aLockedDeviceCanStillImportANotebook() = runTest {
        entitlements.setUnlocked(false)
        repository.setNotebook(notebook())
        val viewModel = viewModel(today = day(2))
        viewModel.onAction(HistoryAction.ImportClicked)

        viewModel.events.test {
            viewModel.onAction(HistoryAction.ImportConfirmed)

            assertThat(awaitItem()).isEqualTo(HistoryEvent.NotebookImported)
        }
        assertThat(backup.importCalls).isEqualTo(1)
    }

    @Test
    fun theArchiveIsNotCalledLockedWhileTheNotebookIsStillLoading() {
        entitlements.setUnlocked(false)

        assertThat(viewModel(today = day(2)).state.value.isArchiveLocked).isFalse()
    }

    @Test
    fun everyArchivedAttemptCarriesItsOwnCompletion() {
        repository.setNotebook(
            notebook(
                attempts = listOf(
                    attemptRecord(
                        number = 1,
                        status = AttemptStatus.FAILED,
                        failedOnDay = 46,
                        checks = completeDaysThrough(45),
                    ),
                    attemptRecord(number = 2, startDate = day(47)),
                ),
            ),
        )

        val state = viewModel(today = day(48)).state.value

        assertThat(state.past.single().completionPercent).isEqualTo(50)
    }

    @Test
    fun exportingTheNotebookReportsThatItWasWrittenOut() = runTest {
        repository.setNotebook(notebook())
        val viewModel = viewModel(today = day(2))

        viewModel.events.test {
            viewModel.onAction(HistoryAction.ExportClicked)

            assertThat(awaitItem()).isEqualTo(HistoryEvent.NotebookExported)
        }
        assertThat(backup.exportCalls).isEqualTo(1)
        assertThat(viewModel.state.value.isBusy).isFalse()
    }

    @Test
    fun anExportThatFailsSaysWhatWentWrong() = runTest {
        repository.setNotebook(notebook())
        backup.exportResult = Result.Error(DataError.Backup.NOTHING_TO_EXPORT)
        val viewModel = viewModel(today = day(2))

        viewModel.events.test {
            viewModel.onAction(HistoryAction.ExportClicked)

            assertThat(awaitItem()).isEqualTo(
                HistoryEvent.BackupFailed(DataError.Backup.NOTHING_TO_EXPORT),
            )
        }
    }

    @Test
    fun anExportTheUserBacksOutOfSaysNothing() = runTest {
        repository.setNotebook(notebook())
        backup.exportResult = Result.Error(DataError.Backup.CANCELLED)
        val viewModel = viewModel(today = day(2))

        viewModel.events.test {
            viewModel.onAction(HistoryAction.ExportClicked)

            expectNoEvents()
        }
        assertThat(viewModel.state.value.isBusy).isFalse()
    }

    @Test
    fun importingAsksForConfirmationBeforeAnyFileIsRead() {
        repository.setNotebook(notebook())
        val viewModel = viewModel(today = day(2))

        viewModel.onAction(HistoryAction.ImportClicked)

        assertThat(viewModel.state.value.isImportConfirmationVisible).isTrue()
        assertThat(backup.importCalls).isEqualTo(0)
    }

    @Test
    fun aConfirmationTheUserDeclinesLeavesTheNotebookAlone() {
        repository.setNotebook(notebook())
        val viewModel = viewModel(today = day(2))

        viewModel.onAction(HistoryAction.ImportClicked)
        viewModel.onAction(HistoryAction.ImportCancelled)

        assertThat(viewModel.state.value.isImportConfirmationVisible).isFalse()
        assertThat(backup.importCalls).isEqualTo(0)
    }

    @Test
    fun aConfirmedImportReplacesTheNotebookAndSaysSo() = runTest {
        repository.setNotebook(notebook())
        val viewModel = viewModel(today = day(2))
        viewModel.onAction(HistoryAction.ImportClicked)

        viewModel.events.test {
            viewModel.onAction(HistoryAction.ImportConfirmed)

            assertThat(awaitItem()).isEqualTo(HistoryEvent.NotebookImported)
        }
        assertThat(backup.importCalls).isEqualTo(1)
        assertThat(viewModel.state.value.isImportConfirmationVisible).isFalse()
    }

    @Test
    fun theImportedNotebookIsWhatTheBackPagesShow() {
        repository.setNotebook(notebook())
        backup.onImport = {
            repository.setNotebook(
                notebook(
                    attempts = listOf(
                        attemptRecord(number = 1, status = AttemptStatus.FAILED, failedOnDay = 12),
                        attemptRecord(number = 2, startDate = day(13)),
                    ),
                ),
            )
        }
        val viewModel = viewModel(today = day(14))
        assertThat(viewModel.state.value.past).isEmpty()

        viewModel.onAction(HistoryAction.ImportClicked)
        viewModel.onAction(HistoryAction.ImportConfirmed)

        assertThat(viewModel.state.value.rows.map { it.number }).containsExactly(2, 1)
        assertThat(viewModel.state.value.past.single().outcome).isEqualTo(
            AttemptOutcomeUi.Failed(failedOnDay = 12, missedHabits = listOf("Workout", "Read")),
        )
    }

    @Test
    fun anImportThatCannotBeReadSaysWhatWasWrongWithTheFile() = runTest {
        repository.setNotebook(notebook())
        backup.importResult = Result.Error(DataError.Backup.MALFORMED_NOTEBOOK)
        val viewModel = viewModel(today = day(2))
        viewModel.onAction(HistoryAction.ImportClicked)

        viewModel.events.test {
            viewModel.onAction(HistoryAction.ImportConfirmed)

            assertThat(awaitItem()).isEqualTo(
                HistoryEvent.BackupFailed(DataError.Backup.MALFORMED_NOTEBOOK),
            )
        }
        assertThat(viewModel.state.value.isBusy).isFalse()
    }

    @Test
    fun anImportTheUserBacksOutOfSaysNothing() = runTest {
        repository.setNotebook(notebook())
        backup.importResult = Result.Error(DataError.Backup.CANCELLED)
        val viewModel = viewModel(today = day(2))
        viewModel.onAction(HistoryAction.ImportClicked)

        viewModel.events.test {
            viewModel.onAction(HistoryAction.ImportConfirmed)

            expectNoEvents()
        }
        assertThat(viewModel.state.value.isImportConfirmationVisible).isFalse()
    }

    @Test
    fun anEmptyNotebookLeavesTheBackPagesLoading() {
        repository.setNotebook(null)

        val state = viewModel(today = day(2)).state.value

        assertThat(state.isLoading).isTrue()
        assertThat(state.current).isNull()
    }

    private fun pastAndPresent() = notebook(
        attempts = listOf(
            attemptRecord(
                number = 1,
                status = AttemptStatus.FAILED,
                failedOnDay = 3,
                checks = completeDaysThrough(2) + partialDay(3),
            ),
            attemptRecord(number = 2, startDate = day(4), checks = completeDaysThrough(1)),
        ),
    )

    private fun viewModel(today: LocalDate) = HistoryViewModel(
        challengeRepository = repository,
        entitlementRepository = entitlements,
        notebookBackup = backup,
        dateProvider = { today.atTime(9, 0) },
    )
}
