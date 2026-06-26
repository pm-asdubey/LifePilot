package com.lifepilot.features.search.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lifepilot.features.search.ui.SearchScreen

fun NavGraphBuilder.searchScreen(navController: NavController) {
    composable(route = "search") {
        SearchScreen(
            onNavigateToObject = { objectId ->
                navController.navigate("object/$objectId")
            },
        )
    }
}
