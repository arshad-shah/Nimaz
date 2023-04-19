package com.arshadshah.nimaz.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.AnimatableIcon
import com.arshadshah.nimaz.ui.components.common.AnimatedText
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.viewModel.SettingsViewModel

@OptIn(ExperimentalAnimationApi::class , ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(navController : NavController)
{
	val context = LocalContext.current
	val viewModelSettings = viewModel(
			key = AppConstants.SETTINGS_VIEWMODEL_KEY ,
			initializer = { SettingsViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity
									 )

	val updateAvailabile = remember {
		viewModelSettings.isUpdateAvailable
	}.collectAsState()

	LaunchedEffect(Unit) {
		viewModelSettings.handleEvent(SettingsViewModel.SettingsEvent.CheckUpdate(context , false))
	}

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
			val selected =
				navBackStackEntry?.destination?.hierarchy?.any { it.route == bottomNavItem.screen_route } == true
			NavigationBarItem(
					modifier = Modifier
						.clickable(
								enabled = true ,
								role = Role.Tab ,
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
						.semantics {
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
						BadgedBox(badge = {
							if (bottomNavItem == BottomNavItem.SettingsScreen && updateAvailabile.value)
							{
								Badge(
										containerColor = MaterialTheme.colorScheme.primary ,
										contentColor = MaterialTheme.colorScheme.onPrimary ,
									 )
								{
									Text(
											text = "1" ,
											style = MaterialTheme.typography.bodySmall ,
											textAlign = TextAlign.Center ,
										)
								}
							}
						}) {
							AnimatableIcon(
									modifier = Modifier
										.size(24.dp) ,
									painter = if (selected) painterResource(id = bottomNavItem.icon) else painterResource(
											id = bottomNavItem.icon_empty
																														 ) ,
									scale = if (selected) 1.3f else 1f ,
									color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.secondary ,
									onClick = {
										if (currentRoute == BottomNavItem.PrayerTimesScreen.screen_route && bottomNavItem == BottomNavItem.Dashboard)
										{
											navController.popBackStack()
										} else
										{
											navController.navigate(bottomNavItem.screen_route) {
												popUpTo(navController.graph.startDestinationId) {
													saveState = true
												}
												launchSingleTop = true
												restoreState = true
											}
										}
									}
										  )
						}
					} ,
					label = {
						AnimatedText(
								text = bottomNavItem.title ,
								scale = if (selected) 1.3f else 1f ,
								modifier = Modifier
									.semantics {
										contentDescription = bottomNavItem.title
									} ,
								color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.secondary ,
									) {
							if (currentRoute == BottomNavItem.PrayerTimesScreen.screen_route && bottomNavItem == BottomNavItem.Dashboard)
							{
								navController.popBackStack()
							} else
							{
								navController.navigate(bottomNavItem.screen_route) {
									popUpTo(navController.graph.startDestinationId) {
										saveState = true
									}
									launchSingleTop = true
									restoreState = true
								}
							}
						}
					} ,
					selected = currentRoute == bottomNavItem.screen_route ,
					onClick = {
						if (currentRoute == BottomNavItem.PrayerTimesScreen.screen_route && bottomNavItem == BottomNavItem.Dashboard)
						{
							navController.popBackStack()
						} else
						{
							navController.navigate(bottomNavItem.screen_route) {
								popUpTo(navController.graph.startDestinationId) {
									saveState = true
								}
								launchSingleTop = true
								restoreState = true
							}
						}
					}
							 )
		}

	}
}


@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview()
{
	val navController = rememberNavController()
	NimazTheme(
			darkTheme = true
			  ) {
		BottomNavigationBar(navController = navController)
	}
}