package com.arshadshah.nimaz.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import com.arshadshah.nimaz.ui.screens.introduction.IntroPage1
import com.arshadshah.nimaz.ui.theme.NimazTheme

class Introduction : ComponentActivity()
{

	override fun onCreate(savedInstanceState : Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContent {
			NimazTheme(
					 darkTheme = isSystemInDarkTheme() ,
					 dynamicColor = true ,
					 ThemeName = "DEFAULT"
					  ) {
				IntroPage1()
			}
		}
	}
}