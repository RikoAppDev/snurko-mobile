package app.snurko.challenge.presentation.sticker

import androidx.compose.runtime.Stable
import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.presentation.home.GridCellUi
import app.snurko.challenge.presentation.home.toChallengeHomeState
import kotlinx.datetime.LocalDate

@Stable
data class DayStickerState(
    val dayIndex: Int,
    val totalDays: Int,
    val streak: Int,
    val cells: List<GridCellUi>,
    val isCompleted: Boolean,
)

fun DayStickerState.stickerFileName(): String = "snurko-day-$dayIndex.png"

fun ActiveChallenge.toDayStickerState(today: LocalDate): DayStickerState {
    val home = toChallengeHomeState(today)
    return DayStickerState(
        dayIndex = home.dayIndex,
        totalDays = home.totalDays,
        streak = home.streak,
        cells = home.cells,
        isCompleted = home.isCompleted,
    )
}
