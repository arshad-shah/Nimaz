package com.arshadshah.nimaz.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.activity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.arshadshah.nimaz.activities.QuranActivity
import com.arshadshah.nimaz.ui.screens.PrayerTimesScreen
import com.arshadshah.nimaz.ui.screens.QiblaScreen
import com.arshadshah.nimaz.ui.screens.settings.PrayerTimesCustomizations
import com.arshadshah.nimaz.ui.screens.settings.SettingsScreen

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NavigationGraph(navController : NavController , paddingValues : PaddingValues)
{
	NavHost(
			navController = navController as NavHostController ,
			startDestination = BottomNavItem.PrayerTimesScreen.screen_route
		   ) {
		composable(BottomNavItem.PrayerTimesScreen.screen_route) {
			PrayerTimesScreen(paddingValues = paddingValues)
		}
		composable(BottomNavItem.QiblaScreen.screen_route) {
			QiblaScreen(paddingValues)
		}
		activity(BottomNavItem.QuranScreen.screen_route) {
			this.activityClass = QuranActivity::class
		}
		SettingGraph(navController = navController , paddingValues = paddingValues)
	}
}

fun NavGraphBuilder.SettingGraph(navController : NavController , paddingValues : PaddingValues)
{
	navigation(startDestination = "settings" , route = BottomNavItem.SettingsScreen.screen_route) {
		composable("settings") {
			SettingsScreen(onNavigateToPrayerTimeCustomizationScreen = {
				navController.navigate(
						"PrayerTimesCustomizations"
									  )
			} , paddingValues)
		}
		composable("PrayerTimesCustomizations") {
			PrayerTimesCustomizations(paddingValues)
		}
	}
}
