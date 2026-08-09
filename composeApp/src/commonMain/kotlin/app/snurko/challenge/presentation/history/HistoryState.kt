package app.snurko.challenge.presentation.history

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.snurko.challenge.domain.NotebookStats
import app.snurko.core.presentation.UiState
import kotlinx.datetime.LocalDate

sealed interface AttemptOutcomeUi {
    data class InProgress(val day: Int) : AttemptOutcomeUi
    data class Failed(val failedOnDay: Int, val missedHabits: List<String>) : AttemptOutcomeUi
    data object Completed : AttemptOutcomeUi
}

@Immutable
data class AttemptRowUi(
    val number: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val outcome: AttemptOutcomeUi,
    val markedDays: List<Boolean>,
    val completionPercent: Int,
)

@Stable
data class HistoryState(
    val rows: List<AttemptRowUi> = emptyList(),
    val stats: NotebookStats? = null,
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val isImportConfirmationVisible: Boolean = false,
) : UiState {

    val current: AttemptRowUi?
        get() = rows.firstOrNull { it.outcome is AttemptOutcomeUi.InProgress }

    val past: List<AttemptRowUi>
        get() = rows.filterNot { it.outcome is AttemptOutcomeUi.InProgress }
}
