package com.arshadshah.nimaz.ui.screens.introduction

import android.annotation.SuppressLint
import android.content.Intent.*
import androidx.compose.runtime.Composable
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.BatteryExemptionUI
import com.arshadshah.nimaz.ui.components.common.CalculationMethodUI
import com.arshadshah.nimaz.ui.components.common.NotificationScreenUI
import com.arshadshah.nimaz.ui.components.settings.LocationSettings

sealed class OnBoardingPage(
	val image : Int ,
	val title : String ,
	val description : String ,
	val extra : @Composable (() -> Unit)? = null ,
						   )
{

	object First : OnBoardingPage(
			image = R.drawable.praying ,
			title = "Assalamu alaykum" ,
			description = "A user-friendly, Beautifully designed app for Muslims, with accurate prayer times completely Ad-Free." ,
								 )

	object Second : OnBoardingPage(
			image = R.drawable.quran ,
			title = "Quran" ,
			description = "Quran with Urdu and English Translations, Audio Recitation, and Bookmarking Feature for Saving Ayahs to Read Later." ,
								  )

	object Third : OnBoardingPage(
			image = R.drawable.tracker_icon ,
			title = "Prayer and Fasting tracker" ,
			description = "Stay on track with your prayers and fasts with the Prayer and Fasting tracker." ,
								 )

	//the Notification permission page
	object Fourth : OnBoardingPage(
			image = R.drawable.adhan ,
			title = "Adhan Notifications" ,
			description = "Enable Adhan Notifications for Nimaz to get Prayer reminders in the form of Adhan." ,
			extra = {
				NotificationScreenUI()
			}
								  )

	object Fifth : OnBoardingPage(
			image = R.drawable.location_pin ,
			title = "Location" ,
			description = "Nimaz needs your location to get accurate prayer times and calculate Qibla direction. You can also use manual location." ,
			extra = {
				LocationSettings(isIntro = true)
			}
								 )

	object Sixth : OnBoardingPage(
			image = R.drawable.time_calculation ,
			title = "Calculation Method" ,
			description = "Nimaz uses the Muslim World League method by default in manual mode and uses altitude of the sun to calculate prayer times in automatic mode." ,
			extra = {
				CalculationMethodUI()
			}
								 )

	//a page to ask for the battery optimization exemption
	@SuppressLint("BatteryLife")
	object Seventh : OnBoardingPage(
			image = R.drawable.battery ,
			title = "Battery Exemption" ,
			description = "Battery optimization can cause issues with Adhan Notifications. Please exempt Nimaz from battery optimization." ,
			extra = {
				BatteryExemptionUI()
			}
								   )

	object Eighth : OnBoardingPage(
			image = R.drawable.check_mark ,
			title = "Onboarding Complete" ,
			description = "You are all set to use Nimaz. You can always change these settings later. I hope Nimaz helps you in your daily life and Kindly keep me and my family in your prayers." ,
			extra = {
			}
								  )
}