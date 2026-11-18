package app.snurko.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.snurko.MainActivity
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.presentation.home.markRotationDegrees
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.dates
import app.snurko.resources.Res
import app.snurko.resources.home_completed
import app.snurko.resources.home_day
import app.snurko.resources.home_restart_explanation
import app.snurko.resources.home_restart_title
import app.snurko.resources.widget_invite
import app.snurko.resources.widget_open
import app.snurko.resources.widget_progress
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val MARK_BITMAP_DP = 20f
private const val CELL_GAP = 3f
private const val CELL_CORNER = 2f
private const val WIDGET_CORNER = 16f
private const val TASK_BOX = 12f
private const val TASK_BOX_BORDER = 1f
private const val TASK_BOX_GAP = 6f
private val DATE_CHECK_INTERVAL = 1.minutes

/** A Glance row or column renders at most ten children. */
private const val MAX_COLUMN_CHILDREN = 10

private data class WidgetLabels(
    val headline: String,
    val detail: String?,
    val note: String?,
    val openApp: String,
)

private data class WidgetFrame(
    val state: WidgetState,
    val labels: WidgetLabels,
)

class SnurkoWidget : GlanceAppWidget(), KoinComponent {

    private val challengeRepository: ChallengeRepository by inject()
    private val dateProvider: DateProvider by inject()

    override val sizeMode = SizeMode.Responsive(
        WIDGET_SIZES.map { DpSize(it.width.dp, it.height.dp) }.toSet(),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val marks = PencilMarks.at((MARK_BITMAP_DP * context.resources.displayMetrics.density).roundToInt())
        val frames = widgetStates(
            activeChallenge = challengeRepository.observeActiveChallenge(),
            dates = dateProvider.dates(every = DATE_CHECK_INTERVAL),
        ).map { state -> WidgetFrame(state = state, labels = labelsFor(state)) }
        val firstFrame = frames.first()

        provideContent {
            val frame by frames.collectAsState(initial = firstFrame)
            SnurkoWidgetContent(state = frame.state, labels = frame.labels, marks = marks)
        }
    }

    private suspend fun labelsFor(state: WidgetState): WidgetLabels {
        val openApp = getString(Res.string.widget_open)
        return when {
            state !is WidgetState.Challenge ->
                WidgetLabels(headline = getString(Res.string.widget_invite), detail = null, note = null, openApp = openApp)

            state.failedOnDay != null -> WidgetLabels(
                headline = getString(Res.string.home_restart_title, state.failedOnDay),
                detail = null,
                note = getString(Res.string.home_restart_explanation),
                openApp = openApp,
            )

            state.isCompleted -> WidgetLabels(
                headline = getString(Res.string.home_completed, state.totalDays),
                detail = null,
                note = null,
                openApp = openApp,
            )

            else -> WidgetLabels(
                headline = getString(Res.string.home_day, state.dayIndex),
                detail = getString(Res.string.widget_progress, state.checkedHabits, state.totalHabits),
                note = null,
                openApp = openApp,
            )
        }
    }
}

/**
 * The marks are drawn once at one size and scaled into every cell, so all sixteen composed sizes
 * share the same bitmaps and RemoteViews ships each of them only once.
 */
private object PencilMarks {
    private var cached: Pair<Int, Map<Float, Bitmap>>? = null

    @Synchronized
    fun at(sizePx: Int): Map<Float, Bitmap> =
        cached?.takeIf { it.first == sizePx }?.second
            ?: pencilCheckBitmaps(sizePx, SnurkoColors.text).also { cached = sizePx to it }
}

/**
 * Reuses the task the launcher would resume instead of stacking a second copy of the
 * activity, so one back press leaves the app.
 */
private fun openAppIntent(context: Context) = Intent(context, MainActivity::class.java).apply {
    action = Intent.ACTION_MAIN
    addCategory(Intent.CATEGORY_LAUNCHER)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
        Intent.FLAG_ACTIVITY_SINGLE_TOP
}

@Composable
private fun SnurkoWidgetContent(state: WidgetState, labels: WidgetLabels, marks: Map<Float, Bitmap>) {
    val size = LocalSize.current
    val context = LocalContext.current
    val challenge = state as? WidgetState.Challenge
    val tasks = challenge?.takeIf { !it.isCompleted && it.failedOnDay == null }?.habits.orEmpty()
    val layout = if (challenge == null) {
        WidgetLayout.HeadlineOnly
    } else {
        widgetLayoutFor(width = size.width.value, height = size.height.value, taskCount = tasks.size)
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(SnurkoColors.paper))
            .cornerRadius(WIDGET_CORNER.dp)
            .clickable(actionStartActivity(openAppIntent(context)))
            .semantics { contentDescription = labels.openApp }
            .padding(WidgetMetrics.PADDING.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        when (layout) {
            WidgetLayout.HeadlineOnly -> Header(labels = labels, headlineLines = 2)

            is WidgetLayout.SideBySide -> Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DayGrid(state = challenge!!, cell = layout.cell, marks = marks)
                Spacer(GlanceModifier.width(WidgetMetrics.GUTTER.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Header(labels = labels, headlineLines = 1)
                    Spacer(GlanceModifier.height(WidgetMetrics.HEADER_GAP.dp))
                    Below(labels = labels, tasks = tasks, rows = layout.taskRows, marks = marks)
                }
            }

            is WidgetLayout.Stacked -> Column(modifier = GlanceModifier.fillMaxSize()) {
                Header(labels = labels, headlineLines = 1)
                Spacer(GlanceModifier.height(WidgetMetrics.HEADER_GAP.dp))
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DayGrid(state = challenge!!, cell = layout.cell, marks = marks)
                }
                if (layout.taskRows > 0) {
                    Spacer(GlanceModifier.height(WidgetMetrics.TASK_GAP.dp))
                    Below(labels = labels, tasks = tasks, rows = layout.taskRows, marks = marks)
                }
            }
        }
    }
}

