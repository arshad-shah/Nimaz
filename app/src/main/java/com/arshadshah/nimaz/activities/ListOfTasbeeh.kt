package com.arshadshah.nimaz.activities

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
import com.arshadshah.nimaz.ui.navigation.TasbihListNavGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme

class ListOfTasbeeh : ComponentActivity()
{

	@RequiresApi(Build.VERSION_CODES.S)
	@OptIn(ExperimentalMaterial3Api::class)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContent {
			NimazTheme {
				val navController = rememberNavController()
				val route =
					remember(navController) { mutableStateOf(navController.currentDestination?.route) }
				Scaffold(
						topBar = {
							TopAppBar(
									title = {
											navController.addOnDestinationChangedListener { _ , destination , _ ->
												route.value = destination.route
											}
											//if the route is chapterList then show the title "Categories" else show the title "Dua List"
											if (route.value == "chapterList")
											{
												Text(text = "Categories")
											}
											else
											{
												Text(text = "Dua List")
											}
									} ,
									navigationIcon = {
										IconButton(onClick = {
											//if nav destination is quran screen then finish activity
											if (navController.currentDestination?.route == "chapterList")
											{
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
									} ,
								  )
						}

						) {it
					TasbihListNavGraph(navController = navController , paddingValues = it )
				}
			}
		}
	}
}