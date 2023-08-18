package com.arshadshah.nimaz.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences


class RoutingActivity : ComponentActivity()
{

	override fun onCreate(savedInstanceState : Bundle?)
	{

		val splashScreen = installSplashScreen()
		splashScreen.setKeepOnScreenCondition { true }

		super.onCreate(savedInstanceState)

		val sharedPref = PrivateSharedPreferences(this.applicationContext)

		if (! LocalDataStore.isInitialized())
		{
			LocalDataStore.init(this)
			Log.d(
					 "Nimaz: Introduction Activity" ,
					 "onCreate:  called and local data store initialized"
				 )
		}

		if (! FirebaseLogger.isInitialized())
		{
			FirebaseLogger.init()
			Log.d(
					 "Nimaz: Introduction Activity" ,
					 "onCreate:  called and firebase logger initialized"
				 )
		}

		//get the first time flag
		val firstTime = sharedPref.getDataBoolean(AppConstants.IS_FIRST_INSTALL , true)

		if (firstTime)
		{
			Log.d(
					 AppConstants.SPLASH_SCREEN_TAG ,
					 "First time install launching setup activity"
				 )
			val intent = Intent(this.applicationContext , Introduction::class.java)
			startActivity(intent)
			finish()
		} else
		{
			Log.d(AppConstants.SPLASH_SCREEN_TAG , "Not first time returning to main activity")
			val intent = Intent(this.applicationContext , MainActivity::class.java)
			startActivity(intent)
			finish()
		}
	}
}