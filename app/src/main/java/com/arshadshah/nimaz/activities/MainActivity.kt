package com.arshadshah.nimaz.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.MAIN_ACTIVITY_TAG
import com.arshadshah.nimaz.ui.navigation.NavigationGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.rememberSystemUiController
import com.arshadshah.nimaz.utils.AutoLocationUtils
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sharedPref: PrivateSharedPreferences

    override fun onResume() {
        super.onResume()
        Log.d(MAIN_ACTIVITY_TAG, "onResume:  called")

        if (PrivateSharedPreferences(this).getDataBoolean(AppConstants.LOCATION_TYPE, true)) {
            if (!AutoLocationUtils.isInitialized()) {
                AutoLocationUtils.init(this)
                Log.d(MAIN_ACTIVITY_TAG, "onResume:  location is initialized")
            }
            if (!LocalDataStore.isInitialized()) {
                LocalDataStore.init(this)
                Log.d(MAIN_ACTIVITY_TAG, "onResume:  data store is initialized")
            }
            AutoLocationUtils.startLocationUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (PrivateSharedPreferences(this).getDataBoolean(AppConstants.LOCATION_TYPE, true)) {
            AutoLocationUtils.stopLocationUpdates()
            Log.d(MAIN_ACTIVITY_TAG, "onDestroy:  location is stopped")
        }
    }

    override fun onPause() {
        super.onPause()
        if (PrivateSharedPreferences(this).getDataBoolean(AppConstants.LOCATION_TYPE, true)) {
            AutoLocationUtils.stopLocationUpdates()
            Log.d(MAIN_ACTIVITY_TAG, "onPause:  location is stopped")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        this.actionBar?.hide()

        val splashScreen = installSplashScreen()

        if (!FirebaseLogger.isInitialized()) {
            FirebaseLogger.init()
            Log.d(MAIN_ACTIVITY_TAG, "onCreate:  called and firebase logger initialized")
        }
        splashScreen.setKeepOnScreenCondition {
            false
        }

        super.onCreate(savedInstanceState)

        val firstTime = sharedPref.getDataBoolean(AppConstants.IS_FIRST_INSTALL, true)

        //this is used to show the full activity on the screen
        setContent {

            val mainViewModel = viewModel(
                key = AppConstants.SETTINGS_VIEWMODEL_KEY,
                initializer = { SettingsViewModel(this@MainActivity) },
                viewModelStoreOwner = this@MainActivity
            )
            val darkTheme = mainViewModel.isDarkMode.collectAsState()
            val themeName = mainViewModel.theme.collectAsState()


            NimazTheme(
                darkTheme = darkTheme.value,
                themeName = themeName.value
            ) {
                val systemUiController = rememberSystemUiController()

                systemUiController.setStatusBarColor(
                    color = MaterialTheme.colorScheme.background,
                    darkIcons = !darkTheme.value,
                )
                systemUiController.setNavigationBarColor(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    darkIcons = !darkTheme.value,
                )
                val navController = rememberNavController()
                val route =
                    remember(navController) { mutableStateOf(navController.currentDestination?.route) }
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    route.value = destination.route
                }

                NavigationGraph(
                    navController = navController,
                    context = this@MainActivity,
                    isFirstInstall = firstTime
                )
            }
        }
    }
}
