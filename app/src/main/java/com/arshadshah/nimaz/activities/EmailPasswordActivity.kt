package com.arshadshah.nimaz.activities

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.ui.navigation.AuthNavigationGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme

class EmailPasswordActivity : ComponentActivity()
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
						topBar ={
							TopAppBar(
									title = { Text("Login") },
									navigationIcon = {
										IconButton(onClick = {
											finish()
										}) {
											Icon(Icons.Filled.ArrowBack, contentDescription = null)
										}
									}
									 )
						}
						) {it
					AuthNavigationGraph(navController = navController, it)
				}
			}
		}
	}
}