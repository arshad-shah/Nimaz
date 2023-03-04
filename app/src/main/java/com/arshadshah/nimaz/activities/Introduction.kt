package com.arshadshah.nimaz.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.screens.introduction.IntroPage1
import com.arshadshah.nimaz.ui.theme.NimazTheme

class Introduction : ComponentActivity()
{

	@OptIn(ExperimentalMaterial3Api::class)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContent {
			val viewModelSettings = viewModel(
					key = "SettingsViewModel" ,
					initializer = { SettingsViewModel(this@Introduction) } ,
					viewModelStoreOwner = this as ComponentActivity
											 )
			val themeState = remember {
				viewModelSettings.theme
			}.collectAsState()

			val darkTheme = remember {
				mutableStateOf(false)
			}
			val dynamicTheme = remember {
				mutableStateOf(false)
			}

			when (themeState.value)
			{
				"DYNAMIC" ->
				{
					dynamicTheme.value = true
					darkTheme.value = isSystemInDarkTheme()
				}

				"SYSTEM" ->
				{
					dynamicTheme.value = false
					darkTheme.value = isSystemInDarkTheme()
				}

				"LIGHT" ->
				{
					dynamicTheme.value = false
					darkTheme.value = false
				}

				"DARK" ->
				{
					dynamicTheme.value = false
					darkTheme.value = true
				}
			}

			NimazTheme(
					darkTheme = darkTheme.value ,
					dynamicColor = dynamicTheme.value ,
					  ) {
				Scaffold {
					it
					IntroPage1()
				}
			}
		}
	}
}