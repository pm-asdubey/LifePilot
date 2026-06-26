package com.lifepilot.features.ai.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifepilot.features.ai.ui.AiChatScreen

fun NavGraphBuilder.aiChatScreen() {
    composable(route = "ai") {
        AiChatScreen()
    }
}
