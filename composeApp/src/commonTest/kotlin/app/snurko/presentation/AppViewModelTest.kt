package app.snurko.presentation

import app.cash.turbine.test
import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.challenge.domain.activeChallenge
import app.snurko.challenge.domain.day
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AppViewModelTest {

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
    fun theAppLoadsBeforeAnythingIsObserved() {
        assertThat(viewModel(today = day(1)).state.value).isEqualTo(AppUiState.Loading)
    }

    @Test
    fun withoutAnActiveChallengeTheAppShowsSetup() = runTest {
        viewModel(today = day(1)).state.test {
            assertThat(awaitItem()).isEqualTo(AppUiState.Setup)
        }
    }

    @Test
    fun anActiveChallengeShowsTheCurrentDayAndHabitCount() = runTest {
        repository.setActiveChallenge(activeChallenge())

        viewModel(today = day(4)).state.test {
            assertThat(awaitItem()).isEqualTo(AppUiState.Started(day = 4, habitCount = 2))
        }
    }

    @Test
    fun startingAChallengeSwitchesTheAppAwayFromSetup() = runTest {
        val viewModel = viewModel(today = day(1))

        viewModel.state.test {
            assertThat(awaitItem()).isEqualTo(AppUiState.Setup)

            repository.setActiveChallenge(activeChallenge())

            assertThat(awaitItem()).isEqualTo(AppUiState.Started(day = 1, habitCount = 2))
        }
    }

    private fun viewModel(today: LocalDate) = AppViewModel(
        challengeRepository = repository,
        dateProvider = { today },
    )
}
