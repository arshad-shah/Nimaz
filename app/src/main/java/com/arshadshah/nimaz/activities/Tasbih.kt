package com.arshadshah.nimaz.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import com.arshadshah.nimaz.ui.screens.tasbih.TasbihDefaultScreen
import com.arshadshah.nimaz.ui.theme.NimazTheme

class Tasbih : ComponentActivity()
{

	@OptIn(ExperimentalMaterial3Api::class)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContent {
			NimazTheme {
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
						} ,
						) {
					it
					TasbihDefaultScreen(it)
				}
			}
		}
	}
}