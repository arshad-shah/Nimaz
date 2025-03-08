package com.arshadshah.nimaz.ui.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.ABOUT_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CALENDER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTERS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.DEBUG_MODE
import com.arshadshah.nimaz.constants.AppConstants.HADITH_CHAPTERS_LIST_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.HADITH_LIST_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.HADITH_SHELF_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.LICENCES_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.MY_QURAN_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.NAMESOFALLAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TIMES_SETTINGS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TRACKER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QIBLA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QURAN_AYA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.SHAHADAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.TAFSEER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_LIST_SCREEN
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.WEB_VIEW_SCREEN_ROUTE
import com.arshadshah.nimaz.ui.screens.Dashboard
import com.arshadshah.nimaz.ui.screens.PrayerTimesScreen
import com.arshadshah.nimaz.ui.screens.hadith.BookShelf
import com.arshadshah.nimaz.ui.screens.hadith.HadithChaptersList
import com.arshadshah.nimaz.ui.screens.hadith.HadithList
import com.arshadshah.nimaz.ui.screens.introduction.IntroPage
import com.arshadshah.nimaz.ui.screens.more.MoreScreen
import com.arshadshah.nimaz.ui.screens.more.NamesOfAllah
import com.arshadshah.nimaz.ui.screens.more.QiblaScreen
import com.arshadshah.nimaz.ui.screens.more.ShahadahScreen
import com.arshadshah.nimaz.ui.screens.more.ZakatCalculator
import com.arshadshah.nimaz.ui.screens.quran.AyatScreen
import com.arshadshah.nimaz.ui.screens.quran.QuranScreen
import com.arshadshah.nimaz.ui.screens.quran.TafseerScreen
import com.arshadshah.nimaz.ui.screens.settings.DebugScreen
import com.arshadshah.nimaz.ui.screens.settings.EnhancedAboutScreen
import com.arshadshah.nimaz.ui.screens.settings.LibraryDetailScreen
import com.arshadshah.nimaz.ui.screens.settings.LicensesScreen
import com.arshadshah.nimaz.ui.screens.settings.PrayerTimesCustomizations
import com.arshadshah.nimaz.ui.screens.settings.SettingsScreen
import com.arshadshah.nimaz.ui.screens.settings.WebViewScreen
import com.arshadshah.nimaz.ui.screens.tasbih.Categories
import com.arshadshah.nimaz.ui.screens.tasbih.ChapterList
import com.arshadshah.nimaz.ui.screens.tasbih.DuaList
import com.arshadshah.nimaz.ui.screens.tasbih.ListOfTasbih
import com.arshadshah.nimaz.ui.screens.tasbih.TasbihScreen
import com.arshadshah.nimaz.ui.screens.tracker.CalendarScreen
import com.arshadshah.nimaz.ui.screens.tracker.PrayerTracker

// Extension function for safer navigation with consistent options
fun NavController.safeNavigate(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {
        // Default navigation behavior - smooth transitions
        launchSingleTop = true
        restoreState = true
    }
) {
    try {
        navigate(route) {
            builder()
        }
        Log.d("Navigation", "Navigating to: $route")
    } catch (e: Exception) {
        Log.e("Navigation", "Failed to navigate to $route: ${e.message}", e)
    }
}

// Extension for bottom nav navigation
fun NavController.navigateToBottomNavItem(route: String) {
    safeNavigate(route) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}

