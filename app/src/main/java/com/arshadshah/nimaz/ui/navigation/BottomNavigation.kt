package com.arshadshah.nimaz.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController : NavController)
{
	val bottomNavItems = listOf(
			BottomNavItem.Dashboard ,
			BottomNavItem.PrayerTimesScreen ,
			BottomNavItem.QuranScreen ,
			BottomNavItem.MoreScreen ,
			BottomNavItem.SettingsScreen
							   )
	NavigationBar(
			containerColor = MaterialTheme.colorScheme.surface ,
			contentColor = MaterialTheme.colorScheme.secondary ,
			modifier = Modifier.semantics {
				contentDescription = "Bottom Navigation Bar"
			}
				 ) {
		val navBackStackEntry by navController.currentBackStackEntryAsState()
		val currentRoute = navBackStackEntry?.destination?.route
		bottomNavItems.forEach { bottomNavItem ->
			NavigationBarItem(
					modifier = Modifier.semantics {
						contentDescription = bottomNavItem.title
					} ,
					colors = NavigationBarItemDefaults.colors(
							selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer ,
							selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer ,
							unselectedIconColor = MaterialTheme.colorScheme.secondary ,
							unselectedTextColor = MaterialTheme.colorScheme.secondary ,
							indicatorColor = MaterialTheme.colorScheme.secondaryContainer
															 ) ,
					icon = {
						Icon(
								modifier = Modifier.size(28.dp) ,
								painter = painterResource(id = bottomNavItem.icon) ,
								contentDescription = bottomNavItem.iconDescription
							)
					} ,
					alwaysShowLabel = true ,
					label = { Text(text = bottomNavItem.title) } ,
					selected = currentRoute == bottomNavItem.screen_route ,
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