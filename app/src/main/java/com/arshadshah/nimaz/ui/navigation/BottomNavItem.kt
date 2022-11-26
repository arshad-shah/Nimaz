package com.arshadshah.nimaz.ui.navigation

import com.arshadshah.nimaz.R


sealed class BottomNavItem(var title : String , var icon : Int , var screen_route : String)
{

	object PrayerTimesScreen :
		BottomNavItem("Prayer Times" , R.drawable.ic_prayer , "prayer_times_screen")

	object QiblaScreen : BottomNavItem("Qibla" , R.drawable.compass , "qibla_screen")
	object QuranScreen : BottomNavItem("Quran" , R.drawable.ic_quran , "quran_screen")
	object SettingsScreen : BottomNavItem("Settings" , R.drawable.settings , "settings_screen")
}
