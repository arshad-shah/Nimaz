package com.arshadshah.nimaz.ui.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
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
import com.arshadshah.nimaz.constants.AppConstants.SCREEN_ANIMATION_DURATION
import com.arshadshah.nimaz.constants.AppConstants.SHAHADAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_LIST_SCREEN
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.WEB_VIEW_SCREEN_ROUTE
import com.arshadshah.nimaz.ui.screens.Dashboard
import com.arshadshah.nimaz.ui.screens.PrayerTimesScreen
import com.arshadshah.nimaz.ui.screens.hadith.BookShelf
import com.arshadshah.nimaz.ui.screens.hadith.HadithChaptersList
import com.arshadshah.nimaz.ui.screens.hadith.HadithList
import com.arshadshah.nimaz.ui.screens.more.MoreScreen
import com.arshadshah.nimaz.ui.screens.more.NamesOfAllah
import com.arshadshah.nimaz.ui.screens.more.QiblaScreen
import com.arshadshah.nimaz.ui.screens.more.ShahadahScreen
import com.arshadshah.nimaz.ui.screens.quran.AyatScreen
import com.arshadshah.nimaz.ui.screens.quran.QuranScreen
import com.arshadshah.nimaz.ui.screens.settings.About
import com.arshadshah.nimaz.ui.screens.settings.DebugScreen
import com.arshadshah.nimaz.ui.screens.settings.Licences
import com.arshadshah.nimaz.ui.screens.settings.PrayerTimesCustomizations
import com.arshadshah.nimaz.ui.screens.settings.SettingsScreen
import com.arshadshah.nimaz.ui.screens.settings.WebViewScreen
import com.arshadshah.nimaz.ui.screens.tasbih.Categories
import com.arshadshah.nimaz.ui.screens.tasbih.ChapterList
import com.arshadshah.nimaz.ui.screens.tasbih.DuaList
import com.arshadshah.nimaz.ui.screens.tasbih.ListOfTasbih
import com.arshadshah.nimaz.ui.screens.tasbih.TasbihScreen
import com.arshadshah.nimaz.ui.screens.tracker.CalenderScreen
import com.arshadshah.nimaz.ui.screens.tracker.PrayerTracker

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NavigationGraph(
    navController: NavController,
    paddingValues: PaddingValues,
    context: MainActivity,
) {
    NavHost(
        navController = navController as NavHostController,
        startDestination = BottomNavItem.Dashboard.screen_route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(
                    easing = FastOutSlowInEasing, // Changed easing for a different effect
                    durationMillis = SCREEN_ANIMATION_DURATION
                )
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                    durationMillis = SCREEN_ANIMATION_DURATION
                )
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                    durationMillis = SCREEN_ANIMATION_DURATION
                )
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(
                    easing = FastOutSlowInEasing,
                    durationMillis = SCREEN_ANIMATION_DURATION
                )
            )
        }
    ) {

        composable(BottomNavItem.Dashboard.screen_route) {
            Dashboard(
                paddingValues = paddingValues,
                onNavigateToCalender = {
                    navController.navigate(CALENDER_SCREEN_ROUTE)
                },
                onNavigateToTracker = {
                    navController.navigate(PRAYER_TRACKER_SCREEN_ROUTE)
                },
                onNavigateToAyatScreen = { number: String, isSurah: Boolean, language: String, scrollToAya: Int ->
                    navController.navigate(
                        MY_QURAN_SCREEN_ROUTE.replace(
                            "{number}",
                            number
                        )
                            .replace(
                                "{isSurah}",
                                isSurah.toString()
                            )
                            .replace(
                                "{language}",
                                language
                            )
                            .replace(
                                "{scrollTo}",
                                scrollToAya.toString()
                            )
                    )
                },
                onNavigateToTasbihScreen = { id: String, arabic: String, translation: String, transliteration: String ->
                    navController.navigate(
                        TASBIH_SCREEN_ROUTE
                            .replace(
                                "{id}",
                                id
                            )
                            .replace(
                                "{arabic}",
                                arabic
                            )
                            .replace(
                                "{translation}",
                                translation
                            )
                            .replace(
                                "{transliteration}",
                                transliteration
                            )
                    )
                },
                onNavigateToTasbihListScreen = {
                    navController.navigate(TASBIH_LIST_SCREEN)
                },
            )

        }

        composable(BottomNavItem.PrayerTimesScreen.screen_route)
        {
            PrayerTimesScreen(
                paddingValues = paddingValues
            )
        }

        composable(CALENDER_SCREEN_ROUTE) {
            CalenderScreen(paddingValues)
        }

        composable(QIBLA_SCREEN_ROUTE) {
            QiblaScreen(paddingValues)
        }
        composable(BottomNavItem.QuranScreen.screen_route) {
            QuranScreen(
                context = context,
                paddingValues = paddingValues,
                onNavigateToAyatScreen = { number: String, isSurah: Boolean, language: String, scrollToAya: Int? ->
                    if (scrollToAya != null) {
                        navController.navigate(
                            MY_QURAN_SCREEN_ROUTE.replace(
                                "{number}",
                                number
                            )
                                .replace(
                                    "{isSurah}",
                                    isSurah.toString()
                                )
                                .replace(
                                    "{language}",
                                    language
                                )
                                .replace(
                                    "{scrollTo}",
                                    scrollToAya.toString()
                                )
                        ) {
                            popUpTo(MY_QURAN_SCREEN_ROUTE) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(
                            QURAN_AYA_SCREEN_ROUTE.replace(
                                "{number}",
                                number
                            )
                                .replace(
                                    "{isSurah}",
                                    isSurah.toString()
                                )
                                .replace(
                                    "{language}",
                                    language
                                )
                        )
                    }
                }
            )
        }
        composable(MY_QURAN_SCREEN_ROUTE) {
            AyatScreen(
                number = it.arguments?.getString("number")!!,
                isSurah = it.arguments?.getString("isSurah")!!,
                language = it.arguments?.getString("language")!!,
                scrollToAya = it.arguments?.getString("scrollTo")!!.toInt(),
                paddingValues = paddingValues,
                context = context
            )
        }

        composable(QURAN_AYA_SCREEN_ROUTE) {
            AyatScreen(
                number = it.arguments?.getString("number")!!,
                isSurah = it.arguments?.getString("isSurah")!!,
                language = it.arguments?.getString("language")!!,
                paddingValues = paddingValues,
                context = context
            )
        }


        composable(BottomNavItem.MoreScreen.screen_route) {
            MoreScreen(
                paddingValues,
                onNavigateToTasbihScreen = { id: String, arabic: String, translation: String, transliteration: String ->
                    navController.navigate(
                        TASBIH_SCREEN_ROUTE
                            .replace(
                                "{id}",
                                id
                            )
                            .replace(
                                "{arabic}",
                                arabic
                            )
                            .replace(
                                "{translation}",
                                translation
                            )
                            .replace(
                                "{transliteration}",
                                transliteration
                            )
                    )
                },
                onNavigateToTasbihListScreen = {
                    navController.navigate(TASBIH_LIST_SCREEN)
                },
                onNavigateToNames = {
                    navController.navigate(NAMESOFALLAH_SCREEN_ROUTE)
                },
                onNavigateToListOfTasbeeh = {
                    navController.navigate(AppConstants.CATEGORY_SCREEN_ROUTE)
                },
                onNavigateToQibla = {
                    navController.navigate(QIBLA_SCREEN_ROUTE)
                },
                onNavigateToShadah = {
                    navController.navigate(SHAHADAH_SCREEN_ROUTE)
                },
                onNavigateToPrayerTracker = {
                    navController.navigate(PRAYER_TRACKER_SCREEN_ROUTE)
                },
                onNavigateToCalender = {
                    navController.navigate(CALENDER_SCREEN_ROUTE)
                },
                onNavigateToZakat = {
                    navController.navigate("Zakat")
                },
                onNavigateToHadithShelf = {
                    navController.navigate(HADITH_SHELF_SCREEN_ROUTE)
                }
            )
        }

        composable(HADITH_SHELF_SCREEN_ROUTE) {
            BookShelf(paddingValues = paddingValues,
                onNavigateToChapterFromFavourite = { bookId: Int, chapterId: Int ->
                    navController.navigate(
                        HADITH_LIST_SCREEN_ROUTE.replace(
                            "{bookId}", bookId.toString()
                        ).replace("{chapterId}", chapterId.toString())
                    )
                },
                onNavigateToChaptersList = { id: Int, title: String ->
                    navController.navigate(
                        HADITH_CHAPTERS_LIST_SCREEN_ROUTE.replace(
                            "{bookId}",
                            id.toString()
                        ).replace("{bookName}", title)
                    )
                }
            )
        }

        composable(HADITH_CHAPTERS_LIST_SCREEN_ROUTE) {
            HadithChaptersList(
                paddingValues = paddingValues,
                bookId = it.arguments?.getString("bookId")
            ) { bookId: Int, chapterId: Int ->
                navController.navigate(
                    HADITH_LIST_SCREEN_ROUTE.replace(
                        "{bookId}", bookId.toString()
                    ).replace("{chapterId}", chapterId.toString())
                )
            }
        }

        composable(HADITH_LIST_SCREEN_ROUTE) {
            HadithList(
                paddingValues,
                bookId = it.arguments?.getString("bookId"),
                chapterId = it.arguments?.getString("chapterId")
            )
        }


        composable(TASBIH_LIST_SCREEN) {
            ListOfTasbih(paddingValues) { id: String, arabic: String, translation: String, transliteration: String ->
                //replace the placeholder with the actual route TASBIH_SCREEN_ROUTE
                //tasbih_screen/{arabic}/{translation}/{transliteration}
                navController.navigate(
                    TASBIH_SCREEN_ROUTE
                        .replace(
                            "{id}",
                            id
                        )
                        .replace(
                            "{arabic}",
                            arabic
                        )
                        .replace(
                            "{translation}",
                            translation
                        )
                        .replace(
                            "{transliteration}",
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
                tasbihId = it.arguments?.getString("id")!!,
                tasbihArabic = it.arguments?.getString("arabic")!!,
                tasbihEnglish = it.arguments?.getString("translation")!!,
                tasbihTranslitration = it.arguments?.getString("transliteration")!!,
                paddingValues = paddingValues,
            )
        }

        composable(NAMESOFALLAH_SCREEN_ROUTE) {
            NamesOfAllah(paddingValues = paddingValues)
        }

        composable(AppConstants.CATEGORY_SCREEN_ROUTE) {
            Categories(
                paddingValues = paddingValues,
            )
            //pass the category name to the next screen
            { category: String, id: Int ->
                Log.d("Category", category)
                navController.navigate(
                    CHAPTERS_SCREEN_ROUTE
                        .replace(
                            "{title}",
                            category
                        )
                        .replace("{id}", id.toString())
                )
            }
        }

        composable(CHAPTERS_SCREEN_ROUTE) {
            ChapterList(
                categoryId = it.arguments?.getString("id")!!,
                paddingValues = paddingValues,
                onNavigateToChapter = { chapterId: Int, categoryName: String ->
                    navController.navigate(
                        CHAPTER_SCREEN_ROUTE
                            .replace(
                                "{chapterId}",
                                chapterId.toString()
                            )
                            .replace(
                                "{categoryName}",
                                categoryName
                            )
                    )
                },
            )
        }

        composable(CHAPTER_SCREEN_ROUTE) {
            DuaList(
                chapterId = it.arguments?.getString("chapterId")!!,
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
                },
                onNavigateToLicencesScreen = {
                    navController.navigate(
                        LICENCES_SCREEN_ROUTE
                    )
                },
                onNavigateToAboutScreen = {
                    navController.navigate(
                        ABOUT_SCREEN_ROUTE
                    )
                },
                onNavigateToWebViewScreen = { url: String ->
                    navController.navigate(
                        WEB_VIEW_SCREEN_ROUTE
                            .replace(
                                "{url}",
                                url
                            ),
                    )
                },
                onNavigateToDebugScreen = {
                    navController.navigate(
                        DEBUG_MODE
                    )
                },
                paddingValues = paddingValues
            )
        }
        composable(WEB_VIEW_SCREEN_ROUTE) {
            WebViewScreen(
                url = it.arguments?.getString("url")!!,
                paddingValues = paddingValues
            )
        }
        composable(ABOUT_SCREEN_ROUTE) {
            About(
                paddingValues
            )
            //navigate to the debug screen
            {
                navController.navigate(DEBUG_MODE)
            }
        }
        composable(LICENCES_SCREEN_ROUTE) {
            Licences(paddingValues)
        }
        composable(PRAYER_TIMES_SETTINGS_SCREEN_ROUTE) {
            PrayerTimesCustomizations(paddingValues)
        }

        composable(DEBUG_MODE) {
            DebugScreen(paddingValues)
        }
    }
}