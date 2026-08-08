package app.snurko

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snurko.challenge.presentation.home.ChallengeHomeRoot
import app.snurko.challenge.presentation.setup.ChallengeSetupRoot
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.designsystem.SnurkoTheme
import app.snurko.presentation.AppUiState
import app.snurko.presentation.AppViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    SnurkoTheme {
        AppRoot()
    }
}

@Composable
private fun AppRoot(viewModel: AppViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state) {
        AppUiState.Loading -> Box(Modifier.fillMaxSize().background(SnurkoColors.paper))
        AppUiState.Setup -> ChallengeSetupRoot()
        AppUiState.Started -> ChallengeHomeRoot()
    }
}
