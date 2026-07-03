package com.lifepilot.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.lifepilot.features.document.navigation.documentViewerScreen
import com.lifepilot.features.home.navigation.homeScreen
import com.lifepilot.features.library.navigation.libraryScreen
import com.lifepilot.features.objectdetail.navigation.objectDetailScreen
import com.lifepilot.features.planner.navigation.plannerScreen
import com.lifepilot.features.search.navigation.searchScreen
import com.lifepilot.features.settings.navigation.settingsScreen

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
        currentDestination?.hierarchy?.any { it.route?.substringBefore('?') == route } == true
    }

    val isOnStartDestination = currentDestination?.hierarchy?.any {
        it.route?.substringBefore('?') == HOME.route
    } == true

    BackHandler(enabled = !isOnStartDestination && showBottomBar) {
        navController.navigate(HOME.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
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
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut() },
        ) {
            homeScreen(navController = navController)
            libraryScreen(navController = navController)
            plannerScreen(navController = navController)
            settingsScreen(navController = navController)
            objectDetailScreen(navController = navController)
            documentViewerScreen(navController = navController)
            searchScreen(navController = navController)
        }
    }
}
