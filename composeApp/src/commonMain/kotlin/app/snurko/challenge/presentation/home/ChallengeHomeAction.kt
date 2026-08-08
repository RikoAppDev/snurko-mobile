package app.snurko.challenge.presentation.home

import app.snurko.core.presentation.UiAction

sealed interface ChallengeHomeAction : UiAction {
    data class HabitToggled(val habitId: Long) : ChallengeHomeAction
    data object ScreenResumed : ChallengeHomeAction
    data object RestartAcknowledged : ChallengeHomeAction
}
