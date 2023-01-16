package com.arshadshah.nimaz.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import com.arshadshah.nimaz.ui.screens.introduction.IntroPage1
import com.arshadshah.nimaz.ui.theme.NimazTheme

class Introduction : ComponentActivity()
{

	@OptIn(ExperimentalMaterial3Api::class)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContent {
			NimazTheme {
				Scaffold {
					it
					IntroPage1()
				}
			}
		}
	}
}