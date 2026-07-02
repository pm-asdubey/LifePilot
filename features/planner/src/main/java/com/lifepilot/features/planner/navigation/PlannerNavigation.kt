package com.lifepilot.features.planner.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lifepilot.features.planner.ui.PlannerScreen

fun NavGraphBuilder.plannerScreen(navController: NavController) {
    composable(
        route = "planner?taskId={taskId}",
        arguments = listOf(
            navArgument("taskId") {
                type = NavType.StringType
                defaultValue = ""
            }
        )
    ) {
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
