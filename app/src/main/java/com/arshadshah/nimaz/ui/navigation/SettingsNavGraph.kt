package com.arshadshah.nimaz.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arshadshah.nimaz.ui.screens.settings.About
import com.arshadshah.nimaz.ui.screens.settings.PrayerTimesCustomizations
import com.arshadshah.nimaz.ui.screens.settings.SettingsScreen

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun SettingsNavGraph(navController : NavController , paddingValues : PaddingValues)
{
	NavHost(
			navController = navController as NavHostController ,
			startDestination = "settings"
		   ) {
		composable("settings") {
			SettingsScreen(
					onNavigateToPrayerTimeCustomizationScreen = {
						navController.navigate(
								"PrayerTimesCustomizations"
											  )
					} ,
					onNavigateToAboutScreen = {
						navController.navigate(
								"about"
											  )
					}
					,
					paddingValues = paddingValues)
		}
		composable("about") {
			About(paddingValues)
		}
		composable("PrayerTimesCustomizations") {
			PrayerTimesCustomizations(paddingValues)
		}
	}
}