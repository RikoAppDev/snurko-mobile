package app.snurko

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.snurko.billing.presentation.UnlockRoute
import app.snurko.billing.presentation.billingGraph
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
fun App(
    isUnlockRequested: Boolean = false,
    onUnlockOpened: () -> Unit = {},
) {
    SnurkoTheme {
        AppRoot(isUnlockRequested = isUnlockRequested, onUnlockOpened = onUnlockOpened)
    }
}

@Composable
private fun AppRoot(
    isUnlockRequested: Boolean,
    onUnlockOpened: () -> Unit,
    viewModel: AppViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state) {
        AppUiState.Loading -> Box(Modifier.fillMaxSize().background(SnurkoColors.paper))
        AppUiState.Setup -> ChallengeSetupRoot()
        AppUiState.Started -> ChallengeNavHost(
            isUnlockRequested = isUnlockRequested,
            onUnlockOpened = onUnlockOpened,
        )
    }
}

@Composable
private fun ChallengeNavHost(isUnlockRequested: Boolean, onUnlockOpened: () -> Unit) {
    val navController = rememberNavController()

    LaunchedEffect(isUnlockRequested) {
        if (isUnlockRequested) {
            navController.navigate(UnlockRoute)
            onUnlockOpened()
        }
    }

    NavHost(
        navController = navController,
        startDestination = ChallengeHomeRoute,
        modifier = Modifier.fillMaxSize().background(SnurkoColors.paper),
    ) {
        challengeGraph(
            navController = navController,
            onOpenReminders = { navController.navigate(RemindersRoute) },
            onOpenUnlock = { navController.navigate(UnlockRoute) },
        )
        remindersGraph(navController)
        billingGraph(navController)
    }
}
