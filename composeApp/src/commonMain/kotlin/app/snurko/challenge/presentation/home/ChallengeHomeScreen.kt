package app.snurko.challenge.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snurko.challenge.presentation.sticker.DayStickerRenderer
import app.snurko.challenge.presentation.sticker.DayStickerSharer
import app.snurko.challenge.presentation.sticker.DayStickerState
import app.snurko.challenge.presentation.sticker.stickerFileName
import app.snurko.core.designsystem.QuietTapShape
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.designsystem.SnurkoTheme
import app.snurko.core.designsystem.navigationBarInset
import app.snurko.core.designsystem.snurkoContentInsets
import app.snurko.core.domain.onFailure
import app.snurko.core.presentation.ObserveAsEvents
import app.snurko.core.presentation.OnResume
import app.snurko.resources.Res
import app.snurko.resources.challenge_fail_condition_summary
import app.snurko.resources.home_completed
import app.snurko.resources.home_day
import app.snurko.resources.home_grid
import app.snurko.resources.home_history_link
import app.snurko.resources.home_reminders_link
import app.snurko.resources.home_restart_acknowledge
import app.snurko.resources.home_restart_explanation
import app.snurko.resources.home_restart_missed
import app.snurko.resources.home_restart_separator
import app.snurko.resources.home_restart_title
import app.snurko.resources.home_share_failed
import app.snurko.resources.home_share_link
import app.snurko.resources.home_streak
import app.snurko.resources.home_today
import app.snurko.resources.home_update_failed
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val GRID_COLUMNS = 10
private val CELL_GAP = 4.dp
private val CELL_CORNER = RoundedCornerShape(3.dp)
private val PAGE_PADDING = 20.dp
private const val MARK_STROKE_RATIO = 0.09f
private val MARK_MIN_STROKE = 1.6.dp
private val GRID_MARK_SIZE = 22.dp
private val CHECKLIST_MARK_SIZE = 24.dp

@Composable
fun ChallengeHomeRoot(
    onOpenHistory: () -> Unit,
    onOpenReminders: () -> Unit,
    viewModel: ChallengeHomeViewModel = koinViewModel(),
    stickerSharer: DayStickerSharer = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val updateFailedMessage = stringResource(Res.string.home_update_failed)
    val shareFailedMessage = stringResource(Res.string.home_share_failed)
    var pendingSticker by remember { mutableStateOf<DayStickerState?>(null) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ChallengeHomeEvent.UpdateFailed -> scope.launch {
                snackbarHostState.showSnackbar(updateFailedMessage)
            }

            is ChallengeHomeEvent.ShareSticker -> pendingSticker = event.sticker
        }
    }

    OnResume { viewModel.onAction(ChallengeHomeAction.ScreenResumed) }

    Scaffold(
        containerColor = SnurkoColors.paper,
        contentWindowInsets = snurkoContentInsets,
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ChallengeHomeScreen(
                state = state,
                onAction = viewModel::onAction,
                onOpenHistory = onOpenHistory,
                onOpenReminders = onOpenReminders,
            )
            pendingSticker?.let { sticker ->
                DayStickerRenderer(sticker = sticker) { image ->
                    pendingSticker = null
                    scope.launch {
                        stickerSharer.share(image, sticker.stickerFileName())
                            .onFailure { snackbarHostState.showSnackbar(shareFailedMessage) }
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeHomeScreen(
    state: ChallengeHomeState,
    onAction: (ChallengeHomeAction) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val restart = state.restart
    if (restart != null) {
        RestartMoment(
            restart = restart,
            onAcknowledge = { onAction(ChallengeHomeAction.RestartAcknowledged) },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnurkoColors.paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PAGE_PADDING)
            .padding(top = 24.dp, bottom = 32.dp + navigationBarInset()),
    ) {
        if (state.isLoading) return@Column

        HomeHeader(state = state)
        Spacer(Modifier.height(20.dp))
        DayGrid(cells = state.cells, markedDays = state.markedDays, totalDays = state.totalDays)
        Spacer(Modifier.height(28.dp))
        if (state.isCompleted) {
            CompletedNote(totalDays = state.totalDays)
        } else {
            TodayChecklist(
                habits = state.habits,
                onToggle = { onAction(ChallengeHomeAction.HabitToggled(it)) },
            )
        }
        Spacer(Modifier.height(20.dp))
        FooterMarginNotes(
            onShare = { onAction(ChallengeHomeAction.ShareRequested) },
            onOpenReminders = onOpenReminders,
            onOpenHistory = onOpenHistory,
        )
    }
}

@Composable
private fun FooterMarginNotes(
    onShare: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.End),
    ) {
        MarginNote(text = stringResource(Res.string.home_share_link), onClick = onShare)
        MarginNote(text = stringResource(Res.string.home_reminders_link), onClick = onOpenReminders)
        MarginNote(text = stringResource(Res.string.home_history_link), onClick = onOpenHistory)
    }
}

@Composable
private fun MarginNote(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(QuietTapShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = SnurkoColors.textSecondary,
        )
    }
}

@Composable
private fun RestartMoment(
    restart: RestartMomentUi,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnurkoColors.paper)
            .padding(horizontal = 28.dp)
            .padding(top = 32.dp, bottom = 32.dp + navigationBarInset()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.home_restart_title, restart.failedOnDay),
            style = MaterialTheme.typography.displaySmall,
            color = SnurkoColors.text,
        )
        if (restart.missedHabits.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(
                    Res.string.home_restart_missed,
                    restart.missedHabits.joinToString(stringResource(Res.string.home_restart_separator)),
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = SnurkoColors.textSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.home_restart_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.textSecondary,
        )
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onAcknowledge,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnurkoColors.accentInk,
                contentColor = SnurkoColors.surface,
            ),
        ) {
            Text(
                text = stringResource(Res.string.home_restart_acknowledge),
                style = MaterialTheme.typography.titleMedium,
            )
        }
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
                size = GRID_MARK_SIZE,
                color = SnurkoColors.text,
            )
        }
    }
}

