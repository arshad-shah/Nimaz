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
import com.arshadshah.nimaz.activities.NamesOfAllah
import com.arshadshah.nimaz.activities.QuranActivity
import com.arshadshah.nimaz.activities.SettingsActivity
import com.arshadshah.nimaz.activities.Tasbih
import com.arshadshah.nimaz.ui.screens.PrayerTimesScreen
import com.arshadshah.nimaz.ui.screens.QiblaScreen

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NavigationGraph(navController : NavController , paddingValues : PaddingValues)
{
	NavHost(
			navController = navController as NavHostController ,
			startDestination = BottomNavItem.PrayerTimesScreen.screen_route
		   ) {
		composable(BottomNavItem.PrayerTimesScreen.screen_route) {
			PrayerTimesScreen(
					paddingValues = paddingValues ,
					onNavigateToTasbihScreen = { arabic : String ->
						navController.navigate("tasbih/$arabic")
					} ,
					onNavigateToNames = {
						navController.navigate("names")
					}
							 )
		}

		activity("tasbih/{arabic}") {
			//pass the arabic string to the tasbih activity
			this.activityClass = Tasbih::class
		}

		activity("names") {
			this.activityClass = NamesOfAllah::class
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