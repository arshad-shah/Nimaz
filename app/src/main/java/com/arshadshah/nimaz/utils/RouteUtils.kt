package com.arshadshah.nimaz.utils

import android.os.Bundle
import com.arshadshah.nimaz.constants.AppConstants

object RouteUtils {
    /**
     * Check and then return a route string to be displayed in the top bar.
     * @param route the current route
     * @param navArguments the arguments of the current route if any
     * @return route string to be displayed in the top bar.
     */
    fun processPageTitle(route: String, navArguments: Bundle?): String {
        return when (route) {
            AppConstants.SETTINGS_SCREEN_ROUTE -> "Settings"
            AppConstants.ABOUT_SCREEN_ROUTE -> "About"
            AppConstants.PRAYER_TIMES_SETTINGS_SCREEN_ROUTE -> "Prayer Times Settings"
            AppConstants.QURAN_SCREEN_ROUTE -> "Quran"

            AppConstants.SHAHADAH_SCREEN_ROUTE -> "Shahadah"
            AppConstants.CHAPTERS_SCREEN_ROUTE -> "Chapters"

            AppConstants.CHAPTER_SCREEN_ROUTE -> {
                //check if the url of the route is for surah or juz using the nav controller
                val chapterId =
                    navArguments?.getString("chapterId")
                "Chapter $chapterId"
            }

            AppConstants.TASBIH_SCREEN_ROUTE -> "Tasbih"
            AppConstants.NAMESOFALLAH_SCREEN_ROUTE -> "Allah"
            AppConstants.PRAYER_TRACKER_SCREEN_ROUTE -> "Trackers"
            AppConstants.CALENDER_SCREEN_ROUTE -> "Calender"
            AppConstants.QIBLA_SCREEN_ROUTE -> "Qibla"
            AppConstants.TASBIH_LIST_SCREEN -> "Tasbih List"

            AppConstants.WEB_VIEW_SCREEN_ROUTE -> {
                //check if the url of the route is privacy_policy using the nav controller
                when (navArguments?.getString("url")) {
                    "privacy_policy" -> {
                        "Privacy Policy"
                    }

                    "help" -> {
                        "Help"
                    }

                    else -> {
                        "Terms and Conditions"
                    }
                }
            }

            AppConstants.LICENCES_SCREEN_ROUTE -> "Open Source Libraries"
            AppConstants.DEBUG_MODE -> "Debug Tools"
            AppConstants.CATEGORY_SCREEN_ROUTE -> "Hisnul Muslim"

            else -> ""
        }
    }

    /**
     * Check if the current route is allowed to show the bottom navigation bar
     * @param route the current route
     * @return true if the current route is allowed to show the bottom navigation bar, false otherwise
     * */
    fun checkRoute(route: String): Boolean {
        val routeToCheck = listOf(
            AppConstants.SETTINGS_SCREEN_ROUTE,
            AppConstants.ABOUT_SCREEN_ROUTE,
            AppConstants.PRAYER_TIMES_SETTINGS_SCREEN_ROUTE,
            AppConstants.QURAN_SCREEN_ROUTE,
            AppConstants.QURAN_AYA_SCREEN_ROUTE,
            AppConstants.SHAHADAH_SCREEN_ROUTE,
            AppConstants.CHAPTERS_SCREEN_ROUTE,
            AppConstants.CHAPTER_SCREEN_ROUTE,
            AppConstants.TASBIH_SCREEN_ROUTE,
            AppConstants.NAMESOFALLAH_SCREEN_ROUTE,
            AppConstants.PRAYER_TRACKER_SCREEN_ROUTE,
            AppConstants.CALENDER_SCREEN_ROUTE,
            AppConstants.QIBLA_SCREEN_ROUTE,
            AppConstants.TASBIH_LIST_SCREEN,
            AppConstants.MY_QURAN_SCREEN_ROUTE,
            AppConstants.WEB_VIEW_SCREEN_ROUTE,
            AppConstants.LICENCES_SCREEN_ROUTE,
            AppConstants.DEBUG_MODE,
            AppConstants.CATEGORY_SCREEN_ROUTE,
            AppConstants.DASHBOARD_SCREEN
        )
        //if the route is in the list then return true
        return routeToCheck.contains(route)
    }
}