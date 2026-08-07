package app.snurko.challenge.presentation.setup

import app.snurko.core.presentation.UiEvent

sealed interface ChallengeSetupEvent : UiEvent {
    data object ChallengeStarted : ChallengeSetupEvent
    data object StartFailed : ChallengeSetupEvent
}
