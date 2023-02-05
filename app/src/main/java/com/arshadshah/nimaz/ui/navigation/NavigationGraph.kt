package com.arshadshah.nimaz.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arshadshah.nimaz.activities.*
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.CHAPTERS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QURAN_AYA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.SHAHADAH_SCREEN_ROUTE
import com.arshadshah.nimaz.ui.screens.MoreScreen
import com.arshadshah.nimaz.ui.screens.PrayerTimesScreen
import com.arshadshah.nimaz.ui.screens.QiblaScreen
import com.arshadshah.nimaz.ui.screens.ShahadahScreen
import com.arshadshah.nimaz.ui.screens.quran.AyatScreen
import com.arshadshah.nimaz.ui.screens.quran.QuranScreen
import com.arshadshah.nimaz.ui.screens.settings.About
import com.arshadshah.nimaz.ui.screens.settings.PrayerTimesCustomizations
import com.arshadshah.nimaz.ui.screens.settings.SettingsScreen
import com.arshadshah.nimaz.ui.screens.tasbih.ChapterList
import com.arshadshah.nimaz.ui.screens.tasbih.DuaList

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
		composable(BottomNavItem.QuranScreen.screen_route) {
			QuranScreen(
					paddingValues ,
					onNavigateToAyatScreen = { number : String , isSurah : Boolean , language : String ->
						//replace the placeholder with the actual route
						navController.navigate(
								QURAN_AYA_SCREEN_ROUTE.replace(
										"{number}" ,
										number
															  )
										.replace(
												"{isSurah}" ,
												isSurah.toString()
												)
										.replace(
												"{language}" ,
												language
												)
											  )
					})
		}
		composable(QURAN_AYA_SCREEN_ROUTE) {
			AyatScreen(
					number = it.arguments?.getString("number") ,
					isSurah = it.arguments?.getString("isSurah") !! ,
					language = it.arguments?.getString("language") !! ,
					paddingValues = paddingValues
					  )
		}


		composable(BottomNavItem.MoreScreen.screen_route) {
			MoreScreen(
					paddingValues ,
					onNavigateToTasbihScreen = { arabic : String ->
						navController.navigate("tasbih/$arabic")
					} ,
					onNavigateToNames = {
						navController.navigate("names")
					} ,
					onNavigateToListOfTasbeeh = {
						navController.navigate(CHAPTERS_SCREEN_ROUTE)
					} ,
					onNavigateToShadah = {
						navController.navigate(SHAHADAH_SCREEN_ROUTE)
					} ,
					onNavigateToZakat = {
						navController.navigate("Zakat")
					} ,
					  )
		}

		activity("tasbih/{arabic}") {
			//pass the arabic string to the tasbih activity
			this.activityClass = Tasbih::class
		}

		activity("names") {
			this.activityClass = NamesOfAllah::class
		}

		composable(CHAPTERS_SCREEN_ROUTE) {
			ChapterList(
					paddingValues ,
					onNavigateToChapter = { chapterId : Int ->
						//replace CHAPTER_SCREEN_ROUTE with the actual route and pass the chapterId
						navController.navigate(
								CHAPTER_SCREEN_ROUTE.replace(
										"{chapterId}" ,
										chapterId.toString()
															  )
											  )
					}
					   )
		}
		composable(CHAPTER_SCREEN_ROUTE) {
			DuaList(
					chapterId = it.arguments?.getString("chapterId")?.toInt() ?: 0 ,
					paddingValues = paddingValues
				   )
		}

		activity("Zakat") {
			this.activityClass = ZakatCalculator::class
		}

		composable(SHAHADAH_SCREEN_ROUTE) {
			ShahadahScreen(paddingValues)
		}

		composable(BottomNavItem.SettingsScreen.screen_route) {
			SettingsScreen(
					onNavigateToPrayerTimeCustomizationScreen = {
						navController.navigate(
								AppConstants.PRAYER_TIMES_SETTINGS_SCREEN_ROUTE
											  )
					} ,
					onNavigateToAboutScreen = {
						navController.navigate(
								AppConstants.ABOUT_SCREEN_ROUTE
											  )
					} ,
					paddingValues = paddingValues)
		}
		composable(AppConstants.ABOUT_SCREEN_ROUTE) {
			About(paddingValues)
		}
		composable(AppConstants.PRAYER_TIMES_SETTINGS_SCREEN_ROUTE) {
			PrayerTimesCustomizations(paddingValues)
		}
	}
}