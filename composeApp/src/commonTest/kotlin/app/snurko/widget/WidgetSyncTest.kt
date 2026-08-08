package app.snurko.widget

import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.challenge.domain.attemptRecord
import app.snurko.challenge.domain.completeDaysThrough
import app.snurko.challenge.domain.notebook
import app.snurko.core.domain.nextMidnightAfter
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test

class NextMidnightTest {

    @Test
    fun midnightIsTheStartOfTomorrow() {
        assertThat(nextMidnightAfter(LocalDateTime(2026, 3, 17, 14, 30)))
            .isEqualTo(LocalDateTime(2026, 3, 18, 0, 0))
    }

    @Test
    fun justAfterMidnightTheNextTurnIsStillADayAway() {
        assertThat(nextMidnightAfter(LocalDateTime(2026, 3, 17, 0, 1)))
            .isEqualTo(LocalDateTime(2026, 3, 18, 0, 0))
    }

    @Test
    fun midnightItselfPointsAtTheFollowingOne() {
        assertThat(nextMidnightAfter(LocalDateTime(2026, 3, 17, 0, 0)))
            .isEqualTo(LocalDateTime(2026, 3, 18, 0, 0))
    }

    @Test
    fun theTurnCanCrossAYear() {
        assertThat(nextMidnightAfter(LocalDateTime(2026, 12, 31, 23, 59)))
            .isEqualTo(LocalDateTime(2027, 1, 1, 0, 0))
    }
}

class WidgetSyncTest {

    @Test
    fun theWidgetIsRedrawnForTheNotebookItStartsWith() = runTest {
        val repository = FakeChallengeRepository()
        repository.setNotebook(notebook())
        val updater = RecordingWidgetUpdater()

        WidgetSync(repository, updater).start(scope())

        assertThat(updater.updateCount).isEqualTo(1)
    }

    @Test
    fun everyNotebookChangeRedrawsTheWidget() = runTest {
        val repository = FakeChallengeRepository()
        repository.setNotebook(notebook())
        val updater = RecordingWidgetUpdater()
        WidgetSync(repository, updater).start(scope())

        repository.setNotebook(notebook(attempts = listOf(attemptRecord(checks = completeDaysThrough(1)))))

        assertThat(updater.updateCount).isEqualTo(2)
    }

    @Test
    fun aWidgetWithoutAChallengeIsStillRedrawn() = runTest {
        val repository = FakeChallengeRepository()
        val updater = RecordingWidgetUpdater()

        WidgetSync(repository, updater).start(scope())

        assertThat(updater.updateCount).isEqualTo(1)
    }

    private fun TestScope.scope() = TestScope(UnconfinedTestDispatcher(testScheduler))
}

private class RecordingWidgetUpdater : WidgetUpdater {

    var updateCount = 0
        private set

    override suspend fun updateAll() {
        updateCount++
    }
}
