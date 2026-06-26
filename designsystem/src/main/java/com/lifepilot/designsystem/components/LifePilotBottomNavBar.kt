package com.lifepilot.designsystem.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy

interface BottomNavDestination {
    val route: String
    val label: String
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
}

@Composable
fun <T : BottomNavDestination> LifePilotBottomNavBar(
    destinations: List<T>,
    currentDestination: NavDestination?,
    onNavigate: (T) -> Unit,
) {
    NavigationBar {
        destinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == destination.route
            } == true

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(text = destination.label) },
            )
        }
    }
}
