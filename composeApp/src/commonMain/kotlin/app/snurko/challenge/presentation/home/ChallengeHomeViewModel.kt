package app.snurko.challenge.presentation.home

import androidx.lifecycle.viewModelScope
import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.domain.AttemptOutcome
import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.dayIndexOn
import app.snurko.challenge.domain.evaluate
import app.snurko.challenge.domain.uncheckedHabitsOn
import app.snurko.challenge.presentation.sticker.toDayStickerState
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.onFailure
import app.snurko.core.domain.onSuccess
import app.snurko.core.presentation.MviViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class ChallengeHomeViewModel(
    private val challengeRepository: ChallengeRepository,
    private val dateProvider: DateProvider,
) : MviViewModel<ChallengeHomeState, ChallengeHomeAction, ChallengeHomeEvent>(
    initialState = ChallengeHomeState(),
) {

    private val resumeTicks = MutableStateFlow(0)

    private var activeChallenge: ActiveChallenge? = null
    private var restartedAttemptId: Long? = null

    init {
        combine(challengeRepository.observeActiveChallenge(), resumeTicks) { active, _ -> active }
            .filterNotNull()
            .onEach(::evaluateAttempt)
            .launchIn(viewModelScope)
    }

    override fun onAction(action: ChallengeHomeAction) {
        when (action) {
            is ChallengeHomeAction.HabitToggled -> toggleHabit(action.habitId)
            ChallengeHomeAction.ScreenResumed -> resumeTicks.update { it + 1 }
            ChallengeHomeAction.RestartAcknowledged -> updateState { it.copy(restart = null) }
            ChallengeHomeAction.ShareRequested -> shareSticker()
        }
    }

    private fun shareSticker() {
        val active = activeChallenge ?: return
        val current = state.value
        if (current.isLoading || current.restart != null) return

        sendEvent(ChallengeHomeEvent.ShareSticker(active.toDayStickerState(dateProvider.today())))
    }

    private suspend fun evaluateAttempt(active: ActiveChallenge) {
        activeChallenge = active
        val today = dateProvider.today()
        when (val outcome = active.evaluate(today)) {
            is AttemptOutcome.Failed -> restart(active, outcome.failedOnDay, today)
            AttemptOutcome.Completed -> complete(active, today)
            AttemptOutcome.Ongoing -> render(active, today)
        }
    }

    private suspend fun complete(active: ActiveChallenge, today: LocalDate) {
        if (active.attempt.status == AttemptStatus.ACTIVE) {
            challengeRepository.completeAttempt(active.attempt.id)
                .onFailure { sendEvent(ChallengeHomeEvent.UpdateFailed) }
        }
        render(active, today)
    }

    private suspend fun restart(active: ActiveChallenge, failedOnDay: Int, today: LocalDate) {
        if (restartedAttemptId == active.attempt.id) return
        restartedAttemptId = active.attempt.id
        val missedHabits = active.uncheckedHabitsOn(failedOnDay).map { it.name }

        challengeRepository.failAttemptAndStartNext(
            attemptId = active.attempt.id,
            failedOnDay = failedOnDay,
            nextAttemptStartDate = today,
        )
            .onSuccess {
                updateState { state ->
                    if (state.restart == null) {
                        state.copy(restart = RestartMomentUi(failedOnDay, missedHabits))
                    } else {
                        state
                    }
                }
            }
            .onFailure {
                restartedAttemptId = null
                render(active, today)
                sendEvent(ChallengeHomeEvent.UpdateFailed)
            }
    }

    private fun render(active: ActiveChallenge, today: LocalDate) {
        updateState { state -> active.toChallengeHomeState(today).copy(restart = state.restart) }
    }

    private fun toggleHabit(habitId: Long) {
        val active = activeChallenge ?: return
        val current = state.value
        if (current.restart != null || current.isCompleted) return
        val habit = current.habits.firstOrNull { it.id == habitId } ?: return

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
