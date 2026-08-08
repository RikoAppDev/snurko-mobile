package app.snurko.challenge.presentation.home

import androidx.lifecycle.viewModelScope
import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.dayIndexOn
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.onFailure
import app.snurko.core.presentation.MviViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChallengeHomeViewModel(
    private val challengeRepository: ChallengeRepository,
    private val dateProvider: DateProvider,
) : MviViewModel<ChallengeHomeState, ChallengeHomeAction, ChallengeHomeEvent>(
    initialState = ChallengeHomeState(),
) {

    private var activeChallenge: ActiveChallenge? = null

    init {
        challengeRepository.observeActiveChallenge()
            .filterNotNull()
            .onEach(::render)
            .launchIn(viewModelScope)
    }

    override fun onAction(action: ChallengeHomeAction) {
        when (action) {
            is ChallengeHomeAction.HabitToggled -> toggleHabit(action.habitId)
        }
    }

    private fun render(active: ActiveChallenge) {
        activeChallenge = active
        updateState { active.toChallengeHomeState(dateProvider.today()) }
    }

    private fun toggleHabit(habitId: Long) {
        val active = activeChallenge ?: return
        val habit = state.value.habits.firstOrNull { it.id == habitId } ?: return

        viewModelScope.launch {
            challengeRepository.setHabitChecked(
                attemptId = active.attempt.id,
                day = active.dayIndexOn(dateProvider.today()),
                habitId = habit.id,
                checked = !habit.isChecked,
            ).onFailure { sendEvent(ChallengeHomeEvent.UpdateFailed) }
        }
    }
}
