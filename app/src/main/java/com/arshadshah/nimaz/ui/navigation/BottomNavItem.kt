package com.arshadshah.nimaz.ui.navigation

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants


sealed class BottomNavItem(
	var title : String ,
	var icon : Int ,
	var screen_route : String ,
	val iconDescription : String = "" ,
						  )
{

	object Dashboard : BottomNavItem(
			AppConstants.DASHBOARD_SCREEN_TITLE ,
			R.drawable.dashboard_icon ,
			AppConstants.DASHBOARD_SCREEN ,
			"Dashboard of nimaz"
									)

	//today
	object PrayerTimesScreen :
		BottomNavItem(
				AppConstants.PRAYER_TIMES_SCREEN_TITLE ,
				R.drawable.person_praying_icon ,
				AppConstants.PRAYER_TIMES_SCREEN_ROUTE ,
				"Prayer Times Screen"
					 )

	object QuranScreen : BottomNavItem(
			AppConstants.QURAN_SCREEN_TITLE ,
			R.drawable.quran_icon ,
			AppConstants.QURAN_SCREEN_ROUTE ,
			"Quran Screen"
									  )

	object MoreScreen : BottomNavItem(
			AppConstants.MORE_SCREEN_TITLE ,
			R.drawable.menu_burger_icon ,
			AppConstants.MORE_SCREEN_ROUTE ,
			"More Features Screen"
									 )

	object SettingsScreen : BottomNavItem(
			AppConstants.SETTINGS_SCREEN_TITLE ,
			R.drawable.settings_icon ,
			AppConstants.SETTINGS_SCREEN_ROUTE ,
			"Settings Screen"
										 )
}
