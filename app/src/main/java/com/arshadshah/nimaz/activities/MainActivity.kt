package com.arshadshah.nimaz.activities

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.constants.AppConstants.ABOUT_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.APP_UPDATE_REQUEST_CODE
import com.arshadshah.nimaz.constants.AppConstants.MAIN_ACTIVITY_TAG
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TIMES_SETTINGS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QURAN_AYA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QURAN_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.SETTINGS_SCREEN_ROUTE
import com.arshadshah.nimaz.ui.components.ui.quran.MoreMenu
import com.arshadshah.nimaz.ui.navigation.BottomNavigationBar
import com.arshadshah.nimaz.ui.navigation.NavigationGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.widgets.Nimaz
import com.arshadshah.nimaz.widgets.updateAppWidget
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : ComponentActivity()
{

	//on resume to check if the update is stalled
	override fun onResume()
	{
		super.onResume()
		Log.d(MAIN_ACTIVITY_TAG , "onResume:  called")
		val appUpdateManager = AppUpdateManagerFactory.create(this)
		appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
			if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
			{
				Log.d(MAIN_ACTIVITY_TAG , "onResume:  update is stalled")
				appUpdateManager.startUpdateFlowForResult(
						appUpdateInfo ,
						AppUpdateType.IMMEDIATE ,
						this ,
						APP_UPDATE_REQUEST_CODE
														 )
			}
		}
	}

	@OptIn(ExperimentalMaterial3Api::class)
	@RequiresApi(Build.VERSION_CODES.S)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		this.actionBar?.hide()

		LocalDataStore.init(this@MainActivity)
		Log.d(MAIN_ACTIVITY_TAG , "onCreate:  called and local data store initialized")

		val appWidgetManager = AppWidgetManager.getInstance(this)
		val appWidgetIds : IntArray = appWidgetManager.getAppWidgetIds(
				ComponentName(
						this ,
						Nimaz::class.java
							 )
																	  )
		for (appWidgetId in appWidgetIds)
		{
			updateAppWidget(this , appWidgetManager , appWidgetId)
		}
		Log.d(MAIN_ACTIVITY_TAG , "onCreate:  app widget updated")

		val appUpdateManager = AppUpdateManagerFactory.create(this)

		// Returns an intent object that you use to check for an update.
		val appUpdateInfoTask = appUpdateManager.appUpdateInfo

		// Checks that the platform will allow the specified type of update.
		appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
			if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
				&& appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
			)
			{
				Log.d(
						MAIN_ACTIVITY_TAG ,
						"onCreate:  update is available and immediate is allowed"
					 )
				// Request the update.
				appUpdateManager.startUpdateFlowForResult(
						appUpdateInfo ,
						AppUpdateType.IMMEDIATE ,
						this ,
						APP_UPDATE_REQUEST_CODE
														 )

			}
		}
		super.onCreate(savedInstanceState)

		//this is used to show the full activity on the screen
		setContent {
			NimazTheme {
				val navController = rememberNavController()
				val route =
					remember(navController) { mutableStateOf(navController.currentDestination?.route) }
				navController.addOnDestinationChangedListener { _ , destination , _ ->
					route.value = destination.route
				}
				val (menuOpen , setMenuOpen) = remember { mutableStateOf(false) }
				Scaffold(
						topBar = {
								 //only add this if the route is not BottomNavItem.SettingsScreen.route
								 if (checkRoute(route.value.toString()))
								 {
									 TopAppBar(
											 title = {
												 Text(
														 text = processPageTitle(route.value.toString()),
														 style = MaterialTheme.typography.titleMedium
													  )
											 },
											 navigationIcon = {
												 IconButton(onClick = { navController.navigateUp() }) {
													 Icon(
															 imageVector = Icons.Default.ArrowBack ,
															 contentDescription = "Back"
														  )
												 }
											 },
											 actions = {
												 //only show the menu button if the title is Quran
												 if (route.value == QURAN_SCREEN_ROUTE)
												 {
													 //open the menu
													 IconButton(onClick = { setMenuOpen(true) }) {
														 Icon(
																 imageVector = Icons.Filled.MoreVert ,
																 contentDescription = "Menu"
															 )
													 }
													 MoreMenu(
															 menuOpen = menuOpen ,
															 setMenuOpen = setMenuOpen
															 )
												 }
											 }
											  )
								 }
						},
						bottomBar = {
							//if the route is BottomNavItem.SettingsScreen then dont show this
							if (!checkRoute(route.value.toString()))
							{
								BottomNavigationBar(navController = navController)
							}
						}
						) { it ->
					NavigationGraph(navController = navController , it)
				}
			}
		}
	}
	fun processPageTitle(route : String) : String
	{
		return when (route)
		{
			SETTINGS_SCREEN_ROUTE -> "Settings"
			ABOUT_SCREEN_ROUTE -> "About"
			PRAYER_TIMES_SETTINGS_SCREEN_ROUTE -> "Prayer Times Customization"
			QURAN_SCREEN_ROUTE -> "Quran"
			QURAN_AYA_SCREEN_ROUTE -> "Aya"
			else -> "Settings"
		}
	}

	//a fuinction to check a givenm route and return a boolean
	fun checkRoute(route : String) : Boolean
	{
		val routeToCheck = listOf(
				SETTINGS_SCREEN_ROUTE ,
				ABOUT_SCREEN_ROUTE ,
				PRAYER_TIMES_SETTINGS_SCREEN_ROUTE,
				QURAN_SCREEN_ROUTE,
				QURAN_AYA_SCREEN_ROUTE
								 )
		//if the route is in the list then return true
		return routeToCheck.contains(route)
	}

}
