package app.snurko.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.dayIndexOn
import app.snurko.core.domain.DateProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AppUiState {
    data object Loading : AppUiState
    data object Setup : AppUiState
    data class Started(val day: Int, val habitCount: Int) : AppUiState
}

class AppViewModel(
    challengeRepository: ChallengeRepository,
    dateProvider: DateProvider,
) : ViewModel() {

    val state: StateFlow<AppUiState> = challengeRepository.observeActiveChallenge()
        .map { active ->
            if (active == null) {
                AppUiState.Setup
            } else {
                AppUiState.Started(
                    day = active.dayIndexOn(dateProvider.today())
                        .coerceIn(1, active.challenge.lengthDays),
                    habitCount = active.challenge.habits.size,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
