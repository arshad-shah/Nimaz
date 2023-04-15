package com.arshadshah.nimaz.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.activities.*
import com.arshadshah.nimaz.constants.AppConstants.ABOUT_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CALENDER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTERS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.LICENCES_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.MY_QURAN_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.NAMESOFALLAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TIMES_SETTINGS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TRACKER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QIBLA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QURAN_AYA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.SCREEN_ANIMATION_DURATION
import com.arshadshah.nimaz.constants.AppConstants.SCREEN_ANIMATION_DURATION_Exit
import com.arshadshah.nimaz.constants.AppConstants.SHAHADAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_LIST_SCREEN
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.WEB_VIEW_SCREEN_ROUTE
import com.arshadshah.nimaz.ui.screens.*
import com.arshadshah.nimaz.ui.screens.more.MoreScreen
import com.arshadshah.nimaz.ui.screens.more.NamesOfAllah
import com.arshadshah.nimaz.ui.screens.more.QiblaScreen
import com.arshadshah.nimaz.ui.screens.more.ShahadahScreen
import com.arshadshah.nimaz.ui.screens.quran.AyatScreen
import com.arshadshah.nimaz.ui.screens.quran.QuranScreen
import com.arshadshah.nimaz.ui.screens.settings.*
import com.arshadshah.nimaz.ui.screens.tasbih.ChapterList
import com.arshadshah.nimaz.ui.screens.tasbih.DuaList
import com.arshadshah.nimaz.ui.screens.tasbih.ListOfTasbih
import com.arshadshah.nimaz.ui.screens.tasbih.TasbihScreen
import com.arshadshah.nimaz.ui.screens.tracker.Calender
import com.arshadshah.nimaz.ui.screens.tracker.PrayerTracker
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable

