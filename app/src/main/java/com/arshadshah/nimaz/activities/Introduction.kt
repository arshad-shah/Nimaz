package com.arshadshah.nimaz.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
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
					key = AppConstants.SETTINGS_VIEWMODEL_KEY ,
					initializer = { SettingsViewModel(this@Introduction) } ,
					viewModelStoreOwner = this@Introduction
											 )
			val themeState = remember {
				viewModelSettings.theme
			}.collectAsState()
			val isDarkTheme = remember {
				viewModelSettings.isDarkMode
			}.collectAsState()

			val darkTheme = remember {
				mutableStateOf(false)
			}
			val dynamicTheme = remember {
				mutableStateOf(false)
			}
			val themeName = remember {
				mutableStateOf("Default")
			}

			when (themeState.value)
			{
				"DYNAMIC" ->
				{
					dynamicTheme.value = true
					darkTheme.value = isDarkTheme.value
				}

				"SYSTEM" ->
				{
					dynamicTheme.value = true
					darkTheme.value = isSystemInDarkTheme()
					themeName.value = "Default"
				}

				"DEFAULT" ->
				{
					dynamicTheme.value = false
					darkTheme.value = isDarkTheme.value
					themeName.value = "Default"
				}

				"Raisin_Black" ->
				{
					dynamicTheme.value = false
					darkTheme.value = isDarkTheme.value
					themeName.value = "Raisin_Black"
				}

				"Dark_Red" ->
				{
					dynamicTheme.value = false
					darkTheme.value = isDarkTheme.value
					themeName.value = "Dark_Red"
				}

				"Dark_Liver" ->
				{
					dynamicTheme.value = false
					darkTheme.value = isDarkTheme.value
					themeName.value = "Dark_Liver"
				}

				"Rustic_brown" ->
				{
					dynamicTheme.value = false
					darkTheme.value = isDarkTheme.value
					themeName.value = "Rustic_brown"
				}
			}

			NimazTheme(
					darkTheme = darkTheme.value ,
					dynamicColor = dynamicTheme.value ,
					ThemeName = themeName.value
					  ) {
				IntroPage1()
			}
		}
	}
}