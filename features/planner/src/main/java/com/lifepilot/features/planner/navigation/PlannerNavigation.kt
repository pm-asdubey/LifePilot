package com.lifepilot.features.planner.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lifepilot.features.planner.ui.PlannerScreen
import com.lifepilot.features.planner.ui.ProjectWorkspaceScreen

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
            onNavigateToProject = { projectId ->
                navController.navigate("project/$projectId")
            },
        )
    }
    composable(
        route = "project/{projectId}",
        arguments = listOf(
            navArgument("projectId") {
                type = NavType.StringType
            }
        )
    ) {
        ProjectWorkspaceScreen(
            onBack = { navController.popBackStack() },
            onNavigateToObject = { objectId ->
                navController.navigate("object/$objectId")
            },
        )
    }
}
