package app.snurko.challenge.presentation.history

import app.snurko.core.domain.DataError
import app.snurko.core.presentation.UiEvent

sealed interface HistoryEvent : UiEvent {
    data object NotebookExported : HistoryEvent
    data object NotebookImported : HistoryEvent
    data class BackupFailed(val error: DataError.Backup) : HistoryEvent
}
