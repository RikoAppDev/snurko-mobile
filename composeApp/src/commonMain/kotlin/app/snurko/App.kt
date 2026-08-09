package app.snurko

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.snurko.challenge.presentation.ChallengeHomeRoute
import app.snurko.challenge.presentation.challengeGraph
import app.snurko.challenge.presentation.setup.ChallengeSetupRoot
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.designsystem.SnurkoTheme
import app.snurko.presentation.AppUiState
import app.snurko.presentation.AppViewModel
import app.snurko.reminders.presentation.RemindersRoute
import app.snurko.reminders.presentation.remindersGraph
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
        AppUiState.Started -> ChallengeNavHost()
    }
}

@Composable
private fun ChallengeNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ChallengeHomeRoute,
        modifier = Modifier.fillMaxSize().background(SnurkoColors.paper),
    ) {
        challengeGraph(
            navController = navController,
            onOpenReminders = { navController.navigate(RemindersRoute) },
        )
        remindersGraph(navController)
    }
}
