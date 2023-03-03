package com.arshadshah.nimaz.ui.screens.introduction

import android.annotation.SuppressLint
import android.content.Intent.*
import androidx.compose.runtime.Composable
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.ui.intro.BatteryExemptionUI
import com.arshadshah.nimaz.ui.components.ui.intro.NotificationScreenUI
import com.arshadshah.nimaz.ui.components.ui.settings.LocationSettings

sealed class OnBoardingPage(
	val image : Int ,
	val title : String ,
	val description : String ,
	val extra : @Composable() (() -> Unit)? = null ,
						   )
{

	object First : OnBoardingPage(
			image = R.drawable.praying ,
			title = "Assalamu alaikum" ,
			description = "Nimaz is a muslim lifestyle companion app that helps you with your daily prayers." ,
								 )

	object Second : OnBoardingPage(
			image = R.drawable.time ,
			title = "Prayer Times" ,
			description = "Accurate prayer times for your location, Adhan notifications, and more." ,
								  )

	object Third : OnBoardingPage(
			image = R.drawable.quran ,
			title = "Quran" ,
			description = "Quran with urdu and english translations. You can also listen to the recitation of the Quran. You can also bookmark ayahs and read them later." ,
								 )

	object Fourth : OnBoardingPage(
			image = R.drawable.tracker_icon ,
			title = "Prayer and Fasting tracker" ,
			description = "You can track your prayers and fasts." ,
								  )

	//the Notification permission page
	object Fifth : OnBoardingPage(
			image = R.drawable.adhan ,
			title = "Adhan Notifications" ,
			description = "Enable Adhan Notifications for Nimaz to get Prayer alerts in the form of Adhan." ,
			extra = {
				NotificationScreenUI()
			}
								 )

	object Sixth : OnBoardingPage(
			image = R.drawable.location_pin ,
			title = "Location" ,
			description = "Nimaz needs your location to get accurate prayer times. You can also use manual location." ,
			extra = {
				LocationSettings(isIntro = true)
			}
								 )

	//a page to ask for the battery optimization exemption
	@SuppressLint("BatteryLife")
	object Seventh : OnBoardingPage(
			image = R.drawable.battery ,
			title = "Battery Exemption" ,
			description = "Nimaz needs to be exempted from battery optimization to show adhan notifications Correctly." ,
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