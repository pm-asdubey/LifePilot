package com.lifepilot.features.timeline.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifepilot.features.timeline.ui.TimelineScreen

fun NavGraphBuilder.timelineScreen(navController: NavController) {
    composable(route = "timeline") {
        TimelineScreen(
            onNavigateToObject = { objectId ->
                navController.navigate("object/$objectId")
            },
        )
    }
}
