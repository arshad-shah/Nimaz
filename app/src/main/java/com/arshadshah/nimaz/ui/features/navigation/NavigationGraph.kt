package com.arshadshah.nimaz.ui.features.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.arshadshah.nimaz.ui.features.screens.AyatScreen
import com.arshadshah.nimaz.ui.features.screens.PrayerTimesScreen
import com.arshadshah.nimaz.ui.features.screens.QiblaScreen
import com.arshadshah.nimaz.ui.features.screens.QuranScreen
import com.arshadshah.nimaz.ui.features.screens.settings.PrayerTimesCustomizations
import com.arshadshah.nimaz.ui.features.screens.settings.SettingsScreen

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NavigationGraph(navController: NavController, paddingValues: PaddingValues) {
    NavHost(
        navController = navController as NavHostController,
        startDestination = BottomNavItem.PrayerTimesScreen.screen_route
    ) {
        composable(BottomNavItem.PrayerTimesScreen.screen_route) {
            PrayerTimesScreen(paddingValues)
        }
        composable(BottomNavItem.QiblaScreen.screen_route) {
            QiblaScreen(paddingValues)
        }
        QuranGraph(navController = navController, paddingValues = paddingValues)
        SettingGraph(navController = navController, paddingValues = paddingValues)
    }
}

fun NavGraphBuilder.QuranGraph(
    navController: NavController,
    paddingValues: PaddingValues,
) {
    navigation(
        route = BottomNavItem.QuranScreen.screen_route,
        startDestination = "quran"
    ) {
        composable("quran") {
            QuranScreen(
                paddingValues,
                onNavigateToAyatScreen = { number: String, isSurah: Boolean, isEnglish: Boolean ->
                    navController.navigate("ayatScreen/$number/$isSurah/$isEnglish")
                })
        }
        composable("ayatScreen/{number}/{isSurah}/{isEnglish}") {
            AyatScreen(
                number = it.arguments?.getString("number"),
                isSurah = it.arguments?.getString("isSurah")!!,
                isEnglish = it.arguments?.getString("isEnglish")!!,
                paddingValues = paddingValues
            )
        }
    }
}

fun NavGraphBuilder.SettingGraph(navController: NavController, paddingValues: PaddingValues) {
    navigation(startDestination = "settings", route = BottomNavItem.SettingsScreen.screen_route) {
        composable("settings") {
            SettingsScreen(onNavigateToPrayerTimeCustomizationScreen = {
                navController.navigate(
                    "PrayerTimesCustomizations"
                )
            }, paddingValues)
        }
        composable("PrayerTimesCustomizations") {
            PrayerTimesCustomizations(paddingValues)
        }
    }
}
