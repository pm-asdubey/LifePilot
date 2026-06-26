package com.lifepilot.features.library.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifepilot.features.library.ui.LibraryScreen

fun NavGraphBuilder.libraryScreen(navController: NavController) {
    composable(route = "library") {
        LibraryScreen(
            onNavigateToObject = { objectId ->
                navController.navigate("object/$objectId")
            },
            onAddObject = {
                // Navigate to add object flow
            },
        )
    }
}
