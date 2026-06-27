package com.lifepilot.features.home.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifepilot.features.home.ui.HomeScreen
import com.lifepilot.features.object.create.ui.CreateObjectSheet

fun NavGraphBuilder.homeScreen(navController: NavController) {
    composable(route = "home") {
        var showCreateSheet by remember { mutableStateOf(false) }

        HomeScreen(
            onNavigateToObject = { objectId ->
                navController.navigate("object/$objectId")
            },
            onNavigateToLibrary = {
                navController.navigate("library")
            },
            onNavigateToTimeline = {
                navController.navigate("timeline")
            },
            onAddObject = {
                showCreateSheet = true
            },
        )

        if (showCreateSheet) {
            CreateObjectSheet(
                onDismiss = { showCreateSheet = false },
                onObjectCreated = { objectId ->
                    showCreateSheet = false
                    navController.navigate("object/$objectId")
                },
            )
        }
    }
}
