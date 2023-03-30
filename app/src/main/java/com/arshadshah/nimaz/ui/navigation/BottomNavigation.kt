package com.arshadshah.nimaz.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.theme.NimazTheme

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
							Crossfade(
									targetState = selected ,
									animationSpec = tween(durationMillis = 100)
									 ) { targetState ->
								Icon(
										painter = painterResource(id = if (targetState) bottomNavItem.icon else bottomNavItem.icon_empty) ,
										contentDescription = bottomNavItem.iconDescription ,
										modifier = Modifier
											.size(24.dp)
									)
							}
						}
					} ,
					label = {
						Text(
								text = bottomNavItem.title ,
								modifier = Modifier
									.semantics {
										contentDescription = bottomNavItem.title
									}
							)
					} ,
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