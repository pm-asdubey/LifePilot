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
        // Domain the create sheet is scoped to (set when opened from a domain section's "+").
        var createSheetDomain by remember { mutableStateOf<String?>(null) }

        LibraryScreen(
            onNavigateToObject = { objectId ->
                navController.navigate("object/$objectId")
            },
            onAddObject = {
                createSheetDomain = null
                showCreateSheet = true
            },
            onNavigateToSearch = {
                navController.navigate("search")
            },
            onAddObjectToDomain = { domain ->
                createSheetDomain = domain
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
                initialDomain = createSheetDomain,
            )
        }
    }
    // NOTE: The "project/{projectId}" route is owned solely by Planner (ProjectWorkspaceScreen)
    // per ADR-002 — Library is Objects-only. The former Library ProjectDetail route was removed
    // because it collided with Planner's identical route and shadowed it as dead code.
}
