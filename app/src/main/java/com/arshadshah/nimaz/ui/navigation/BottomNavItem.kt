package com.arshadshah.nimaz.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import com.arshadshah.nimaz.ui.components.ui.icons.Prayer
import compose.icons.FeatherIcons
import compose.icons.feathericons.Book
import compose.icons.feathericons.Calendar
import compose.icons.feathericons.Compass
import compose.icons.feathericons.Settings


sealed class BottomNavItem(var title : String , var icon : ImageVector , var screen_route : String)
{
	//today
	object TodayScreen : BottomNavItem("Today" , FeatherIcons.Calendar , "today")
	object PrayerTimesScreen :
		BottomNavItem("Prayer" , Icons.Prayer , "prayer_times_screen")
	object QiblaScreen : BottomNavItem("Qibla" , FeatherIcons.Compass , "qibla_screen")
	object QuranScreen : BottomNavItem("Quran" , FeatherIcons.Book , "quran_screen")
	object SettingsScreen : BottomNavItem("Settings" , FeatherIcons.Settings , "settings_screen")
}
