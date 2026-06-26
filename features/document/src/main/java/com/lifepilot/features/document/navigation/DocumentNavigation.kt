package com.lifepilot.features.document.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lifepilot.features.document.ui.DocumentViewerScreen

fun NavGraphBuilder.documentViewerScreen(navController: NavController) {
    composable(
        route = "document/{documentId}",
        arguments = listOf(navArgument("documentId") { type = NavType.StringType }),
    ) {
        DocumentViewerScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
