package app.snurko.challenge.presentation.history

import app.snurko.core.presentation.UiAction

sealed interface HistoryAction : UiAction {
    data object ExportClicked : HistoryAction
    data object ImportClicked : HistoryAction
    data object ImportConfirmed : HistoryAction
    data object ImportCancelled : HistoryAction
}
