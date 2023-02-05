package com.arshadshah.nimaz.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences


class RoutingActivity : ComponentActivity()
{

	override fun onCreate(savedInstanceState : Bundle?)
	{

		val splashScreen = installSplashScreen()
		splashScreen.setKeepOnScreenCondition { true }

		super.onCreate(savedInstanceState)

		val sharedPref = PrivateSharedPreferences(this@RoutingActivity)

		LocalDataStore.init(this@RoutingActivity)

		//get the first time flag
		val firstTime = sharedPref.getDataBoolean(AppConstants.IS_FIRST_INSTALL , true)

		if (firstTime)
		{
			Log.d(
					AppConstants.SPLASH_SCREEN_TAG ,
					"First time install launching setup activity"
				 )
			val intent = Intent(this@RoutingActivity , Introduction::class.java)
			startActivity(intent)
			finish()
		} else
		{
			Log.d(AppConstants.SPLASH_SCREEN_TAG , "Not first time returning to main activity")
			val intent = Intent(this@RoutingActivity , MainActivity::class.java)
			startActivity(intent)
			finish()
		}
	}
}