package com.lifepilot.features.search.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifepilot.features.search.ui.SearchScreen

fun NavGraphBuilder.searchScreen(navController: NavController) {
    composable(route = "search") {
        SearchScreen(
            onNavigateToObject = { objectId ->
                navController.navigate("object/$objectId")
            },
            onNavigateToTask = { taskId ->
                navController.navigate("planner?taskId=$taskId") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onNavigateToConversation = { conversationId ->
                runCatching {
                    navController.getBackStackEntry("home")
                        .savedStateHandle["resumeConversationId"] = conversationId
                }
                navController.navigate("home") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }
}
