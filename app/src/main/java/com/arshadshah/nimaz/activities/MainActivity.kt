package com.arshadshah.nimaz.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.APP_UPDATE_REQUEST_CODE
import com.arshadshah.nimaz.constants.AppConstants.DASHBOARD_SCREEN
import com.arshadshah.nimaz.constants.AppConstants.MAIN_ACTIVITY_TAG
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TIMES_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.SCREEN_ANIMATION_DURATION
import com.arshadshah.nimaz.constants.AppConstants.SCREEN_ANIMATION_DURATION_Exit
import com.arshadshah.nimaz.constants.AppConstants.THEME_DEFAULT
import com.arshadshah.nimaz.services.UpdateService
import com.arshadshah.nimaz.ui.components.common.CustomTopBar
import com.arshadshah.nimaz.ui.navigation.BottomNavigationBar
import com.arshadshah.nimaz.ui.navigation.NavigationGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.ThemeChoser
import com.arshadshah.nimaz.ui.theme.rememberSystemUiController
import com.arshadshah.nimaz.utils.AutoLocationUtils
import com.arshadshah.nimaz.utils.CustomAnimation
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.NetworkChecker
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.RouteUtils.checkRoute
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    //on resume to check if the update is stalled
    override fun onResume() {
        super.onResume()
        Log.d(MAIN_ACTIVITY_TAG, "onResume:  called")

        if (PrivateSharedPreferences(this).getDataBoolean(AppConstants.LOCATION_TYPE, true)) {
            if (!AutoLocationUtils.isInitialized()) {
                AutoLocationUtils.init(this)
                Log.d(MAIN_ACTIVITY_TAG, "onResume:  location is initialized")
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

        if (!LocalDataStore.isInitialized()) {
            LocalDataStore.init(this)
            Log.d(MAIN_ACTIVITY_TAG, "onCreate:  called and local data store initialized")
        }

        if (!FirebaseLogger.isInitialized()) {
            FirebaseLogger.init()
            Log.d(MAIN_ACTIVITY_TAG, "onCreate:  called and firebase logger initialized")
        }

        super.onCreate(savedInstanceState)

        //this is used to show the full activity on the screen
        setContent {

            val darkTheme = remember {
                mutableStateOf(false)
            }
            val dynamicTheme = remember {
                mutableStateOf(false)
            }
            val themeName = remember {
                mutableStateOf(THEME_DEFAULT)
            }

            ThemeChoser(
                darkTheme,
                dynamicTheme,
                themeName
            )


            NimazTheme(
                darkTheme = darkTheme.value,
                dynamicColor = dynamicTheme.value,
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

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                //check for network connection
                val networkConnection =
                    remember { mutableStateOf(NetworkChecker().networkCheck(this@MainActivity)) }

                LaunchedEffect(networkConnection.value) {
                    if (!networkConnection.value) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "No internet connection",
                                duration = SnackbarDuration.Indefinite,
                                withDismissAction = true
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.testTag("mainActivity"),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        CustomTopBar(
                            route = route,
                            navController = navController,
                            context = this@MainActivity
                        )
                    },
                    bottomBar = {
                        AnimatedVisibility(
                            visible = if (route.value.toString() === DASHBOARD_SCREEN || route.value.toString() === PRAYER_TIMES_SCREEN_ROUTE) true else !checkRoute(
                                route.value.toString()
                            ),
                            enter = CustomAnimation.fadeIn(duration = SCREEN_ANIMATION_DURATION),
                            exit = CustomAnimation.fadeOut(duration = SCREEN_ANIMATION_DURATION_Exit),
                            content = {
                                BottomNavigationBar(navController = navController)
                            })

                    }
                ) {
                    NavigationGraph(navController = navController, it, context = this@MainActivity)
                }
            }
        }
    }
}
