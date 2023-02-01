package com.arshadshah.nimaz.ui.navigation

import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arshadshah.nimaz.activities.*
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.ui.screens.MoreScreen
import com.arshadshah.nimaz.ui.screens.PrayerTimesScreen
import com.arshadshah.nimaz.ui.screens.QiblaScreen

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NavigationGraph(
	navController : NavController ,
	paddingValues : PaddingValues ,
				   )
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
		composable(BottomNavItem.MoreScreen.screen_route) {
			MoreScreen(paddingValues,
					   onNavigateToTasbihScreen = { arabic : String ->
						   navController.navigate("tasbih/$arabic")
					   },
					   onNavigateToNames = {
						   navController.navigate("names")
					   },
					   onNavigateToListOfTasbeeh = {
						   navController.navigate("listoftasbeeh")
					   },
					   onNavigateToShadah = {
						   navController.navigate("shahadah")
					   },
					  )
		}

		activity("tasbih/{arabic}") {
			//pass the arabic string to the tasbih activity
			this.activityClass = Tasbih::class
		}

		activity("names") {
			this.activityClass = NamesOfAllah::class
		}

		activity("listoftasbeeh") {
			this.activityClass = ListOfTasbeeh::class
		}

		activity("shahadah") {
			this.activityClass = ShahadahActivity::class
		}

		activity(BottomNavItem.SettingsScreen.screen_route) {
			this.activityClass = SettingsActivity::class
		}
	}
}