package app.snurko.core.presentation

import app.cash.turbine.test
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

private data class CounterState(val count: Int = 0) : UiState

private sealed interface CounterAction : UiAction {
    data object Increment : CounterAction
    data class ShowMessage(val message: String) : CounterAction
}

private sealed interface CounterEvent : UiEvent {
    data class MessageShown(val message: String) : CounterEvent
}

private class CounterViewModel : MviViewModel<CounterState, CounterAction, CounterEvent>(
    initialState = CounterState(),
) {
    override fun onAction(action: CounterAction) {
        when (action) {
            is CounterAction.Increment -> updateState { it.copy(count = it.count + 1) }
            is CounterAction.ShowMessage -> sendEvent(CounterEvent.MessageShown(action.message))
        }
    }
}

class MviViewModelTest {

    private lateinit var viewModel: CounterViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = CounterViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposesInitialState() = runTest {
        viewModel.state.test {
            assertThat(awaitItem()).isEqualTo(CounterState(count = 0))
        }
    }

    @Test
    fun actionUpdatesState() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.onAction(CounterAction.Increment)

            assertThat(awaitItem()).isEqualTo(CounterState(count = 1))
        }
    }

    @Test
    fun stateUpdatesAccumulate() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.onAction(CounterAction.Increment)
            awaitItem()
            viewModel.onAction(CounterAction.Increment)

            assertThat(awaitItem()).isEqualTo(CounterState(count = 2))
        }
    }

    @Test
    fun actionSendsOneTimeEvent() = runTest {
        viewModel.events.test {
            viewModel.onAction(CounterAction.ShowMessage("saved"))

            assertThat(awaitItem()).isEqualTo(CounterEvent.MessageShown("saved"))
        }
    }

    @Test
    fun eventsSentWhileUnsubscribedAreDeliveredExactlyOnce() = runTest {
        viewModel.onAction(CounterAction.ShowMessage("early"))

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(CounterEvent.MessageShown("early"))
            expectNoEvents()
        }

        viewModel.events.test {
            expectNoEvents()
        }
    }
}
