package app.snurko.widget

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.snurko.challenge.domain.activeChallenge
import app.snurko.challenge.domain.completeDaysThrough
import app.snurko.challenge.domain.day
import app.snurko.challenge.domain.partialDay
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.dates
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WidgetStatesTest {

    @Test
    fun aHabitCheckedWhileTheWidgetIsShownRedrawsItsMarks() = runTest {
        val challenges = MutableStateFlow(activeChallenge(checks = completeDaysThrough(2)))

        widgetStates(challenges, flowOf(day(3))).test {
            assertThat(awaitChallenge().checkedHabits).isEqualTo(0)

            challenges.value = activeChallenge(checks = completeDaysThrough(2) + partialDay(3))

            assertThat(awaitChallenge().checkedHabits).isEqualTo(1)
        }
    }

    @Test
    fun theWidgetTurnsToTheNextDayWhenTheDateChanges() = runTest {
        val dates = MutableStateFlow(day(3))

        widgetStates(flowOf(activeChallenge(checks = completeDaysThrough(3))), dates).test {
            assertThat(awaitChallenge().dayIndex).isEqualTo(3)

            dates.value = day(4)

            assertThat(awaitChallenge().dayIndex).isEqualTo(4)
        }
    }

    @Test
    fun theSameStateTwiceIsDrawnOnce() = runTest {
        widgetStates(flowOf(activeChallenge()), flowOf(day(3), day(3))).test {
            awaitChallenge()
            awaitComplete()
        }
    }

    @Test
    fun theCalendarIsReadAgainAfterEveryInterval() = runTest {
        var now = LocalDateTime(2026, 3, 17, 23, 59)
        val dateProvider = DateProvider { now }

        dateProvider.dates(every = 1.minutes).test {
            assertThat(awaitItem()).isEqualTo(LocalDate(2026, 3, 17))

            now = LocalDateTime(2026, 3, 18, 0, 0)
            testScheduler.advanceTimeBy(1.minutes + 1.seconds)

            assertThat(awaitItem()).isEqualTo(LocalDate(2026, 3, 18))
        }
    }

    @Test
    fun anUnchangedDateIsNotRepeated() = runTest {
        val dateProvider = DateProvider { LocalDateTime(2026, 3, 17, 12, 0) }

        dateProvider.dates(every = 1.minutes).test {
            assertThat(awaitItem()).isEqualTo(LocalDate(2026, 3, 17))

            testScheduler.advanceTimeBy(5.minutes)

            expectNoEvents()
        }
    }

    private suspend fun ReceiveTurbine<WidgetState>.awaitChallenge(): WidgetState.Challenge =
        awaitItem() as WidgetState.Challenge
}
