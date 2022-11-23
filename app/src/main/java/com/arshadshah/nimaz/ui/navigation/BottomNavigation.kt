package com.arshadshah.nimaz.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavController) {
    val bottomNavItems = listOf(
        BottomNavItem.PrayerTimesScreen,
        BottomNavItem.QiblaScreen,
        BottomNavItem.QuranScreen,
        BottomNavItem.SettingsScreen
    )
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.secondary
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        bottomNavItems.forEach { bottomNavItem ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = bottomNavItem.icon),
                        contentDescription = null
                    )
                },
                label = { Text(text = bottomNavItem.title) },
                selected = currentRoute == bottomNavItem.screen_route,
                onClick = {
                    navController.navigate(bottomNavItem.screen_route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

    }
}