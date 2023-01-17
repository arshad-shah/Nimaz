package com.arshadshah.nimaz.activities

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.ui.navigation.BottomNavigationBar
import com.arshadshah.nimaz.ui.navigation.NavigationGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.widgets.Nimaz
import com.arshadshah.nimaz.widgets.updateAppWidget
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : ComponentActivity()
{

	val REQUEST_CODE = 100

	//on resume to check if the update is stalled
	override fun onResume()
	{
		super.onResume()
		val appUpdateManager = AppUpdateManagerFactory.create(this)
		appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
			if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
			{
				appUpdateManager.startUpdateFlowForResult(
					appUpdateInfo,
					AppUpdateType.IMMEDIATE,
					this,
					REQUEST_CODE
				)
			}
		}
	}

	@OptIn(ExperimentalMaterial3Api::class)
	@RequiresApi(Build.VERSION_CODES.S)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		this.actionBar?.hide()

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

		val appUpdateManager = AppUpdateManagerFactory.create(this)

		// Returns an intent object that you use to check for an update.
		val appUpdateInfoTask = appUpdateManager.appUpdateInfo

		// Checks that the platform will allow the specified type of update.
		appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
			if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
				&& appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
			) {
				// Request the update.
				appUpdateManager.startUpdateFlowForResult(
						appUpdateInfo ,
						AppUpdateType.IMMEDIATE ,
						this ,
						REQUEST_CODE
														 )

			}
		}
		super.onCreate(savedInstanceState)
		//this is used to show the full activity on the screen
		setContent {
			NimazTheme {
				val navController = rememberNavController()
				Location().getAutomaticLocation(this)

				Scaffold(
						bottomBar = { BottomNavigationBar(navController = navController) }
						) { it ->
					NavigationGraph(navController = navController , it)
				}
			}
		}
	}
}

//this is a main component to show everything else
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun DefaultPreview(modifier : Modifier = Modifier)
{
	val navController = rememberNavController()
	Scaffold(
			bottomBar = { BottomNavigationBar(navController = navController) }
			) { it ->
		NavigationGraph(navController = navController , it)
	}
}