/** Today's tasks while the attempt runs; the reason to open the app once it has failed. */
@Composable
private fun Below(labels: WidgetLabels, tasks: List<WidgetHabit>, rows: Int, marks: Map<Float, Bitmap>) {
    when {
        labels.note != null -> Text(
            text = labels.note,
            maxLines = rows.coerceAtLeast(1),
            style = TextStyle(color = ColorProvider(SnurkoColors.textSecondary), fontSize = 13.sp),
        )

        tasks.isNotEmpty() -> TaskList(tasks = tasks, rows = rows, marks = marks)
    }
}

@Composable
private fun Header(labels: WidgetLabels, headlineLines: Int) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = labels.headline,
            maxLines = headlineLines,
            style = TextStyle(
                color = ColorProvider(SnurkoColors.text),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (labels.detail != null) {
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = labels.detail,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(SnurkoColors.textSecondary),
                    fontSize = 14.sp,
                ),
            )
        }
    }
}

@Composable
private fun TaskList(tasks: List<WidgetHabit>, rows: Int, marks: Map<Float, Bitmap>) {
    val capacity = rows.coerceAtMost(MAX_COLUMN_CHILDREN)
    val visible = if (tasks.size <= capacity) tasks else tasks.take(capacity - 1)
    val hidden = tasks.size - visible.size

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        visible.forEachIndexed { index, task ->
            TaskRow(task = task, mark = marks[markRotationDegrees(index + 1)])
        }
        if (hidden > 0) {
            Text(
                text = "+$hidden",
                maxLines = 1,
                modifier = GlanceModifier.height(WidgetMetrics.TASK_ROW.dp),
                style = TextStyle(color = ColorProvider(SnurkoColors.textSecondary), fontSize = 13.sp),
            )
        }
    }
}

@Composable
private fun TaskRow(task: WidgetHabit, mark: Bitmap?) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(WidgetMetrics.TASK_ROW.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(TASK_BOX.dp)
                .cornerRadius(CELL_CORNER.dp)
                .background(ColorProvider(SnurkoColors.line)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = GlanceModifier
                    .size((TASK_BOX - 2 * TASK_BOX_BORDER).dp)
                    .cornerRadius(CELL_CORNER.dp)
                    .background(ColorProvider(SnurkoColors.surface)),
            ) {}
            if (task.checked && mark != null) {
                Image(
                    provider = ImageProvider(mark),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.size(TASK_BOX.dp),
                )
            }
        }
        Spacer(GlanceModifier.width(TASK_BOX_GAP.dp))
        Text(
            text = task.name,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(if (task.checked) SnurkoColors.textSecondary else SnurkoColors.text),
                fontSize = 13.sp,
            ),
        )
    }
}

/**
 * Every cell carries its own gap: a Glance row or column renders at most ten children, and
 * spacers between the cells would spend half of that budget.
 */
@Composable
private fun DayGrid(state: WidgetState.Challenge, cell: Float, marks: Map<Float, Bitmap>) {
    Column {
        state.cellRows().forEachIndexed { rowIndex, row ->
            Row {
                row.forEachIndexed { columnIndex, dayCell ->
                    DayCell(
                        cell = dayCell,
                        day = rowIndex * WIDGET_GRID_COLUMNS + columnIndex + 1,
                        slot = cell,
                        marks = marks,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(cell: WidgetCell, day: Int, slot: Float, marks: Map<Float, Bitmap>) {
    val face = (slot - CELL_GAP).coerceAtLeast(1f)
    val mark = marks[markRotationDegrees(day)].takeIf { cell == WidgetCell.MARKED }

    Box(
        modifier = GlanceModifier.size(slot.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = GlanceModifier
                .size(face.dp)
                .cornerRadius(CELL_CORNER.dp)
                .background(ColorProvider(cell.fill())),
            contentAlignment = Alignment.Center,
        ) {
            if (mark != null) {
                Image(
                    provider = ImageProvider(mark),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.size(face.dp),
                )
            }
        }
    }
}

private fun WidgetCell.fill() = when (this) {
    WidgetCell.MARKED -> SnurkoColors.surface
    WidgetCell.TODAY -> SnurkoColors.accent
    WidgetCell.MISSED -> SnurkoColors.line
    WidgetCell.FUTURE -> SnurkoColors.surface
}
