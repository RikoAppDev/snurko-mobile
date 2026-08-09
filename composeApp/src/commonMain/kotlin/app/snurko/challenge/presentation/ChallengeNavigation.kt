package app.snurko.challenge.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.snurko.challenge.presentation.history.HistoryRoot
import app.snurko.challenge.presentation.home.ChallengeHomeRoot
import kotlinx.serialization.Serializable

@Serializable
data object ChallengeHomeRoute

@Serializable
data object HistoryRoute

fun NavGraphBuilder.challengeGraph(
    navController: NavController,
    onOpenReminders: () -> Unit,
    onOpenUnlock: () -> Unit,
) {
    composable<ChallengeHomeRoute> {
        ChallengeHomeRoot(
            onOpenHistory = { navController.navigate(HistoryRoute) },
            onOpenReminders = onOpenReminders,
        )
    }
    composable<HistoryRoute> {
        HistoryRoot(
            onNavigateBack = { navController.popBackStack() },
            onOpenUnlock = onOpenUnlock,
        )
    }
}
