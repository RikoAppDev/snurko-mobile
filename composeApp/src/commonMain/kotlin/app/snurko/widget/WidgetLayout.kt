package app.snurko.widget

import kotlin.math.floor
import kotlin.math.min

/** All values in dp. */
object WidgetMetrics {
    const val PADDING = 12f
    const val GUTTER = 12f
    const val HEADER = 24f
    const val HEADER_GAP = 6f
    const val TASK_GAP = 8f
    const val TASK_ROW = 18f
    const val MIN_TASK_COLUMN = 112f
    const val MIN_CELL = 7f
    const val MAX_CELL = 24f
}

data class WidgetSize(val width: Float, val height: Float)

/**
 * The sizes the widget is composed for. The host shows the one that fits its real space and lies
 * closest to it, so a dense set keeps the layout close to whatever a launcher hands out — and the
 * choice rests on the space actually laid out, not on the size the launcher reports. The platform
 * accepts at most sixteen.
 */
val WIDGET_SIZES: List<WidgetSize> = listOf(
    WidgetSize(110f, 40f),
    WidgetSize(160f, 90f),
    WidgetSize(160f, 140f),
    WidgetSize(160f, 220f),
    WidgetSize(160f, 300f),
    WidgetSize(240f, 90f),
    WidgetSize(240f, 140f),
    WidgetSize(240f, 220f),
    WidgetSize(240f, 300f),
    WidgetSize(320f, 90f),
    WidgetSize(320f, 140f),
    WidgetSize(320f, 165f),
    WidgetSize(320f, 220f),
    WidgetSize(320f, 300f),
    WidgetSize(400f, 220f),
    WidgetSize(400f, 330f),
)

sealed interface WidgetLayout {

    /** Too small for a readable grid: the day and today's count only. */
    data object HeadlineOnly : WidgetLayout

    /** Grid on the left; headline and today's tasks in the column on the right. */
    data class SideBySide(val cell: Float, val taskRows: Int) : WidgetLayout

    /** Headline on top, the grid under it, today's tasks under the grid when there is room. */
    data class Stacked(val cell: Float, val taskRows: Int) : WidgetLayout
}

/**
 * Picks the arrangement that shows the most of today's tasks, and among equals the larger grid.
 * [taskCount] is how many tasks there are to show; zero when the widget has none to list.
 */
fun widgetLayoutFor(
    width: Float,
    height: Float,
    taskCount: Int = Int.MAX_VALUE,
): WidgetLayout {
    val candidates = listOfNotNull(sideBySide(width, height, taskCount), stacked(width, height))
    val best = candidates.maxWithOrNull(
        compareBy<WidgetLayout> { minOf(it.taskRows, taskCount) }.thenBy { it.cell },
    )
    return best ?: WidgetLayout.HeadlineOnly
}

private val WidgetLayout.cell: Float
    get() = when (this) {
        WidgetLayout.HeadlineOnly -> 0f
        is WidgetLayout.SideBySide -> cell
        is WidgetLayout.Stacked -> cell
    }

private val WidgetLayout.taskRows: Int
    get() = when (this) {
        WidgetLayout.HeadlineOnly -> 0
        is WidgetLayout.SideBySide -> taskRows
        is WidgetLayout.Stacked -> taskRows
    }

private fun sideBySide(width: Float, height: Float, taskCount: Int): WidgetLayout.SideBySide? {
    with(WidgetMetrics) {
        val innerHeight = height - 2 * PADDING
        val gridWidth = width - 2 * PADDING - GUTTER - MIN_TASK_COLUMN
        val cell = min(min(innerHeight / WIDGET_GRID_ROWS, gridWidth / WIDGET_GRID_COLUMNS), MAX_CELL)
        val taskRows = rowsIn(innerHeight - HEADER - HEADER_GAP)
        if (cell < MIN_CELL) return null
        if (taskCount > 0 && taskRows < 1) return null
        return WidgetLayout.SideBySide(cell = cell, taskRows = taskRows)
    }
}

private fun stacked(width: Float, height: Float): WidgetLayout.Stacked? {
    with(WidgetMetrics) {
        val gridHeight = height - 2 * PADDING - HEADER - HEADER_GAP
        val cell = min(min((width - 2 * PADDING) / WIDGET_GRID_COLUMNS, gridHeight / WIDGET_GRID_ROWS), MAX_CELL)
        if (cell < MIN_CELL) return null
        val taskRows = rowsIn(gridHeight - cell * WIDGET_GRID_ROWS - TASK_GAP)
        return WidgetLayout.Stacked(cell = cell, taskRows = taskRows)
    }
}

private fun rowsIn(height: Float): Int = floor(height / WidgetMetrics.TASK_ROW).toInt().coerceAtLeast(0)
