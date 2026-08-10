package app.snurko.challenge.presentation.sticker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
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
private val CARD_PADDING = 30.dp
private val HEADER_HEIGHT = 92.dp
private val HEADER_GAP = 22.dp
private val CELL_GAP = 3.dp
private val CELL_CORNER = RoundedCornerShape(3.dp)
private val WORDMARK_PADDING = 14.dp
private val MARK_SIZE = 21.dp

@Composable
fun DayStickerCard(state: DayStickerState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(SnurkoColors.paper)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = CARD_PADDING)
                .padding(top = CARD_PADDING),
        ) {
            StickerHeader(state = state)
            Spacer(Modifier.height(HEADER_GAP))
            StickerGrid(cells = state.cells)
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
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp, lineHeight = 66.sp),
            color = SnurkoColors.text,
        )
        if (!state.isCompleted) {
            Text(
                text = stringResource(Res.string.home_streak, state.streak),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                color = SnurkoColors.textSecondary,
            )
        }
    }
}

@Composable
private fun StickerGrid(cells: List<GridCellUi>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        cells.chunked(GRID_COLUMNS).forEachIndexed { rowIndex, rowCells ->
            Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                rowCells.forEachIndexed { columnIndex, cell ->
                    StickerCell(
                        day = rowIndex * GRID_COLUMNS + columnIndex + 1,
                        cell = cell,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerCell(day: Int, cell: GridCellUi, modifier: Modifier = Modifier) {
    val isToday = cell == GridCellUi.TODAY || cell == GridCellUi.TODAY_COMPLETE
    val isMarked = cell == GridCellUi.COMPLETE || cell == GridCellUi.TODAY_COMPLETE

    Box(
        modifier = modifier
            .aspectRatio(1f)
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
                size = MARK_SIZE,
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