@Composable
private fun CompletedNote(totalDays: Int) {
    Text(
        text = stringResource(Res.string.home_completed, totalDays),
        style = MaterialTheme.typography.displayMedium,
        color = SnurkoColors.text,
        modifier = Modifier.fillMaxWidth(),
    )
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
            .clip(QuietTapShape)
            .toggleable(
                value = habit.isChecked,
                role = Role.Checkbox,
                onValueChange = { onToggle(habit.id) },
            )
            .padding(horizontal = 6.dp, vertical = 8.dp),
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
                size = CHECKLIST_MARK_SIZE,
                color = SnurkoColors.text,
            )
        }
    }
}

@Composable
internal fun PencilMark(
    rotation: Float,
    size: Dp,
    color: Color,
) {
    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
            .clearAndSetSemantics { },
    ) {
        val side = this.size.minDimension
        val stroke = maxOf(side * MARK_STROKE_RATIO, MARK_MIN_STROKE.toPx())
        val mark = Path().apply {
            moveTo(side * 0.20f, side * 0.55f)
            lineTo(side * 0.42f, side * 0.78f)
            lineTo(side * 0.82f, side * 0.24f)
        }
        drawPath(
            path = mark,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Preview
@Composable
private fun ChallengeHomeScreenPreview() {
    SnurkoTheme {
        ChallengeHomeScreen(state = previewState, onAction = {}, onOpenHistory = {}, onOpenReminders = {})
    }
}

@Preview
@Composable
private fun ChallengeHomeCompletedPreview() {
    SnurkoTheme {
        ChallengeHomeScreen(
            state = previewState.copy(
                dayIndex = 90,
                streak = 90,
                cells = List(90) { GridCellUi.COMPLETE },
                habits = emptyList(),
                isCompleted = true,
            ),
            onAction = {},
            onOpenHistory = {},
            onOpenReminders = {},
        )
    }
}

@Preview
@Composable
private fun ChallengeHomeRestartMomentPreview() {
    SnurkoTheme {
        ChallengeHomeScreen(
            state = previewState.copy(
                restart = RestartMomentUi(failedOnDay = 12, missedHabits = listOf("Read", "No alcohol")),
            ),
            onAction = {},
            onOpenHistory = {},
            onOpenReminders = {},
        )
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
