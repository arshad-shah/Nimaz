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
				Scaffold(
						topBar = {
							TopAppBar(
									title = { Text(text = "Tasbih") } ,
									navigationIcon = {
										IconButton(onClick = {
											finish()
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