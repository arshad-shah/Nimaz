package com.arshadshah.nimaz.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.activity
import com.arshadshah.nimaz.activities.*
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.CHAPTERS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.FORGOT_PASSWORD_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.LOGIN_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.NAMESOFALLAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QURAN_AYA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.REGISTER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.SCREEN_ANIMATION_DURATION
import com.arshadshah.nimaz.constants.AppConstants.SHAHADAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_SCREEN_ROUTE
import com.arshadshah.nimaz.ui.screens.*
import com.arshadshah.nimaz.ui.screens.auth.EmailPasswordScreenSignin
import com.arshadshah.nimaz.ui.screens.auth.EmailPasswordScreenSignup
import com.arshadshah.nimaz.ui.screens.auth.ForgotPasswordScreen
import com.arshadshah.nimaz.ui.screens.quran.AyatScreen
import com.arshadshah.nimaz.ui.screens.quran.QuranScreen
import com.arshadshah.nimaz.ui.screens.settings.About
import com.arshadshah.nimaz.ui.screens.settings.PrayerTimesCustomizations
import com.arshadshah.nimaz.ui.screens.settings.SettingsScreen
import com.arshadshah.nimaz.ui.screens.tasbih.ChapterList
import com.arshadshah.nimaz.ui.screens.tasbih.DuaList
import com.arshadshah.nimaz.ui.screens.tasbih.TasbihScreen
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
			startDestination = BottomNavItem.PrayerTimesScreen.screen_route ,
			enterTransition = {
				when (initialState.destination.route)
				{
					BottomNavItem.PrayerTimesScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					BottomNavItem.QiblaScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					BottomNavItem.QuranScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					BottomNavItem.MoreScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					BottomNavItem.SettingsScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					QURAN_AYA_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					CHAPTER_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					CHAPTERS_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					TASBIH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					NAMESOFALLAH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					SHAHADAH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					LOGIN_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					REGISTER_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					FORGOT_PASSWORD_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					else -> EnterTransition.None
				}
			} ,
			exitTransition = {
				when (targetState.destination.route)
				{
					BottomNavItem.PrayerTimesScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					BottomNavItem.QiblaScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					BottomNavItem.QuranScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					BottomNavItem.MoreScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					BottomNavItem.SettingsScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					QURAN_AYA_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					CHAPTER_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					CHAPTERS_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					TASBIH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					NAMESOFALLAH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					SHAHADAH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					LOGIN_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					REGISTER_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					FORGOT_PASSWORD_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					else -> ExitTransition.None
				}
			} ,
			popEnterTransition = {
				when (initialState.destination.route)
				{
					BottomNavItem.PrayerTimesScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					BottomNavItem.QiblaScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					BottomNavItem.QuranScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					BottomNavItem.MoreScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					BottomNavItem.SettingsScreen.screen_route ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					QURAN_AYA_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					CHAPTER_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					CHAPTERS_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					TASBIH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					NAMESOFALLAH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					SHAHADAH_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					LOGIN_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					REGISTER_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					FORGOT_PASSWORD_SCREEN_ROUTE ->
						slideIntoContainer(
								AnimatedContentScope.SlideDirection.Left ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										  )
					else -> EnterTransition.None
				}
			} ,
			popExitTransition = {
				when (targetState.destination.route)
				{
					BottomNavItem.PrayerTimesScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					BottomNavItem.QiblaScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					BottomNavItem.QuranScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					BottomNavItem.MoreScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					BottomNavItem.SettingsScreen.screen_route ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					QURAN_AYA_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					CHAPTER_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					CHAPTERS_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					TASBIH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					NAMESOFALLAH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					SHAHADAH_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					LOGIN_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					REGISTER_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					FORGOT_PASSWORD_SCREEN_ROUTE ->
						slideOutOfContainer(
								AnimatedContentScope.SlideDirection.Right ,
								animationSpec = tween(SCREEN_ANIMATION_DURATION)
										   )
					else -> ExitTransition.None
				}
			}
				   ) {
		composable(BottomNavItem.PrayerTimesScreen.screen_route)
		{ PrayerTimesScreen(paddingValues = paddingValues) }

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
						//replace the placeholder with the actual route TASBIH_SCREEN_ROUTE
						navController.navigate(
								TASBIH_SCREEN_ROUTE.replace(
										"{arabic}" ,
										arabic
														   )
											  )
					} ,
					onNavigateToNames = {
						navController.navigate(NAMESOFALLAH_SCREEN_ROUTE)
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

		composable(TASBIH_SCREEN_ROUTE) {
			TasbihScreen(paddingValues = paddingValues)
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

		activity("Zakat") {
			this.activityClass = ZakatCalculator::class
		}

		composable(SHAHADAH_SCREEN_ROUTE) {
			ShahadahScreen(paddingValues)
		}

		composable(BottomNavItem.SettingsScreen.screen_route) {
			SettingsScreen(
					onNavigateToSignin = {
						navController.navigate(LOGIN_SCREEN_ROUTE)
					},
					onNavigateToSignup = {
						navController.navigate(REGISTER_SCREEN_ROUTE)
					},
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

		composable(LOGIN_SCREEN_ROUTE) {
			EmailPasswordScreenSignin(
					paddingValues,
					onNavigateToSignup = {
						navController.navigate(REGISTER_SCREEN_ROUTE)
					},
					onNavigateToPasswordReset = {
						navController.navigate(FORGOT_PASSWORD_SCREEN_ROUTE)
					},
					navController = navController
					 )
		}
		composable(REGISTER_SCREEN_ROUTE) {
			EmailPasswordScreenSignup(paddingValues, navController = navController)
		}

		//forgot password
		composable(FORGOT_PASSWORD_SCREEN_ROUTE) {
			ForgotPasswordScreen(paddingValues, navController = navController,
								 onNavigateToSignin = {
				navController.navigate(LOGIN_SCREEN_ROUTE)
			}
								) {
				navController.navigate(REGISTER_SCREEN_ROUTE)
			}
		}
	}
}