package app.snurko.challenge.presentation.home

import app.snurko.core.presentation.UiEvent

sealed interface ChallengeHomeEvent : UiEvent {
    data object UpdateFailed : ChallengeHomeEvent
}
