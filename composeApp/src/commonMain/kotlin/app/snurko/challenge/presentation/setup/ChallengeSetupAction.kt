package app.snurko.challenge.presentation.setup

import app.snurko.core.presentation.UiAction

sealed interface ChallengeSetupAction : UiAction {
    data class HabitNameChanged(val value: String) : ChallengeSetupAction
    data class FailConditionChanged(val value: String) : ChallengeSetupAction
    data object HabitSubmitted : ChallengeSetupAction
    data class HabitEditRequested(val id: Long) : ChallengeSetupAction
    data object HabitEditCancelled : ChallengeSetupAction
    data class HabitRemoved(val id: Long) : ChallengeSetupAction
    data object StartRequested : ChallengeSetupAction
    data object StartConfirmed : ChallengeSetupAction
    data object StartCancelled : ChallengeSetupAction
}
