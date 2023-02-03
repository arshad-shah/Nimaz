package com.arshadshah.nimaz.activities

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.ui.navigation.SettingsNavGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme

class SettingsActivity : ComponentActivity()
{

	@RequiresApi(Build.VERSION_CODES.S)
	@OptIn(ExperimentalMaterial3Api::class)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContent {
			NimazTheme {
				val navController = rememberNavController()
				Scaffold(
						topBar = {
							TopAppBar(
									title = {
										val route =
											remember(navController) { mutableStateOf(navController.currentDestination?.route) }
										navController.addOnDestinationChangedListener { _ , destination , _ ->
											route.value = destination.route
										}
										Text(
												text = processPageTitle(route.value.toString()) ,
												style = MaterialTheme.typography.titleLarge
											)
									} ,
									navigationIcon = {
										IconButton(onClick = {
											//if nav destination is quran screen then finish activity
											if (navController.currentDestination?.route == "settings")
											{
												//set the result to ok
												setResult(Activity.RESULT_OK)
												finish()
											} else
											{
												navController.popBackStack()
											}
										}) {
											Icon(
													imageVector = Icons.Filled.ArrowBack ,
													contentDescription = "Back"
												)
										}
									}
									 )
						}

						) {
					SettingsNavGraph(navController = navController , it)
				}
			}
		}
	}

	fun processPageTitle(route : String) : String
	{
		return when (route)
		{
			"settings" -> "Settings"
			"about" -> "About"
			"PrayerTimesCustomizations" -> "Prayer Times Customization"
			"privacyPolicy" -> "Privacy Policy"
			else -> "Settings"
		}
	}
}