// Default transition animation durations
private const val NAV_ANIM_DURATION = 300

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NavigationGraph(
    navController: NavController,
    context: MainActivity,
    isFirstInstall: Boolean,
) {
    val navHostController = navController as NavHostController

    // Set up navigation error tracking
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            Log.d("Navigation", "Navigated to: ${destination.route}")
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    val startDestination = if (isFirstInstall) "Intro" else BottomNavItem.Dashboard.screen_route

    // Define standard transitions for all screens
    val defaultEnterTransition = remember {
        { _: AnimatedContentTransitionScope<NavBackStackEntry> ->
            fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
        }
    }

    val defaultExitTransition = remember {
        { _: AnimatedContentTransitionScope<NavBackStackEntry> ->
            fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
        }
    }

    NavHost(
        navController = navHostController,
        startDestination = startDestination,
        enterTransition = defaultEnterTransition,
        exitTransition = defaultExitTransition,
    ) {
        composable("Intro") {
            IntroPage(
                navController = navController,
            )
        }

        // --- Bottom Navigation Items ---

        composable(BottomNavItem.Dashboard.screen_route) {
            Dashboard(
                navController = navController,
                context = context,
                onNavigateToCalender = {
                    navController.safeNavigate(CALENDER_SCREEN_ROUTE)
                },
                onNavigateToAyatScreen = { number: String, isSurah: Boolean, language: String, scrollToAya: Int ->
                    val route = MY_QURAN_SCREEN_ROUTE
                        .replace("{number}", number)
                        .replace("{isSurah}", isSurah.toString())
                        .replace("{language}", language)
                        .replace("{scrollTo}", scrollToAya.toString())

                    navController.safeNavigate(route)
                },
                onNavigateToTasbihScreen = { id: String, arabic: String, translation: String, transliteration: String ->
                    val route = TASBIH_SCREEN_ROUTE
                        .replace("{id}", id)
                        .replace("{arabic}", arabic)
                        .replace("{translation}", translation)
                        .replace("{transliteration}", transliteration)

                    navController.safeNavigate(route)
                },
                onNavigateToTasbihListScreen = {
                    navController.safeNavigate(TASBIH_LIST_SCREEN)
                },
            )
        }

        composable(BottomNavItem.PrayerTimesScreen.screen_route) {
            PrayerTimesScreen(
                navController = navController,
            )
        }

        composable(BottomNavItem.QuranScreen.screen_route) {
            QuranScreen(
                navController = navController,
                onNavigateToAyatScreen = { number: String, isSurah: Boolean, language: String, scrollToAya: Int? ->
                    if (scrollToAya != null) {
                        val route = MY_QURAN_SCREEN_ROUTE
                            .replace("{number}", number)
                            .replace("{isSurah}", isSurah.toString())
                            .replace("{language}", language)
                            .replace("{scrollTo}", scrollToAya.toString())

                        navController.safeNavigate(route) {
                            // Avoid using inclusive=true as it causes animation issues
                            popUpTo(BottomNavItem.QuranScreen.screen_route) {
                                saveState = true
                            }
                        }
                    } else {
                        val route = QURAN_AYA_SCREEN_ROUTE
                            .replace("{number}", number)
                            .replace("{isSurah}", isSurah.toString())
                            .replace("{language}", language)

                        navController.safeNavigate(route)
                    }
                }
            )
        }

        composable(BottomNavItem.MoreScreen.screen_route) {
            MoreScreen(
                navController = navController,
                onNavigateToTasbihScreen = { id: String, arabic: String, translation: String, transliteration: String ->
                    val route = TASBIH_SCREEN_ROUTE
                        .replace("{id}", id)
                        .replace("{arabic}", arabic)
                        .replace("{translation}", translation)
                        .replace("{transliteration}", transliteration)

                    navController.safeNavigate(route)
                },
                onNavigateToTasbihListScreen = {
                    navController.safeNavigate(TASBIH_LIST_SCREEN)
                },
                onNavigateToNames = {
                    navController.safeNavigate(NAMESOFALLAH_SCREEN_ROUTE)
                },
                onNavigateToListOfTasbeeh = {
                    navController.safeNavigate(AppConstants.CATEGORY_SCREEN_ROUTE)
                },
                onNavigateToQibla = {
                    navController.safeNavigate(QIBLA_SCREEN_ROUTE)
                },
                onNavigateToShadah = {
                    navController.safeNavigate(SHAHADAH_SCREEN_ROUTE)
                },
                onNavigateToPrayerTracker = {
                    navController.safeNavigate(PRAYER_TRACKER_SCREEN_ROUTE)
                },
                onNavigateToCalender = {
                    navController.safeNavigate(CALENDER_SCREEN_ROUTE)
                },
                onNavigateToZakat = {
                    navController.safeNavigate("Zakat")
                },
                onNavigateToHadithShelf = {
                    navController.safeNavigate(HADITH_SHELF_SCREEN_ROUTE)
                }
            )
        }

        composable(BottomNavItem.SettingsScreen.screen_route) {
            SettingsScreen(
                activity = context,
                navController = navController,
                onNavigateToPrayerTimeCustomizationScreen = {
                    navController.safeNavigate(PRAYER_TIMES_SETTINGS_SCREEN_ROUTE)
                },
                onNavigateToLicencesScreen = {
                    navController.safeNavigate(LICENCES_SCREEN_ROUTE)
                },
                onNavigateToAboutScreen = {
                    navController.safeNavigate(ABOUT_SCREEN_ROUTE)
                },
                onNavigateToWebViewScreen = { url: String ->
                    val route = WEB_VIEW_SCREEN_ROUTE.replace("{url}", url)
                    navController.safeNavigate(route)
                },
                onNavigateToDebugScreen = {
                    navController.safeNavigate(DEBUG_MODE)
                },
            )
        }

        // --- Content Screens ---

        composable(CALENDER_SCREEN_ROUTE) {
            CalendarScreen(navController = navController)
        }

        composable(QIBLA_SCREEN_ROUTE) {
            QiblaScreen(navController)
        }

        composable(MY_QURAN_SCREEN_ROUTE) {
            AyatScreen(
                number = it.arguments?.getString("number")!!,
                isSurah = it.arguments?.getString("isSurah")!!,
                language = it.arguments?.getString("language")!!,
                scrollToAya = it.arguments?.getString("scrollTo")!!.toInt(),
                navController = navController
            )
        }

        composable(QURAN_AYA_SCREEN_ROUTE) {
            AyatScreen(
                navController = navController,
                number = it.arguments?.getString("number")!!,
                isSurah = it.arguments?.getString("isSurah")!!,
                language = it.arguments?.getString("language")!!,
            )
        }

        composable(HADITH_SHELF_SCREEN_ROUTE) {
            BookShelf(
                navController = navController,
                onNavigateToChapterFromFavourite = { bookId: Int, chapterId: Int ->
                    val route = HADITH_LIST_SCREEN_ROUTE
                        .replace("{bookId}", bookId.toString())
                        .replace("{chapterId}", chapterId.toString())

                    navController.safeNavigate(route)
                },
                onNavigateToChaptersList = { id: Int, title: String ->
                    val route = HADITH_CHAPTERS_LIST_SCREEN_ROUTE
                        .replace("{bookId}", id.toString())
                        .replace("{bookName}", title)

                    navController.safeNavigate(route)
                }
            )
        }

        composable(HADITH_CHAPTERS_LIST_SCREEN_ROUTE) {
            HadithChaptersList(
                navController = navController,
                bookId = it.arguments?.getString("bookId"),
                onNavigateToAChapter = { bookId: Int, chapterId: Int ->
                    val route = HADITH_LIST_SCREEN_ROUTE
                        .replace("{bookId}", bookId.toString())
                        .replace("{chapterId}", chapterId.toString())

                    navController.safeNavigate(route)
                }
            )
        }

        composable(HADITH_LIST_SCREEN_ROUTE) {
            HadithList(
                navController = navController,
                bookId = it.arguments?.getString("bookId"),
                chapterId = it.arguments?.getString("chapterId")
            )
        }

        composable(TASBIH_LIST_SCREEN) {
            ListOfTasbih(
                navController = navController
            ) { id: String, arabic: String, translation: String, transliteration: String ->
                val route = TASBIH_SCREEN_ROUTE
                    .replace("{id}", id)
                    .replace("{arabic}", arabic)
                    .replace("{translation}", translation)
                    .replace("{transliteration}", transliteration)

                navController.safeNavigate(route)
            }
        }

        composable(PRAYER_TRACKER_SCREEN_ROUTE) {
            PrayerTracker(navController = navController)
        }

        composable(TASBIH_SCREEN_ROUTE) {
            TasbihScreen(
                tasbihId = it.arguments?.getString("id")!!,
                tasbihArabic = it.arguments?.getString("arabic")!!,
                tasbihEnglish = it.arguments?.getString("translation")!!,
                tasbihTranslitration = it.arguments?.getString("transliteration")!!,
                navController = navController
            )
        }

        composable(NAMESOFALLAH_SCREEN_ROUTE) {
            NamesOfAllah(navController = navController)
        }

        composable(AppConstants.CATEGORY_SCREEN_ROUTE) {
            Categories(
                navController = navController,
            ) { category: String, id: Int ->
                Log.d("Category", category)
                val route = CHAPTERS_SCREEN_ROUTE
                    .replace("{title}", category)
                    .replace("{id}", id.toString())

                navController.safeNavigate(route)
            }
        }

        composable(CHAPTERS_SCREEN_ROUTE) {
            ChapterList(
                categoryId = it.arguments?.getString("id")!!,
                navController = navController,
                onNavigateToChapter = { chapterId: Int, categoryName: String ->
                    val route = CHAPTER_SCREEN_ROUTE
                        .replace("{chapterId}", chapterId.toString())
                        .replace("{categoryName}", categoryName)

                    navController.safeNavigate(route)
                },
            )
        }

        composable(CHAPTER_SCREEN_ROUTE) {
            DuaList(
                chapterId = it.arguments?.getString("chapterId")!!,
                navController = navController
            )
        }

        composable(SHAHADAH_SCREEN_ROUTE) {
            ShahadahScreen(navController)
        }

        composable(WEB_VIEW_SCREEN_ROUTE) {
            WebViewScreen(
                url = it.arguments?.getString("url")!!,
                navController = navController
            )
        }

        composable(ABOUT_SCREEN_ROUTE) {
            EnhancedAboutScreen(
                navController = navController,
                onImageClicked = { navController.safeNavigate(DEBUG_MODE) }
            )
        }

        composable(LICENCES_SCREEN_ROUTE) {
            LicensesScreen(
                navController = navController,
            )
        }

        composable(PRAYER_TIMES_SETTINGS_SCREEN_ROUTE) {
            PrayerTimesCustomizations(navController)
        }

        composable(DEBUG_MODE) {
            DebugScreen(
                navController = navController,
            )
        }

        composable("Zakat") {
            ZakatCalculator(navController = navController)
        }

        composable("licenseDetail/{id}") {
            LibraryDetailScreen(
                uniqueLibId = it.arguments?.getString("id")!!,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // Use special transition for Tafseer screen as it's content-heavy
        composable(
            TAFSEER_SCREEN_ROUTE,
            enterTransition = {
                fadeIn(animationSpec = tween(NAV_ANIM_DURATION + 100))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            }
        ) { backStackEntry ->
            val surahNumber = backStackEntry.arguments?.getString("surahNumber")?.toIntOrNull() ?: 1
            val ayaNumber = backStackEntry.arguments?.getString("ayaNumber")?.toIntOrNull() ?: 1

            TafseerScreen(
                ayaNumber = ayaNumber,
                surahNumber = surahNumber,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}