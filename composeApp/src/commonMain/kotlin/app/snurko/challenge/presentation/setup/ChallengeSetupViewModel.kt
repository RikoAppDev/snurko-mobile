package app.snurko.challenge.presentation.setup

import androidx.lifecycle.viewModelScope
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.DEFAULT_CHALLENGE_NAME
import app.snurko.challenge.domain.Habit
import app.snurko.challenge.domain.SetupDraftRepository
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.onFailure
import app.snurko.core.domain.onSuccess
import app.snurko.core.presentation.MviViewModel
import kotlinx.coroutines.launch

class ChallengeSetupViewModel(
    private val challengeRepository: ChallengeRepository,
    private val setupDraftRepository: SetupDraftRepository,
    private val dateProvider: DateProvider,
) : MviViewModel<ChallengeSetupState, ChallengeSetupAction, ChallengeSetupEvent>(
    initialState = ChallengeSetupState(),
) {

    private var nextDraftId = 1L

    init {
        viewModelScope.launch { restoreDraft() }
    }

    override fun onAction(action: ChallengeSetupAction) {
        when (action) {
            is ChallengeSetupAction.HabitNameChanged ->
                updateState { it.copy(habitNameInput = action.value) }

            is ChallengeSetupAction.FailConditionChanged ->
                updateState { it.copy(failConditionInput = action.value) }

            ChallengeSetupAction.HabitSubmitted -> submitHabit()
            is ChallengeSetupAction.HabitEditRequested -> editHabit(action.id)
            ChallengeSetupAction.HabitEditCancelled -> updateState { it.withClearedInput() }
            is ChallengeSetupAction.HabitRemoved -> removeHabit(action.id)

            ChallengeSetupAction.StartRequested ->
                updateState { if (it.canStart) it.copy(isConfirmingStart = true) else it }

            ChallengeSetupAction.StartCancelled -> updateState { it.copy(isConfirmingStart = false) }
            ChallengeSetupAction.StartConfirmed -> startChallenge()
        }
    }

    private fun submitHabit() {
        val current = state.value
        if (!current.canSubmitHabit) return

        val editedId = current.editedHabitId
        val habits = if (editedId == null) {
            current.habits + HabitDraftUi(
                id = nextDraftId++,
                name = current.habitName,
                failCondition = current.failCondition,
            )
        } else {
            current.habits.map { habit ->
                if (habit.id == editedId) {
                    habit.copy(name = current.habitName, failCondition = current.failCondition)
                } else {
                    habit
                }
            }
        }

        updateState { it.copy(habits = habits).withClearedInput() }
        persistDraft(habits)
    }

    private fun editHabit(id: Long) {
        val habit = state.value.habits.firstOrNull { it.id == id } ?: return
        updateState {
            it.copy(
                habitNameInput = habit.name,
                failConditionInput = habit.failCondition.orEmpty(),
                editedHabitId = id,
            )
        }
    }

    private fun removeHabit(id: Long) {
        updateState { current ->
            val habits = current.habits.filterNot { it.id == id }
            val without = current.copy(habits = habits)
            if (current.editedHabitId == id) without.withClearedInput() else without
        }
        persistDraft(state.value.habits)
    }

    private fun startChallenge() {
        val current = state.value
        if (!current.canStart) return

        updateState { it.copy(isConfirmingStart = false, isStarting = true) }

        viewModelScope.launch {
            challengeRepository.createChallenge(
                name = DEFAULT_CHALLENGE_NAME,
                startDate = dateProvider.today(),
                habits = current.habits.map {
                    Habit(name = it.name, failCondition = it.failCondition)
                },
            )
                .onSuccess {
                    setupDraftRepository.clear()
                    sendEvent(ChallengeSetupEvent.ChallengeStarted)
                }
                .onFailure {
                    updateState { state -> state.copy(isStarting = false) }
                    sendEvent(ChallengeSetupEvent.StartFailed)
                }
        }
    }

    private suspend fun restoreDraft() {
        val restored = setupDraftRepository.load()
        if (restored.isEmpty()) return

        val drafts = restored.map { habit ->
            HabitDraftUi(
                id = nextDraftId++,
                name = habit.name,
                failCondition = habit.failCondition,
            )
        }
        updateState { current ->
            if (current.habits.isEmpty()) current.copy(habits = drafts) else current
        }
    }

    private fun persistDraft(habits: List<HabitDraftUi>) {
        viewModelScope.launch {
            setupDraftRepository.save(
                habits.map { Habit(name = it.name, failCondition = it.failCondition) },
            )
        }
    }

    private fun ChallengeSetupState.withClearedInput(): ChallengeSetupState = copy(
        habitNameInput = "",
        failConditionInput = "",
        editedHabitId = null,
    )
}
