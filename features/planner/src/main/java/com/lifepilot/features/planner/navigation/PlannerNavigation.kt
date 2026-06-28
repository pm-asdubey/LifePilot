package com.lifepilot.features.planner.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifepilot.features.planner.ui.PlannerScreen

fun NavGraphBuilder.plannerScreen(navController: NavController) {
    composable(route = "planner") {
        PlannerScreen(
            onNavigateToObject = { objectId ->
                navController.navigate("object/$objectId")
            },
            onNavigateToSearch = {
                navController.navigate("search")
            },
        )
    }
}
