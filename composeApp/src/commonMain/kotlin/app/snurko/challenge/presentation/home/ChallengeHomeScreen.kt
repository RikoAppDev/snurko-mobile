package app.snurko.challenge.presentation.home

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.designsystem.SnurkoTheme
import app.snurko.core.presentation.ObserveAsEvents
import app.snurko.resources.Res
import app.snurko.resources.challenge_fail_condition_summary
import app.snurko.resources.home_day
import app.snurko.resources.home_day_mark
import app.snurko.resources.home_grid
import app.snurko.resources.home_streak
import app.snurko.resources.home_today
import app.snurko.resources.home_update_failed
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val GRID_COLUMNS = 10
private val CELL_GAP = 4.dp
private val CELL_CORNER = RoundedCornerShape(3.dp)
private val PAGE_PADDING = 20.dp

@Composable
fun ChallengeHomeRoot(viewModel: ChallengeHomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val updateFailedMessage = stringResource(Res.string.home_update_failed)

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ChallengeHomeEvent.UpdateFailed -> scope.launch {
                snackbarHostState.showSnackbar(updateFailedMessage)
            }
        }
    }

    Scaffold(
        containerColor = SnurkoColors.paper,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        ChallengeHomeScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun ChallengeHomeScreen(
    state: ChallengeHomeState,
    onAction: (ChallengeHomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnurkoColors.paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PAGE_PADDING)
            .padding(top = 24.dp, bottom = 32.dp),
    ) {
        if (state.isLoading) return@Column

        HomeHeader(state = state)
        Spacer(Modifier.height(20.dp))
        DayGrid(cells = state.cells, markedDays = state.markedDays, totalDays = state.totalDays)
        Spacer(Modifier.height(28.dp))
        TodayChecklist(
            habits = state.habits,
            onToggle = { onAction(ChallengeHomeAction.HabitToggled(it)) },
        )
    }
}

@Composable
private fun HomeHeader(state: ChallengeHomeState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = state.challengeName,
            style = MaterialTheme.typography.displayMedium,
            color = SnurkoColors.text,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.home_day, state.dayIndex),
                style = MaterialTheme.typography.titleMedium,
                color = SnurkoColors.textSecondary,
            )
            Text(
                text = stringResource(Res.string.home_streak, state.streak),
                style = MaterialTheme.typography.bodyMedium,
                color = SnurkoColors.textSecondary,
            )
        }
    }
}

@Composable
private fun DayGrid(cells: List<GridCellUi>, markedDays: Int, totalDays: Int) {
    val gridDescription = stringResource(Res.string.home_grid, markedDays, totalDays)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = gridDescription },
        verticalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        cells.chunked(GRID_COLUMNS).forEachIndexed { rowIndex, rowCells ->
            Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                rowCells.forEachIndexed { columnIndex, cell ->
                    DayCell(
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
private fun DayCell(day: Int, cell: GridCellUi, modifier: Modifier = Modifier) {
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
                fontSize = 22.sp,
                color = SnurkoColors.text,
            )
        }
    }
}

@Composable
private fun TodayChecklist(habits: List<HabitRowUi>, onToggle: (Long) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.home_today),
            style = MaterialTheme.typography.labelLarge,
            color = SnurkoColors.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        habits.forEach { habit ->
            HorizontalDivider(color = SnurkoColors.line)
            HabitCheckRow(habit = habit, onToggle = onToggle)
        }
        HorizontalDivider(color = SnurkoColors.line)
    }
}

@Composable
private fun HabitCheckRow(habit: HabitRowUi, onToggle: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = habit.isChecked,
                role = Role.Checkbox,
                onValueChange = { onToggle(habit.id) },
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckMarkBox(isChecked = habit.isChecked, markSeed = habit.id.toInt())
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyLarge,
                color = SnurkoColors.text,
            )
            habit.failCondition?.let { failCondition ->
                Text(
                    text = stringResource(
                        Res.string.challenge_fail_condition_summary,
                        failCondition,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SnurkoColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun CheckMarkBox(isChecked: Boolean, markSeed: Int) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CELL_CORNER)
            .background(if (isChecked) SnurkoColors.surface else Color.Transparent)
            .border(
                width = if (isChecked) 2.dp else 1.dp,
                color = if (isChecked) SnurkoColors.accent else SnurkoColors.line,
                shape = CELL_CORNER,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isChecked) {
            PencilMark(
                rotation = markRotationDegrees(markSeed),
                fontSize = 24.sp,
                color = SnurkoColors.text,
            )
        }
    }
}

@Composable
private fun PencilMark(
    rotation: Float,
    fontSize: TextUnit,
    color: Color,
) {
    Text(
        text = stringResource(Res.string.home_day_mark),
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = fontSize,
            lineHeight = fontSize,
        ),
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .graphicsLayer { rotationZ = rotation }
            .clearAndSetSemantics { },
    )
}

@Preview
@Composable
private fun ChallengeHomeScreenPreview() {
    SnurkoTheme {
        ChallengeHomeScreen(state = previewState, onAction = {})
    }
}

private val previewState = ChallengeHomeState(
    challengeName = "90 days",
    dayIndex = 12,
    streak = 11,
    cells = (1..90).map { day ->
        when {
            day < 12 -> GridCellUi.COMPLETE
            day == 12 -> GridCellUi.TODAY
            else -> GridCellUi.FUTURE
        }
    },
    habits = listOf(
        HabitRowUi(1, "Workout", "less than 30 minutes", isChecked = true),
        HabitRowUi(2, "Read", "fewer than 10 pages", isChecked = false),
        HabitRowUi(3, "No alcohol", null, isChecked = false),
    ),
    isLoading = false,
)
