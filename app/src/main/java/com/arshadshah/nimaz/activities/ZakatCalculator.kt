package com.arshadshah.nimaz.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import com.arshadshah.nimaz.ui.screens.ZakatCalculatorScreen
import com.arshadshah.nimaz.ui.theme.NimazTheme

class ZakatCalculator : ComponentActivity()
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
									title = {
										Text(text = "Zakat Calculator")
									},
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
					ZakatCalculatorScreen(it)
				}

			}
		}
	}
}