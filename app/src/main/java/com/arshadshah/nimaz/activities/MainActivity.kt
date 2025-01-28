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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.MAIN_ACTIVITY_TAG
import com.arshadshah.nimaz.ui.navigation.NavigationGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.rememberSystemUiController
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.ThemeDataStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sharedPref: PrivateSharedPreferences

    @Inject
    lateinit var themeDataStore: ThemeDataStore

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        this.actionBar?.hide()

        val splashScreen = installSplashScreen()

        if (!FirebaseLogger.isInitialized()) {
            FirebaseLogger.init()
            Log.d(MAIN_ACTIVITY_TAG, "onCreate: called and firebase logger initialized")
        }

        splashScreen.setKeepOnScreenCondition { false }

        super.onCreate(savedInstanceState)

        val firstTime = sharedPref.getDataBoolean(AppConstants.IS_FIRST_INSTALL, true)

        setContent {
            // Collect theme preferences directly from ThemeDataStore
            val isDarkMode by themeDataStore.darkModeFlow.collectAsState(initial = false)
            val currentTheme by themeDataStore.themeFlow.collectAsState(initial = AppConstants.THEME_SYSTEM)

            NimazTheme(
                darkTheme = isDarkMode,
                themeName = currentTheme
            ) {
                val systemUiController = rememberSystemUiController()

                systemUiController.setStatusBarColor(
                    color = MaterialTheme.colorScheme.background,
                    darkIcons = !isDarkMode,
                )
                systemUiController.setNavigationBarColor(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    darkIcons = !isDarkMode,
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
                    isFirstInstall = firstTime,
                )
            }
        }
    }
}