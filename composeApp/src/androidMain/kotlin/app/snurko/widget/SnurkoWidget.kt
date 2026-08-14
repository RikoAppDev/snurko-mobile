package app.snurko.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
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
import app.snurko.resources.Res
import app.snurko.resources.home_completed
import app.snurko.resources.home_day
import app.snurko.resources.widget_invite
import app.snurko.resources.widget_open
import app.snurko.resources.widget_progress
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val SMALL = DpSize(120.dp, 60.dp)
private val MEDIUM = DpSize(180.dp, 110.dp)
private val LARGE = DpSize(250.dp, 180.dp)
private val EXTRA_LARGE = DpSize(320.dp, 340.dp)

private val GRID_MIN_HEIGHT = 160.dp
private val PAGE_PADDING = 12.dp
private val HEADER_HEIGHT = 34.dp
private val CELL_GAP = 3.dp
private val CELL_CORNER = 2.dp
private val MAX_CELL_SLOT = 24.dp
private val WIDGET_CORNER = 16.dp

private data class WidgetLabels(
    val headline: String,
    val detail: String?,
    val openApp: String,
)

class SnurkoWidget : GlanceAppWidget(), KoinComponent {

    private val challengeRepository: ChallengeRepository by inject()
    private val dateProvider: DateProvider by inject()

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE, EXTRA_LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = widgetState(
            active = challengeRepository.observeActiveChallenge().first(),
            today = dateProvider.today(),
        )
        val labels = labelsFor(state)

        provideContent { SnurkoWidgetContent(state = state, labels = labels) }
    }

    private suspend fun labelsFor(state: WidgetState): WidgetLabels = when (state) {
        WidgetState.NoChallenge -> WidgetLabels(
            headline = getString(Res.string.widget_invite),
            detail = null,
            openApp = getString(Res.string.widget_open),
        )

        is WidgetState.Challenge -> WidgetLabels(
            headline = if (state.isCompleted) {
                getString(Res.string.home_completed, state.totalDays)
            } else {
                getString(Res.string.home_day, state.dayIndex)
            },
            detail = if (state.isCompleted) {
                null
            } else {
                getString(Res.string.widget_progress, state.checkedHabits, state.totalHabits)
            },
            openApp = getString(Res.string.widget_open),
        )
    }
}

@Composable
private fun SnurkoWidgetContent(state: WidgetState, labels: WidgetLabels) {
    val size = LocalSize.current
    val gridState = (state as? WidgetState.Challenge)?.takeIf { size.height >= GRID_MIN_HEIGHT }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(SnurkoColors.paper))
            .cornerRadius(WIDGET_CORNER)
            .clickable(actionStartActivity<MainActivity>())
            .semantics { contentDescription = labels.openApp }
            .padding(PAGE_PADDING),
        verticalAlignment = if (gridState == null) Alignment.CenterVertically else Alignment.Top,
    ) {
        Header(labels = labels)
        if (gridState != null) {
            Spacer(GlanceModifier.height(8.dp))
            DayGrid(state = gridState, available = size)
        }
    }
}

@Composable
private fun Header(labels: WidgetLabels) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = labels.headline,
            maxLines = 2,
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

/**
 * Every cell carries its own gap: a Glance row or column renders at most ten children, and
 * spacers between the cells would spend half of that budget.
 */
@Composable
private fun DayGrid(state: WidgetState.Challenge, available: DpSize) {
    val slot = cellSlotFor(available)
    val face = slot - CELL_GAP
    val density = LocalContext.current.resources.displayMetrics.density
    val markPx = (face.value * density).toInt()
    val marks = remember(markPx) { pencilCheckBitmaps(markPx, SnurkoColors.text) }

    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.cellRows().forEachIndexed { rowIndex, row ->
            Row {
                row.forEachIndexed { columnIndex, cell ->
                    DayCell(
                        cell = cell,
                        day = rowIndex * WIDGET_GRID_COLUMNS + columnIndex + 1,
                        slot = slot,
                        marks = marks,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(cell: WidgetCell, day: Int, slot: Dp, marks: Map<Float, Bitmap>) {
    val face = slot - CELL_GAP
    val mark = marks[markRotationDegrees(day)].takeIf { cell == WidgetCell.MARKED }

    Box(
        modifier = GlanceModifier.size(slot),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = GlanceModifier
                .size(face)
                .cornerRadius(CELL_CORNER)
                .background(ColorProvider(cell.fill())),
            contentAlignment = Alignment.Center,
        ) {
            if (mark != null) {
                Image(
                    provider = ImageProvider(mark),
                    contentDescription = null,
                    modifier = GlanceModifier.size(face),
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

private fun cellSlotFor(available: DpSize): Dp {
    val byWidth = (available.width - PAGE_PADDING * 2) / WIDGET_GRID_COLUMNS
    val byHeight = (available.height - PAGE_PADDING * 2 - HEADER_HEIGHT) / WIDGET_GRID_ROWS
    return minOf(byWidth, byHeight, MAX_CELL_SLOT)
}
