package app.snurko.reminders.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object RemindersRoute

fun NavGraphBuilder.remindersGraph(navController: NavController) {
    composable<RemindersRoute> {
        RemindersRoot(onNavigateBack = { navController.popBackStack() })
    }
}
