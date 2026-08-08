package app.snurko.presentation

import app.cash.turbine.test
import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.challenge.domain.activeChallenge
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
        assertThat(viewModel().state.value).isEqualTo(AppUiState.Loading)
    }

    @Test
    fun withoutAnActiveChallengeTheAppShowsSetup() = runTest {
        viewModel().state.test {
            assertThat(awaitItem()).isEqualTo(AppUiState.Setup)
        }
    }

    @Test
    fun anActiveChallengeShowsTheHome() = runTest {
        repository.setActiveChallenge(activeChallenge())

        viewModel().state.test {
            assertThat(awaitItem()).isEqualTo(AppUiState.Started)
        }
    }

    @Test
    fun startingAChallengeSwitchesTheAppAwayFromSetup() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertThat(awaitItem()).isEqualTo(AppUiState.Setup)

            repository.setActiveChallenge(activeChallenge())

            assertThat(awaitItem()).isEqualTo(AppUiState.Started)
        }
    }

    private fun viewModel() = AppViewModel(challengeRepository = repository)
}
