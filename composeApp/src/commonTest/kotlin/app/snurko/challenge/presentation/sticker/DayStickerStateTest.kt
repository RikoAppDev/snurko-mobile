package app.snurko.challenge.presentation.sticker

import app.snurko.challenge.domain.activeChallenge
import app.snurko.challenge.domain.completeDay
import app.snurko.challenge.domain.completeDaysThrough
import app.snurko.challenge.domain.day
import app.snurko.challenge.domain.partialDay
import app.snurko.challenge.presentation.home.GridCellUi
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class DayStickerStateTest {

    @Test
    fun theStickerCarriesTheDayTheLengthAndTheStreak() {
        val sticker = activeChallenge(checks = completeDaysThrough(11)).toDayStickerState(day(12))

        assertThat(sticker.dayIndex).isEqualTo(12)
        assertThat(sticker.totalDays).isEqualTo(90)
        assertThat(sticker.streak).isEqualTo(11)
        assertThat(sticker.isCompleted).isFalse()
    }

    @Test
    fun theStickerGridHasOneCellPerChallengeDay() {
        val sticker = activeChallenge().toDayStickerState(day(1))

        assertThat(sticker.cells).hasSize(90)
    }

    @Test
    fun theStickerGridCarriesTheRealMarksAndFramesToday() {
        val sticker = activeChallenge(checks = completeDaysThrough(3)).toDayStickerState(day(4))

        assertThat(sticker.cells.take(5)).containsExactly(
            GridCellUi.COMPLETE,
            GridCellUi.COMPLETE,
            GridCellUi.COMPLETE,
            GridCellUi.TODAY,
            GridCellUi.FUTURE,
        )
    }

    @Test
    fun aDayThatWasLostStaysAnEmptyCellOnTheSticker() {
        val sticker = activeChallenge(checks = listOf(completeDay(1), partialDay(2)))
            .toDayStickerState(day(3))

        assertThat(sticker.cells[1]).isEqualTo(GridCellUi.FAILED)
        assertThat(sticker.streak).isEqualTo(1)
    }

    @Test
    fun todayCarriesItsMarkOnceEveryHabitIsChecked() {
        val sticker = activeChallenge(checks = completeDaysThrough(3)).toDayStickerState(day(3))

        assertThat(sticker.cells[2]).isEqualTo(GridCellUi.TODAY_COMPLETE)
        assertThat(sticker.streak).isEqualTo(3)
    }

    @Test
    fun aFinishedChallengeGivesTheCompletedSticker() {
        val sticker = activeChallenge(checks = completeDaysThrough(90)).toDayStickerState(day(94))

        assertThat(sticker.isCompleted).isTrue()
        assertThat(sticker.dayIndex).isEqualTo(90)
        assertThat(sticker.streak).isEqualTo(90)
        assertThat(sticker.cells.all { it == GridCellUi.COMPLETE }).isTrue()
    }

    @Test
    fun theSharedFileIsNamedAfterTheDayItShows() {
        val sticker = activeChallenge(checks = completeDaysThrough(11)).toDayStickerState(day(12))

        assertThat(sticker.stickerFileName()).isEqualTo("snurko-day-12.png")
    }

    @Test
    fun theStickerReadsFromTheAttemptOnGoingNotTheFirstOne() {
        val sticker = activeChallenge(
            checks = listOf(completeDay(1)),
            attemptNumber = 2,
            attemptStart = day(10),
        ).toDayStickerState(day(11))

        assertThat(sticker.dayIndex).isEqualTo(2)
        assertThat(sticker.streak).isEqualTo(1)
        assertThat(sticker.cells[0]).isEqualTo(GridCellUi.COMPLETE)
        assertThat(sticker.cells[1]).isEqualTo(GridCellUi.TODAY)
    }
}
