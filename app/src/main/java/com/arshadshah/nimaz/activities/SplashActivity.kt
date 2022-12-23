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
			delay(2000)
			val loginToken =
				NimazServicesImpl.login(AppConstants.USER_USERNAME , AppConstants.USER_PASSWORD)
			val sharedPref = PrivateSharedPreferences(this@SplashActivity)
			sharedPref.saveData(AppConstants.LOGIN_TOKEN , loginToken.token)
			Log.d("SplashActivity" , "onCreate: $loginToken")

			val intent = Intent(this@SplashActivity , MainActivity::class.java)
			startActivity(intent)
			finish()
		}
	}
}