package com.arshadshah.nimaz.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.activity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arshadshah.nimaz.activities.QuranActivity
import com.arshadshah.nimaz.activities.SettingsActivity
import com.arshadshah.nimaz.ui.screens.PrayerTimesScreen
import com.arshadshah.nimaz.ui.screens.QiblaScreen
import com.arshadshah.nimaz.ui.screens.TodayScreen

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NavigationGraph(navController : NavController , paddingValues : PaddingValues)
{
	NavHost(
			navController = navController as NavHostController ,
			startDestination = BottomNavItem.TodayScreen.screen_route
		   ) {
		composable(BottomNavItem.TodayScreen.screen_route) {
			TodayScreen(paddingValues = paddingValues)
		}
		composable(BottomNavItem.PrayerTimesScreen.screen_route) {
			PrayerTimesScreen(paddingValues = paddingValues)
		}
		composable(BottomNavItem.QiblaScreen.screen_route) {
			QiblaScreen(paddingValues)
		}
		activity(BottomNavItem.QuranScreen.screen_route) {
			this.activityClass = QuranActivity::class
		}
		activity(BottomNavItem.SettingsScreen.screen_route) {
			this.activityClass = SettingsActivity::class
		}
	}
}