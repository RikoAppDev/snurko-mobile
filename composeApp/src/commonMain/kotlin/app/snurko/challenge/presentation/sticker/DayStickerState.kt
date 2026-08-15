package app.snurko.challenge.presentation.sticker

import androidx.compose.runtime.Stable
import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.presentation.home.GridCellUi
import app.snurko.challenge.presentation.home.toChallengeHomeState
import kotlinx.datetime.LocalDate

data class StickerHabitUi(
    val name: String,
    val isChecked: Boolean,
)

@Stable
data class DayStickerState(
    val dayIndex: Int,
    val totalDays: Int,
    val streak: Int,
    val cells: List<GridCellUi>,
    val habits: List<StickerHabitUi> = emptyList(),
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
        habits = home.habits.map { StickerHabitUi(name = it.name, isChecked = it.isChecked) },
        isCompleted = home.isCompleted,
    )
}
