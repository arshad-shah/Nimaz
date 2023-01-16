package com.arshadshah.nimaz.activities

import android.os.Build
import android.os.Bundle
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
import com.arshadshah.nimaz.ui.components.ui.quran.MoreMenu
import com.arshadshah.nimaz.ui.navigation.QuranNavGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme

class QuranActivity : ComponentActivity()
{

	@RequiresApi(Build.VERSION_CODES.S)
	@OptIn(ExperimentalMaterial3Api::class)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContent {
			NimazTheme {
				val navController = rememberNavController()
				val (menuOpen , setMenuOpen) = remember { mutableStateOf(false) }
				val route =
					remember(navController) { mutableStateOf(navController.currentDestination?.route) }
				Scaffold(
						topBar = {
							TopAppBar(
									title = {
										navController.addOnDestinationChangedListener { _ , destination , _ ->
											route.value = destination.route
										}
										if (route.value == "quran")
										{
											Text(text = "Quran")
										} else if (route.value == "ayatScreen/{number}/{isSurah}/{language}")
										{
											val number = remember(navController) {
												mutableStateOf(
														navController.currentBackStackEntry?.arguments?.getString(
																"number"
																												 )
															  )
											}
											val isSurah = remember(navController) {
												mutableStateOf(
														navController.currentBackStackEntry?.arguments?.getString(
																"isSurah"
																												 )
															  )
											}

											//get the isSurah and number and show the correct title
											navController.addOnDestinationChangedListener { _ , _ , _ ->
												number.value =
													navController.currentBackStackEntry?.arguments?.getString(
															"number"
																											 )
												isSurah.value =
													navController.currentBackStackEntry?.arguments?.getString(
															"isSurah"
																											 )
											}
											if (isSurah.value == "true")
											{
												Text(text = "Surah ${number.value}")
											} else
											{
												Text(text = "Juz ${number.value}")
											}
										}
									} ,
									navigationIcon = {
										IconButton(onClick = {
											//if nav destination is quran screen then finish activity
											if (navController.currentDestination?.route == "quran")
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
									actions = {
										//only show the menu button if the title is Quran
										if (route.value == "quran")
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

						) { it ->
					QuranNavGraph(navController = navController , it)
				}
			}
		}
	}
}