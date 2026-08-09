package app.snurko.billing.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TipJarRoute

fun NavGraphBuilder.billingGraph(navController: NavController) {
    composable<TipJarRoute> {
        TipJarRoot(onNavigateBack = { navController.popBackStack() })
    }
}
