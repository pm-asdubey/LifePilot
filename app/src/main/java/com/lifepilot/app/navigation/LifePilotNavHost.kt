package com.lifepilot.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifepilot.app.navigation.TopLevelDestination.HOME
import com.lifepilot.designsystem.components.LifePilotBottomNavBar
import com.lifepilot.features.ai.navigation.aiChatScreen
import com.lifepilot.features.document.navigation.documentViewerScreen
import com.lifepilot.features.home.navigation.homeScreen
import com.lifepilot.features.library.navigation.libraryScreen
import com.lifepilot.features.object.navigation.objectDetailScreen
import com.lifepilot.features.search.navigation.searchScreen
import com.lifepilot.features.settings.navigation.settingsScreen
import com.lifepilot.features.timeline.navigation.timelineScreen

@Composable
fun LifePilotNavHost(deepLinkObjectId: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(deepLinkObjectId) {
        if (!deepLinkObjectId.isNullOrBlank()) {
            navController.navigate("object/$deepLinkObjectId")
        }
    }

    val topLevelRoutes = TopLevelDestination.entries.map { it.route }
    val showBottomBar = topLevelRoutes.any { route ->
        currentDestination?.hierarchy?.any { it.route == route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                LifePilotBottomNavBar(
                    destinations = TopLevelDestination.entries,
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            homeScreen(navController = navController)
            libraryScreen(navController = navController)
            searchScreen(navController = navController)
            aiChatScreen()
            settingsScreen(navController = navController)
            objectDetailScreen(navController = navController)
            documentViewerScreen(navController = navController)
            timelineScreen(navController = navController)
        }
    }
}
