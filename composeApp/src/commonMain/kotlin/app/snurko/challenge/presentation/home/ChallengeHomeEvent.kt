package app.snurko.challenge.presentation.home

import app.snurko.challenge.presentation.sticker.DayStickerState
import app.snurko.core.presentation.UiEvent

sealed interface ChallengeHomeEvent : UiEvent {
    data object UpdateFailed : ChallengeHomeEvent
    data class ShareSticker(val sticker: DayStickerState) : ChallengeHomeEvent
}
