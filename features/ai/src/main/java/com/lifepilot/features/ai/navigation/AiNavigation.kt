package com.lifepilot.features.ai.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifepilot.features.ai.ui.AiChatScreen

fun NavGraphBuilder.aiChatScreen(navController: NavController) {
    composable(route = "ai") {
        AiChatScreen(
            onNavigateToSettings = { navController.navigate("settings") },
        )
    }
}
