package com.arshadshah.nimaz.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.ui.icons.Prayer
import compose.icons.FeatherIcons
import compose.icons.feathericons.Book
import compose.icons.feathericons.Compass
import compose.icons.feathericons.Menu
import compose.icons.feathericons.Settings


sealed class BottomNavItem(var title : String , var icon : ImageVector , var screen_route : String)
{

	//today
	object PrayerTimesScreen :
		BottomNavItem(AppConstants.PRAYER_TIMES_SCREEN_TITLE, Icons.Prayer , AppConstants.PRAYER_TIMES_SCREEN_ROUTE)
	object QiblaScreen : BottomNavItem( AppConstants.QIBLA_SCREEN_TITLE , FeatherIcons.Compass , AppConstants.QIBLA_SCREEN_ROUTE)
	object QuranScreen : BottomNavItem( AppConstants.QURAN_SCREEN_TITLE , FeatherIcons.Book , AppConstants.QURAN_SCREEN_ROUTE)
	object MoreScreen : BottomNavItem( AppConstants.MORE_SCREEN_TITLE , FeatherIcons.Menu , AppConstants.MORE_SCREEN_ROUTE)
	object SettingsScreen : BottomNavItem( AppConstants.SETTINGS_SCREEN_TITLE , FeatherIcons.Settings , AppConstants.SETTINGS_SCREEN_ROUTE)
}
