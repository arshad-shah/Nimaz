package com.arshadshah.nimaz.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.network.NimazServicesImpl
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity()
{

	override fun onCreate(savedInstanceState : Bundle?)
	{

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
		{
			val splashScreen = installSplashScreen()
			splashScreen.setKeepOnScreenCondition { true }
		}

		super.onCreate(savedInstanceState)
		lifecycleScope.launchWhenCreated {
			delay(AppConstants.SPLASH_SCREEN_DURATION)
			val sharedPref = PrivateSharedPreferences(this@SplashActivity)

			LocalDataStore.init(this@SplashActivity)

			//get the first time flag
			val firstTime = sharedPref.getDataBoolean(AppConstants.IS_FIRST_INSTALL , true)

			if (firstTime)
			{
				Log.d(AppConstants.SPLASH_SCREEN_TAG, "First time install launching setup activity")
				val intent = Intent(this@SplashActivity , Introduction::class.java)
				startActivity(intent)
				finish()
			} else
			{
				Log.d(AppConstants.SPLASH_SCREEN_TAG, "Not first time returning to main activity")
				val intent = Intent(this@SplashActivity , MainActivity::class.java)
				startActivity(intent)
				finish()
			}
		}
	}
}