@OptIn(ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NavigationGraph(
	navController : NavController ,
	paddingValues : PaddingValues ,
				   )
{

	AnimatedNavHost(
			navController = navController as NavHostController ,
			startDestination = BottomNavItem.Dashboard.screen_route ,
			enterTransition = {
				when (initialState.destination.route)
				{
					BottomNavItem.PrayerTimesScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					QIBLA_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					BottomNavItem.QuranScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					BottomNavItem.MoreScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					BottomNavItem.SettingsScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					PRAYER_TIMES_SETTINGS_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					ABOUT_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					QURAN_AYA_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					CHAPTER_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					CHAPTERS_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					TASBIH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					NAMESOFALLAH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					SHAHADAH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					PRAYER_TRACKER_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					CALENDER_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					BottomNavItem.Dashboard.screen_route ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					TASBIH_LIST_SCREEN ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					MY_QURAN_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					WEB_VIEW_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					else -> EnterTransition.None
				}
			} ,
			exitTransition = {
				when (targetState.destination.route)
				{
					BottomNavItem.PrayerTimesScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					QIBLA_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					BottomNavItem.QuranScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					BottomNavItem.MoreScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					BottomNavItem.SettingsScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					PRAYER_TIMES_SETTINGS_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					ABOUT_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					CHAPTERS_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					TASBIH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					NAMESOFALLAH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					SHAHADAH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					PRAYER_TRACKER_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					CALENDER_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					BottomNavItem.Dashboard.screen_route ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					TASBIH_LIST_SCREEN ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					MY_QURAN_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					WEB_VIEW_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					LICENCES_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					else -> ExitTransition.None
				}
			} ,
			popEnterTransition = {
				when (initialState.destination.route)
				{
					BottomNavItem.PrayerTimesScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					QIBLA_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					BottomNavItem.QuranScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					BottomNavItem.MoreScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					BottomNavItem.SettingsScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					PRAYER_TIMES_SETTINGS_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					ABOUT_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					QURAN_AYA_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					CHAPTER_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					CHAPTERS_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					TASBIH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					NAMESOFALLAH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					SHAHADAH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					PRAYER_TRACKER_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					CALENDER_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					BottomNavItem.Dashboard.screen_route ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					TASBIH_LIST_SCREEN ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					MY_QURAN_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					WEB_VIEW_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					LICENCES_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentTransitionScope.SlideDirection.Left ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION
													 )
										  )
					else -> EnterTransition.None
				}
			} ,
			popExitTransition = {
				when (targetState.destination.route)
				{
					BottomNavItem.PrayerTimesScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					QIBLA_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					BottomNavItem.QuranScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					BottomNavItem.MoreScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					BottomNavItem.SettingsScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					PRAYER_TIMES_SETTINGS_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					ABOUT_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					QURAN_AYA_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					CHAPTER_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					CHAPTERS_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					TASBIH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					NAMESOFALLAH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					SHAHADAH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					PRAYER_TRACKER_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					CALENDER_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					BottomNavItem.Dashboard.screen_route ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					TASBIH_LIST_SCREEN ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					MY_QURAN_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					WEB_VIEW_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )
					LICENCES_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentTransitionScope.SlideDirection.Right ,
								animationSpec = tween(
										easing = LinearOutSlowInEasing ,
										durationMillis = SCREEN_ANIMATION_DURATION_Exit
													 )
										   )

					else -> ExitTransition.None
				}
			}
				   ) {

		composable(BottomNavItem.Dashboard.screen_route) {
			Dashboard(
					paddingValues = paddingValues ,
					onNavigateToPrayerTimes = {
						navController.navigate(BottomNavItem.PrayerTimesScreen.screen_route)
					} ,
					onNavigateToCalender = {
						navController.navigate(CALENDER_SCREEN_ROUTE)
					} ,
					onNavigateToTracker = {
						navController.navigate(PRAYER_TRACKER_SCREEN_ROUTE)
					} ,
					onNavigateToAyatScreen = { number : String , isSurah : Boolean , language : String , scrollToAya : Int ->
						navController.navigate(
								MY_QURAN_SCREEN_ROUTE.replace(
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
									.replace(
											"{scrollTo}" ,
											scrollToAya.toString()
											)
											  )
					} ,
					onNavigateToTasbihScreen = { id : String , arabic : String , translation : String , transliteration : String ->
						navController.navigate(
								TASBIH_SCREEN_ROUTE
									.replace(
											"{id}" ,
											id
											)
									.replace(
											"{arabic}" ,
											arabic
											)
									.replace(
											"{translation}" ,
											translation
											)
									.replace(
											"{transliteration}" ,
											transliteration
											)
											  )
					} ,
					onNavigateToTasbihListScreen = {
						navController.navigate(TASBIH_LIST_SCREEN)
					} ,
					 )

		}

		composable(BottomNavItem.PrayerTimesScreen.screen_route)
		{
			PrayerTimesScreen(
					paddingValues = paddingValues
							 ) {
				navController.navigate(CALENDER_SCREEN_ROUTE)
			}
		}

		composable(CALENDER_SCREEN_ROUTE) {
			Calender(paddingValues)
		}

		composable(QIBLA_SCREEN_ROUTE) {
			QiblaScreen(paddingValues)
		}
		composable(BottomNavItem.QuranScreen.screen_route) {
			QuranScreen(
					paddingValues
					   ) { number : String , isSurah : Boolean , language : String , scrollToAya : Int? ->
				if (scrollToAya != null)
				{
					navController.navigate(
							MY_QURAN_SCREEN_ROUTE.replace(
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
								.replace(
										"{scrollTo}" ,
										scrollToAya.toString()
										)
										  ) {
						popUpTo(MY_QURAN_SCREEN_ROUTE) {
							inclusive = true
						}
						launchSingleTop = true
					}
				} else
				{
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
				}
			}
		}
		composable(MY_QURAN_SCREEN_ROUTE) {
			AyatScreen(
					number = it.arguments?.getString("number") !! ,
					isSurah = it.arguments?.getString("isSurah") !! ,
					language = it.arguments?.getString("language") !! ,
					scrollToAya = it.arguments?.getString("scrollTo") !!.toInt() ,
					paddingValues = paddingValues ,
					  )
		}

		composable(QURAN_AYA_SCREEN_ROUTE) {
			AyatScreen(
					number = it.arguments?.getString("number") !! ,
					isSurah = it.arguments?.getString("isSurah") !! ,
					language = it.arguments?.getString("language") !! ,
					paddingValues = paddingValues ,
					  )
		}


		composable(BottomNavItem.MoreScreen.screen_route) {
			MoreScreen(
					paddingValues ,
					onNavigateToTasbihScreen = { id : String , arabic : String , translation : String , transliteration : String ->
						navController.navigate(
								TASBIH_SCREEN_ROUTE
									.replace(
											"{id}" ,
											id
											)
									.replace(
											"{arabic}" ,
											arabic
											)
									.replace(
											"{translation}" ,
											translation
											)
									.replace(
											"{transliteration}" ,
											transliteration
											)
											  )
					} ,
					onNavigateToTasbihListScreen = {
						navController.navigate(TASBIH_LIST_SCREEN)
					} ,
					onNavigateToNames = {
						navController.navigate(NAMESOFALLAH_SCREEN_ROUTE)
					} ,
					onNavigateToListOfTasbeeh = {
						navController.navigate(CHAPTERS_SCREEN_ROUTE)
					} ,
					onNavigateToQibla = {
						navController.navigate(QIBLA_SCREEN_ROUTE)
					} ,
					onNavigateToShadah = {
						navController.navigate(SHAHADAH_SCREEN_ROUTE)
					} ,
					onNavigateToPrayerTracker = {
						navController.navigate(PRAYER_TRACKER_SCREEN_ROUTE)
					} ,
					onNavigateToCalender = {
						navController.navigate(CALENDER_SCREEN_ROUTE)
					} ,
					onNavigateToZakat = {
						navController.navigate("Zakat")
					} ,
					  )
		}

		composable(TASBIH_LIST_SCREEN) {
			ListOfTasbih(paddingValues) { id : String , arabic : String , translation : String , transliteration : String ->
				//replace the placeholder with the actual route TASBIH_SCREEN_ROUTE
				//tasbih_screen/{arabic}/{translation}/{transliteration}
				navController.navigate(
						TASBIH_SCREEN_ROUTE
							.replace(
									"{id}" ,
									id
									)
							.replace(
									"{arabic}" ,
									arabic
									)
							.replace(
									"{translation}" ,
									translation
									)
							.replace(
									"{transliteration}" ,
									transliteration
									)
									  )
			}
		}

		composable(PRAYER_TRACKER_SCREEN_ROUTE) {
			PrayerTracker(paddingValues)
		}

		composable(TASBIH_SCREEN_ROUTE) {
			TasbihScreen(
					tasbihId = it.arguments?.getString("id") !! ,
					tasbihArabic = it.arguments?.getString("arabic") !! ,
					tasbihEnglish = it.arguments?.getString("translation") !! ,
					tasbihTranslitration = it.arguments?.getString("transliteration") !! ,
					paddingValues = paddingValues ,
						)
		}

		composable(NAMESOFALLAH_SCREEN_ROUTE) {
			NamesOfAllah(paddingValues = paddingValues)
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

		composable(SHAHADAH_SCREEN_ROUTE) {
			ShahadahScreen(paddingValues)
		}

		composable(BottomNavItem.SettingsScreen.screen_route) {
			SettingsScreen(
					onNavigateToPrayerTimeCustomizationScreen = {
						navController.navigate(
								PRAYER_TIMES_SETTINGS_SCREEN_ROUTE
											  )
					} ,
					onNavigateToLicencesScreen = {
						navController.navigate(
								LICENCES_SCREEN_ROUTE
											  )
					} ,
					onNavigateToAboutScreen = {
						navController.navigate(
								ABOUT_SCREEN_ROUTE
											  )
					} ,
					onNavigateToWebViewScreen = { url : String ->
						navController.navigate(
								WEB_VIEW_SCREEN_ROUTE
									.replace(
											"{url}" ,
											url
											) ,
											  )
					} ,
					paddingValues = paddingValues)
		}
		composable(WEB_VIEW_SCREEN_ROUTE) {
			WebViewScreen(
					url = it.arguments?.getString("url") !! ,
					paddingValues = paddingValues
						 )
		}
		composable(ABOUT_SCREEN_ROUTE) {
			About(paddingValues)
		}
		composable(LICENCES_SCREEN_ROUTE) {
			Licences(paddingValues)
		}
		composable(PRAYER_TIMES_SETTINGS_SCREEN_ROUTE) {
			PrayerTimesCustomizations(paddingValues)
		}
	}
}