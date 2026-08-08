package app.snurko.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snurko.challenge.domain.ChallengeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AppUiState {
    data object Loading : AppUiState
    data object Setup : AppUiState
    data object Started : AppUiState
}

class AppViewModel(
    challengeRepository: ChallengeRepository,
) : ViewModel() {

    val state: StateFlow<AppUiState> = challengeRepository.observeActiveChallenge()
        .map { active -> if (active == null) AppUiState.Setup else AppUiState.Started }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
