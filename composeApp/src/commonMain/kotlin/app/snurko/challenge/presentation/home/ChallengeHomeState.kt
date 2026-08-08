package app.snurko.challenge.presentation.home

import androidx.compose.runtime.Stable
import app.snurko.challenge.domain.CHALLENGE_LENGTH_DAYS
import app.snurko.core.presentation.UiState

enum class GridCellUi {
    COMPLETE,
    FAILED,
    TODAY,
    TODAY_COMPLETE,
    FUTURE,
}

data class HabitRowUi(
    val id: Long,
    val name: String,
    val failCondition: String?,
    val isChecked: Boolean,
)

@Stable
data class ChallengeHomeState(
    val challengeName: String = "",
    val dayIndex: Int = 1,
    val totalDays: Int = CHALLENGE_LENGTH_DAYS,
    val streak: Int = 0,
    val cells: List<GridCellUi> = emptyList(),
    val habits: List<HabitRowUi> = emptyList(),
    val isLoading: Boolean = true,
    val isCompleted: Boolean = false,
) : UiState {

    val markedDays: Int
        get() = cells.count { it == GridCellUi.COMPLETE || it == GridCellUi.TODAY_COMPLETE }
}
