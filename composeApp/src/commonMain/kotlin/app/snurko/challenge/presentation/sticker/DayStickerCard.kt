package app.snurko.challenge.presentation.sticker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.snurko.challenge.presentation.home.GridCellUi
import app.snurko.challenge.presentation.home.PencilMark
import app.snurko.challenge.presentation.home.markRotationDegrees
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.designsystem.SnurkoTheme
import app.snurko.resources.Res
import app.snurko.resources.home_completed
import app.snurko.resources.home_day
import app.snurko.resources.home_streak
import app.snurko.resources.sticker_wordmark
import org.jetbrains.compose.resources.stringResource

private const val GRID_COLUMNS = 10
private const val GRID_ROWS = 9

private val CARD_PADDING = 26.dp
private val HEADER_HEIGHT = 64.dp
private val HEADER_GAP = 10.dp
private val LIST_GAP = 14.dp
private val WORDMARK_RESERVE = 20.dp
private val CELL_GAP = 2.5.dp
private val CELL_CORNER = RoundedCornerShape(3.dp)
private val MAX_CELL = 28.dp
private val WORDMARK_PADDING = 14.dp

private val FULL_ROW_HEIGHT = 20.dp
private val LIST_MAX_HEIGHT = 140.dp
private val FULL_NAME_SIZE = 14.sp
private const val FULL_SIZE_HABITS = 7

@Composable
fun DayStickerCard(state: DayStickerState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(SnurkoColors.paper)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = CARD_PADDING)
                .padding(top = CARD_PADDING, bottom = CARD_PADDING + WORDMARK_RESERVE),
        ) {
            StickerHeader(state = state)
            Spacer(Modifier.height(HEADER_GAP))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopStart,
            ) {
                StickerGrid(cells = state.cells, available = maxHeight)
            }
            if (state.habits.isNotEmpty()) {
                Spacer(Modifier.height(LIST_GAP))
                StickerHabits(habits = state.habits)
            }
        }
        Text(
            text = stringResource(Res.string.sticker_wordmark),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = SnurkoColors.textMuted,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = CARD_PADDING, bottom = WORDMARK_PADDING),
        )
    }
}

@Composable
private fun StickerHeader(state: DayStickerState) {
    Column(modifier = Modifier.fillMaxWidth().height(HEADER_HEIGHT)) {
        Text(
            text = if (state.isCompleted) {
                stringResource(Res.string.home_completed, state.totalDays)
            } else {
                stringResource(Res.string.home_day, state.dayIndex)
            },
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp, lineHeight = 48.sp),
            color = SnurkoColors.text,
        )
        if (!state.isCompleted) {
            Text(
                text = stringResource(Res.string.home_streak, state.streak),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = SnurkoColors.textSecondary,
            )
        }
    }
}

@Composable
private fun StickerGrid(cells: List<GridCellUi>, available: Dp) {
    val byHeight = (available - CELL_GAP * (GRID_ROWS - 1)) / GRID_ROWS
    val cell = minOf(byHeight, MAX_CELL)

    Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
        cells.chunked(GRID_COLUMNS).forEachIndexed { rowIndex, rowCells ->
            Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                rowCells.forEachIndexed { columnIndex, gridCell ->
                    StickerCell(
                        day = rowIndex * GRID_COLUMNS + columnIndex + 1,
                        cell = gridCell,
                        size = cell,
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerCell(day: Int, cell: GridCellUi, size: Dp) {
    val isToday = cell == GridCellUi.TODAY || cell == GridCellUi.TODAY_COMPLETE
    val isMarked = cell == GridCellUi.COMPLETE || cell == GridCellUi.TODAY_COMPLETE

    Box(
        modifier = Modifier
            .size(size)
            .clip(CELL_CORNER)
            .background(if (cell == GridCellUi.FAILED) SnurkoColors.paper else SnurkoColors.surface)
            .border(
                width = if (isToday) 2.dp else 1.dp,
                color = when {
                    isToday -> SnurkoColors.accent
                    cell == GridCellUi.FAILED -> SnurkoColors.line
                    else -> SnurkoColors.lineSoft
                },
                shape = CELL_CORNER,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isMarked) {
            PencilMark(
                rotation = markRotationDegrees(day),
                size = size * 0.8f,
                color = SnurkoColors.text,
            )
        }
    }
}

/**
 * Up to [FULL_SIZE_HABITS] lines keep their full size; beyond that the whole list is
 * scaled to the same band so the card never grows past its fixed frame.
 */
@Composable
private fun StickerHabits(habits: List<StickerHabitUi>) {
    val rowHeight = minOf(FULL_ROW_HEIGHT, LIST_MAX_HEIGHT / habits.size)
    val scale = (rowHeight / FULL_ROW_HEIGHT).coerceAtMost(1f)
    val nameSize = FULL_NAME_SIZE * scale
    val boxSize = rowHeight * 0.74f

    Column(modifier = Modifier.fillMaxWidth()) {
        habits.forEachIndexed { index, habit ->
            Row(
                modifier = Modifier.fillMaxWidth().height(rowHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StickerHabitBox(isChecked = habit.isChecked, size = boxSize, seed = index + 1)
                Spacer(Modifier.width(boxSize * 0.55f))
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = nameSize,
                        lineHeight = nameSize * 1.1f,
                    ),
                    color = SnurkoColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StickerHabitBox(isChecked: Boolean, size: Dp, seed: Int) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CELL_CORNER)
            .background(if (isChecked) SnurkoColors.surface else SnurkoColors.paper)
            .border(
                width = if (isChecked) 1.5.dp else 1.dp,
                color = if (isChecked) SnurkoColors.accent else SnurkoColors.line,
                shape = CELL_CORNER,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isChecked) {
            PencilMark(
                rotation = markRotationDegrees(seed),
                size = size * 0.86f,
                color = SnurkoColors.text,
            )
        }
    }
}

@Preview
@Composable
private fun DayStickerCardPreview() {
    SnurkoTheme {
        Box(Modifier.size(width = 360.dp, height = 450.dp)) {
            DayStickerCard(
                state = DayStickerState(
                    dayIndex = 34,
                    totalDays = 90,
                    streak = 33,
                    cells = (1..90).map { day ->
                        when {
                            day == 17 -> GridCellUi.FAILED
                            day < 34 -> GridCellUi.COMPLETE
                            day == 34 -> GridCellUi.TODAY
                            else -> GridCellUi.FUTURE
                        }
                    },
                    habits = listOf(
                        StickerHabitUi("Read at least 10 pages", isChecked = true),
                        StickerHabitUi("Morning without social networks", isChecked = true),
                        StickerHabitUi("Fast workout in the morning", isChecked = false),
                        StickerHabitUi("Cold shower if showering", isChecked = true),
                        StickerHabitUi("Evening stretching", isChecked = false),
                    ),
                    isCompleted = false,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun DayStickerCardCompletedPreview() {
    SnurkoTheme {
        Box(Modifier.size(width = 360.dp, height = 450.dp)) {
            DayStickerCard(
                state = DayStickerState(
                    dayIndex = 90,
                    totalDays = 90,
                    streak = 90,
                    cells = List(90) { GridCellUi.COMPLETE },
                    isCompleted = true,
                ),
            )
        }
    }
}
