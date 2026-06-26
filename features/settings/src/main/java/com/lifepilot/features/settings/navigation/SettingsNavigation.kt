package com.lifepilot.features.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifepilot.features.settings.ui.SettingsScreen

fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable(route = "settings") {
        SettingsScreen()
    }
}
