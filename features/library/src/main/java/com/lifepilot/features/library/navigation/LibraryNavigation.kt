package com.lifepilot.features.library.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifepilot.features.library.ui.LibraryScreen
import com.lifepilot.features.objectdetail.create.ui.CreateObjectSheet

fun NavGraphBuilder.libraryScreen(navController: NavController) {
    composable(route = "library") {
        var showCreateSheet by remember { mutableStateOf(false) }

        LibraryScreen(
            onNavigateToObject = { objectId ->
                navController.navigate("object/$objectId")
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
