package com.lifepilot.features.object.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lifepilot.features.object.ui.ObjectDetailScreen

fun NavGraphBuilder.objectDetailScreen(navController: NavController) {
    composable(
        route = "object/{objectId}",
        arguments = listOf(navArgument("objectId") { type = NavType.StringType }),
    ) {
        ObjectDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDocument = { documentId ->
                navController.navigate("document/$documentId")
            },
        )
    }
}
