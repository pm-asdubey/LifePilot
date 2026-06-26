package com.lifepilot.features.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifepilot.features.home.ui.HomeScreen

fun NavGraphBuilder.homeScreen(navController: NavController) {
    composable(route = "home") {
        HomeScreen(
            onNavigateToObject = { objectId ->
                navController.navigate("object/$objectId")
            },
            onNavigateToLibrary = {
                navController.navigate("library")
            },
            onAddObject = {
                navController.navigate("library")
            },
        )
    }
}
