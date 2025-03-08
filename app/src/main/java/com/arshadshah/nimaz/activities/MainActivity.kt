package com.arshadshah.nimaz.activities

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.CALENDER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TRACKER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QIBLA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_SCREEN_ROUTE
import com.arshadshah.nimaz.ui.navigation.BottomNavItem
import com.arshadshah.nimaz.ui.navigation.NavigationGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.rememberSystemUiController
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.ThemeDataStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sharedPref: PrivateSharedPreferences

    @Inject
    lateinit var themeDataStore: ThemeDataStore

    @Inject
    lateinit var firebaseLogger: FirebaseLogger

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.actionBar?.hide()

        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition { false }

        // Log app start
        firebaseLogger.logEvent(
            "app_started",
            mapOf(
                "android_version" to Build.VERSION.RELEASE,
                "device_model" to Build.MODEL
            )
        )

        val firstTime = sharedPref.getDataBoolean(AppConstants.IS_FIRST_INSTALL, true)

        // Log whether this is first install
        if (firstTime) {
            firebaseLogger.logEvent("first_time_user", null)
        } else {
            firebaseLogger.logEvent("returning_user", null)
        }

        setContent {
            // Collect theme preferences directly from ThemeDataStore
            val isDarkMode by themeDataStore.darkModeFlow.collectAsState(initial = false)
            val currentTheme by themeDataStore.themeFlow.collectAsState(initial = AppConstants.THEME_SYSTEM)

            NimazTheme(
                darkTheme = isDarkMode,
                themeName = currentTheme
            ) {
                val systemUiController = rememberSystemUiController()

                systemUiController.setStatusBarColor(
                    color = MaterialTheme.colorScheme.background,
                    darkIcons = !isDarkMode,
                )
                systemUiController.setNavigationBarColor(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    darkIcons = !isDarkMode,
                )
                val navController = rememberNavController()
                val route =
                    remember(navController) { mutableStateOf(navController.currentDestination?.route) }

                // Add navigation destination change listener with screen tracking
                navController.addOnDestinationChangedListener { controller, destination, arguments ->
                    val newRoute = destination.route ?: "unknown"
                    route.value = newRoute

                    // Log screen view
                    logScreenView(
                        navController = controller,
                        screenRoute = newRoute,
                        arguments = arguments
                    )
                }

                NavigationGraph(
                    navController = navController,
                    context = this@MainActivity,
                    isFirstInstall = firstTime,
                )
            }
        }
    }

    /**
     * Log screen view to analytics
     */
    private fun logScreenView(
        navController: NavController,
        screenRoute: String,
        arguments: Bundle?
    ) {
        // Extract the base route without parameters
        val baseRoute = screenRoute.split("?", "/").firstOrNull() ?: screenRoute

        // Map route to readable screen name
        val screenName = getScreenNameFromRoute(baseRoute)

        // Collect parameters for the analytics event
        val params = mutableMapOf<String, Any>(
            "screen_route" to screenRoute,
            "screen_name" to screenName
        )

        // Add any route-specific parameters
        when {
            screenRoute.contains("surahNumber") || screenRoute.contains("ayaNumber") -> {
                arguments?.getString("surahNumber")?.let { params["surah_number"] = it }
                arguments?.getString("ayaNumber")?.let { params["aya_number"] = it }
            }

            screenRoute.contains("bookId") || screenRoute.contains("chapterId") -> {
                arguments?.getString("bookId")?.let { params["book_id"] = it }
                arguments?.getString("chapterId")?.let { params["chapter_id"] = it }
            }

            screenRoute.contains("number") || screenRoute.contains("isSurah") -> {
                arguments?.getString("number")?.let { params["number"] = it }
                arguments?.getString("isSurah")?.let { params["is_surah"] = it }
                arguments?.getString("language")?.let { params["language"] = it }
                arguments?.getString("scrollTo")?.let { params["scroll_to"] = it }
            }

            screenRoute.contains("id") || screenRoute.contains("arabic") -> {
                arguments?.getString("id")?.let { params["tasbih_id"] = it }
                // Don't log the full Arabic/translation text to avoid large event parameters
                arguments?.getString("arabic")?.let { params["has_arabic"] = true }
                arguments?.getString("translation")?.let { params["has_translation"] = true }
                arguments?.getString("transliteration")
                    ?.let { params["has_transliteration"] = true }
            }
        }

        // Log the screen view event
        firebaseLogger.logScreenView(screenName, screenName)

        // Log additional analytics for specific screens
        when (baseRoute) {
            "Qibla", QIBLA_SCREEN_ROUTE -> {
                firebaseLogger.logEvent("qibla_finder_opened", null)
            }

            "Zakat" -> {
                firebaseLogger.logEvent("zakat_calculator_opened", null)
            }

            "Tasbih", TASBIH_SCREEN_ROUTE -> {
                arguments?.getString("id")?.let {
                    firebaseLogger.logEvent("tasbih_counter_used", mapOf("tasbih_id" to it))
                }
            }

            "PrayerTracker", PRAYER_TRACKER_SCREEN_ROUTE -> {
                firebaseLogger.logEvent("prayer_tracker_opened", null)
            }

            "Calender", CALENDER_SCREEN_ROUTE -> {
                firebaseLogger.logEvent("calendar_opened", null)
            }
        }
    }

    /**
     * Convert route to readable screen name based on your app's routes
     */
    private fun getScreenNameFromRoute(route: String): String {
        return when (route) {
            "Intro" -> "OnboardingScreen"
            BottomNavItem.Dashboard.screen_route -> "DashboardScreen"
            BottomNavItem.PrayerTimesScreen.screen_route -> "PrayerTimesScreen"
            BottomNavItem.QuranScreen.screen_route -> "QuranScreen"
            BottomNavItem.MoreScreen.screen_route -> "MoreScreen"
            BottomNavItem.SettingsScreen.screen_route -> "SettingsScreen"
            QIBLA_SCREEN_ROUTE -> "QiblaScreen"
            CALENDER_SCREEN_ROUTE -> "CalendarScreen"
            PRAYER_TRACKER_SCREEN_ROUTE -> "PrayerTrackerScreen"
            TASBIH_SCREEN_ROUTE -> "TasbihScreen"
            AppConstants.TASBIH_LIST_SCREEN -> "TasbihListScreen"
            AppConstants.NAMESOFALLAH_SCREEN_ROUTE -> "NamesOfAllahScreen"
            AppConstants.CATEGORY_SCREEN_ROUTE -> "CategoryScreen"
            AppConstants.CHAPTERS_SCREEN_ROUTE -> "ChaptersScreen"
            AppConstants.CHAPTER_SCREEN_ROUTE -> "DuaListScreen"
            AppConstants.SHAHADAH_SCREEN_ROUTE -> "ShahadahScreen"
            AppConstants.QURAN_AYA_SCREEN_ROUTE, AppConstants.MY_QURAN_SCREEN_ROUTE -> "AyatScreen"
            AppConstants.TAFSEER_SCREEN_ROUTE -> "TafseerScreen"
            AppConstants.HADITH_SHELF_SCREEN_ROUTE -> "HadithBookShelfScreen"
            AppConstants.HADITH_CHAPTERS_LIST_SCREEN_ROUTE -> "HadithChaptersListScreen"
            AppConstants.HADITH_LIST_SCREEN_ROUTE -> "HadithListScreen"
            AppConstants.PRAYER_TIMES_SETTINGS_SCREEN_ROUTE -> "PrayerTimesSettingsScreen"
            AppConstants.LICENCES_SCREEN_ROUTE -> "LicensesScreen"
            AppConstants.ABOUT_SCREEN_ROUTE -> "AboutScreen"
            AppConstants.WEB_VIEW_SCREEN_ROUTE -> "WebViewScreen"
            AppConstants.DEBUG_MODE -> "DebugScreen"
            "Zakat" -> "ZakatCalculatorScreen"
            "licenseDetail" -> "LibraryDetailScreen"
            else -> {
                // For unknown routes, try to derive a name from the route
                route.split("/").first().capitalize() + "Screen"
            }
        }
    }

    // Extension function to capitalize first letter
    private fun String.capitalize(): String {
        return if (this.isNotEmpty()) {
            this.replaceFirstChar { it.uppercase() }
        } else {
            "Unknown"
        }
    }
}