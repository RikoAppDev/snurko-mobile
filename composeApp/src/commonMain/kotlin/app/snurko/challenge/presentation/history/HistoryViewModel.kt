package app.snurko.challenge.presentation.history

import androidx.lifecycle.viewModelScope
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.NotebookBackup
import app.snurko.challenge.domain.stats
import app.snurko.core.domain.DataError
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.onFailure
import app.snurko.core.domain.onSuccess
import app.snurko.core.presentation.MviViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HistoryViewModel(
    challengeRepository: ChallengeRepository,
    private val notebookBackup: NotebookBackup,
    private val dateProvider: DateProvider,
) : MviViewModel<HistoryState, HistoryAction, HistoryEvent>(initialState = HistoryState()) {

    init {
        challengeRepository.observeNotebook()
            .filterNotNull()
            .onEach { notebook ->
                val rows = notebook.toAttemptRows(dateProvider.today())
                updateState { state ->
                    state.copy(rows = rows, stats = notebook.stats(), isLoading = false)
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onAction(action: HistoryAction) {
        when (action) {
            HistoryAction.ExportClicked -> exportNotebook()
            HistoryAction.ImportClicked -> updateState { it.copy(isImportConfirmationVisible = true) }
            HistoryAction.ImportCancelled -> updateState { it.copy(isImportConfirmationVisible = false) }
            HistoryAction.ImportConfirmed -> importNotebook()
        }
    }

    private fun exportNotebook() {
        if (state.value.isBusy) return
        updateState { it.copy(isBusy = true) }
        viewModelScope.launch {
            notebookBackup.exportToDocument()
                .onSuccess { sendEvent(HistoryEvent.NotebookExported) }
                .onFailure(::reportBackupFailure)
            updateState { it.copy(isBusy = false) }
        }
    }

    private fun importNotebook() {
        if (state.value.isBusy) return
        updateState { it.copy(isImportConfirmationVisible = false, isBusy = true) }
        viewModelScope.launch {
            notebookBackup.importFromDocument()
                .onSuccess { sendEvent(HistoryEvent.NotebookImported) }
                .onFailure(::reportBackupFailure)
            updateState { it.copy(isBusy = false) }
        }
    }

    /** A picker the user backed out of is not a failure, so it stays silent. */
    private fun reportBackupFailure(error: DataError.Backup) {
        if (error != DataError.Backup.CANCELLED) {
            sendEvent(HistoryEvent.BackupFailed(error))
        }
    }
}
