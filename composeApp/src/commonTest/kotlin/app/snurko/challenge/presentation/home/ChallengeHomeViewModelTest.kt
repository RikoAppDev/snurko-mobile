package app.snurko.challenge.presentation.home

import app.cash.turbine.test
import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.challenge.domain.READ_ID
import app.snurko.challenge.domain.WORKOUT_ID
import app.snurko.challenge.domain.activeChallenge
import app.snurko.challenge.domain.completeDaysThrough
import app.snurko.challenge.domain.day
import app.snurko.challenge.domain.partialDay
import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChallengeHomeViewModelTest {

    private lateinit var repository: FakeChallengeRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeChallengeRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theHomeIsLoadingUntilTheChallengeArrives() {
        val state = viewModel(today = day(1)).state.value

        assertThat(state.isLoading).isTrue()
        assertThat(state.cells).isEmpty()
        assertThat(state.habits).isEmpty()
    }

    @Test
    fun theActiveChallengeIsRenderedAsAGridAndAChecklist() {
        repository.setActiveChallenge(activeChallenge(checks = completeDaysThrough(3)))

        val state = viewModel(today = day(4)).state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.dayIndex).isEqualTo(4)
        assertThat(state.streak).isEqualTo(3)
        assertThat(state.cells[0]).isEqualTo(GridCellUi.COMPLETE)
        assertThat(state.cells[3]).isEqualTo(GridCellUi.TODAY)
        assertThat(state.habits.map { it.name }).containsExactly("Workout", "Read")
    }

    @Test
    fun checkingAHabitRecordsItForTodayOnly() {
        repository.setActiveChallenge(activeChallenge(checks = completeDaysThrough(2)))
        val viewModel = viewModel(today = day(3))

        viewModel.onAction(ChallengeHomeAction.HabitToggled(WORKOUT_ID))

        assertThat(repository.checkCalls).containsExactly(
            FakeChallengeRepository.CheckCall(
                attemptId = 1,
                day = 3,
                habitId = WORKOUT_ID,
                checked = true,
            ),
        )
        assertThat(viewModel.state.value.habits.single { it.id == WORKOUT_ID }.isChecked).isTrue()
    }

    @Test
    fun checkingAHabitAgainUnchecksIt() {
        repository.setActiveChallenge(
            activeChallenge(checks = completeDaysThrough(2) + partialDay(3)),
        )
        val viewModel = viewModel(today = day(3))

        viewModel.onAction(ChallengeHomeAction.HabitToggled(WORKOUT_ID))

        assertThat(repository.checkCalls.single().checked).isFalse()
        assertThat(viewModel.state.value.habits.single { it.id == WORKOUT_ID }.isChecked).isFalse()
    }

    @Test
    fun checkingTheLastHabitMarksTodaysCellStraightAway() {
        repository.setActiveChallenge(
            activeChallenge(checks = completeDaysThrough(2) + partialDay(3)),
        )
        val viewModel = viewModel(today = day(3))
        assertThat(viewModel.state.value.cells[2]).isEqualTo(GridCellUi.TODAY)

        viewModel.onAction(ChallengeHomeAction.HabitToggled(READ_ID))

        assertThat(viewModel.state.value.cells[2]).isEqualTo(GridCellUi.TODAY_COMPLETE)
        assertThat(viewModel.state.value.streak).isEqualTo(3)
    }

    @Test
    fun togglingAHabitThatIsNotOnTheListChangesNothing() {
        repository.setActiveChallenge(activeChallenge())
        val viewModel = viewModel(today = day(1))

        viewModel.onAction(ChallengeHomeAction.HabitToggled(habitId = 404))

        assertThat(repository.checkCalls).isEmpty()
    }

    @Test
    fun aCheckThatCannotBeSavedIsReported() = runTest {
        repository.setActiveChallenge(activeChallenge())
        repository.checkResult = Result.Error(DataError.Local.DISK_FULL)
        val viewModel = viewModel(today = day(1))

        viewModel.events.test {
            viewModel.onAction(ChallengeHomeAction.HabitToggled(WORKOUT_ID))

            assertThat(awaitItem()).isEqualTo(ChallengeHomeEvent.UpdateFailed)
        }
        assertThat(viewModel.state.value.habits.single { it.id == WORKOUT_ID }.isChecked).isFalse()
    }

    private fun viewModel(today: LocalDate) = ChallengeHomeViewModel(
        challengeRepository = repository,
        dateProvider = { today },
    )
}
