package app.snurko.widget

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThanOrEqualTo
import kotlin.test.Test

private const val EPSILON = 0.01f

class WidgetLayoutTest {

    @Test
    fun aWideShortWidgetPutsTheGridLeftAndEveryTaskRight() {
        val layout = sideBySide(width = 320f, height = 165f)

        assertThat(layout.taskRows).isGreaterThanOrEqualTo(6)
        assertThat(layout.cell).isGreaterThanOrEqualTo(15f)
    }

    @Test
    fun aLargeWideWidgetKeepsGridAndTasksSideBySide() {
        val layout = sideBySide(width = 400f, height = 220f)

        assertThat(layout.taskRows).isGreaterThanOrEqualTo(6)
        assertThat(layout.cell).isGreaterThanOrEqualTo(20f)
    }

    @Test
    fun theSideBySideGridAlwaysLeavesRoomForTheTaskColumn() {
        val width = 240f
        val layout = sideBySide(width = width, height = 300f)
        val gridWidth = layout.cell * WIDGET_GRID_COLUMNS

        assertThat(width - 2 * WidgetMetrics.PADDING - WidgetMetrics.GUTTER - gridWidth)
            .isGreaterThanOrEqualTo(WidgetMetrics.MIN_TASK_COLUMN)
    }

    @Test
    fun theSideBySideGridFitsTheHeight() {
        val height = 140f
        val layout = sideBySide(width = 320f, height = height)

        assertThat(layout.cell * WIDGET_GRID_ROWS).isLessThanOrEqualTo(height - 2 * WidgetMetrics.PADDING + EPSILON)
    }

    @Test
    fun aNarrowTallWidgetStacksTheTasksUnderTheGrid() {
        val layout = widgetLayoutFor(width = 160f, height = 300f)

        assertThat(layout).isInstanceOf(WidgetLayout.Stacked::class)
        layout as WidgetLayout.Stacked
        assertThat(layout.taskRows).isGreaterThanOrEqualTo(4)
    }

    @Test
    fun theStackedLayoutFitsGridHeaderAndTasksIntoTheHeight() {
        val height = 300f
        val layout = widgetLayoutFor(width = 160f, height = height) as WidgetLayout.Stacked
        val used = WidgetMetrics.HEADER + WidgetMetrics.HEADER_GAP + layout.cell * WIDGET_GRID_ROWS +
            WidgetMetrics.TASK_GAP + layout.taskRows * WidgetMetrics.TASK_ROW

        assertThat(used).isLessThanOrEqualTo(height - 2 * WidgetMetrics.PADDING + EPSILON)
    }

    @Test
    fun aSmallSquareStillShowsTheGrid() {
        val layout = widgetLayoutFor(width = 160f, height = 140f)

        assertThat(layout).isInstanceOf(WidgetLayout.Stacked::class)
    }

    @Test
    fun theSmallestWidgetShowsOnlyTheHeadline() {
        assertThat(widgetLayoutFor(width = 110f, height = 40f)).isEqualTo(WidgetLayout.HeadlineOnly)
    }

    @Test
    fun aShortNarrowWidgetHasNoRoomForAReadableGrid() {
        assertThat(widgetLayoutFor(width = 160f, height = 90f)).isEqualTo(WidgetLayout.HeadlineOnly)
    }

    @Test
    fun cellsNeverGrowPastTheMaximum() {
        val layout = sideBySide(width = 400f, height = 330f)

        assertThat(layout.cell).isLessThanOrEqualTo(WidgetMetrics.MAX_CELL)
    }

    @Test
    fun everyResponsiveSizeHasALayoutThatFitsIt() {
        for (size in WIDGET_SIZES) {
            when (val layout = widgetLayoutFor(size.width, size.height)) {
                WidgetLayout.HeadlineOnly -> Unit
                is WidgetLayout.SideBySide ->
                    assertThat(layout.cell * WIDGET_GRID_ROWS).isLessThanOrEqualTo(size.height - 2 * WidgetMetrics.PADDING + EPSILON)
                is WidgetLayout.Stacked ->
                    assertThat(layout.cell * WIDGET_GRID_COLUMNS).isLessThanOrEqualTo(size.width - 2 * WidgetMetrics.PADDING + EPSILON)
            }
        }
    }

    @Test
    fun theResponsiveSizesStayWithinThePlatformLimit() {
        assertThat(WIDGET_SIZES.size).isLessThanOrEqualTo(16)
    }

    @Test
    fun aWidgetBetweenTheDeclaredSizesStillGetsTheTaskColumn() {
        val picked = bestFittingSize(width = 360f, height = 175f)

        assertThat(widgetLayoutFor(picked.width, picked.height)).isInstanceOf(WidgetLayout.SideBySide::class)
    }

    private fun sideBySide(width: Float, height: Float): WidgetLayout.SideBySide {
        val layout = widgetLayoutFor(width = width, height = height)
        assertThat(layout).isInstanceOf(WidgetLayout.SideBySide::class)
        return layout as WidgetLayout.SideBySide
    }

    /** Mirrors the platform's choice: the declared size that fits and lies closest to the real one. */
    private fun bestFittingSize(width: Float, height: Float): WidgetSize =
        WIDGET_SIZES
            .filter { it.width <= width + 1 && it.height <= height + 1 }
            .minBy { (width - it.width) * (width - it.width) + (height - it.height) * (height - it.height) }
}
