package com.arshadshah.nimaz.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.THEME_DARK_RED
import com.arshadshah.nimaz.constants.AppConstants.THEME_DEFAULT
import com.arshadshah.nimaz.constants.AppConstants.THEME_RAISIN_BLACK
import com.arshadshah.nimaz.constants.AppConstants.THEME_RUSTIC_BROWN
import com.arshadshah.nimaz.constants.AppConstants.THEME_SYSTEM
import com.arshadshah.nimaz.viewModel.SettingsViewModel

@Composable
fun ThemeChoser(
    darkTheme: MutableState<Boolean>,
    dynamicTheme: MutableState<Boolean>,
    themeName: MutableState<String>,
) {
    val context = LocalContext.current
    val viewModelSettings = viewModel(
        key = AppConstants.SETTINGS_VIEWMODEL_KEY,
        initializer = { SettingsViewModel(context) },
        viewModelStoreOwner = context as ComponentActivity
    )
    val themeState = remember {
        viewModelSettings.theme
    }.collectAsState()
    val isDarkTheme = remember {
        viewModelSettings.isDarkMode
    }.collectAsState()

    when (themeState.value) {
        THEME_SYSTEM -> {
            dynamicTheme.value = true
            darkTheme.value = isSystemInDarkTheme()
            themeName.value = THEME_DEFAULT
        }

        THEME_DEFAULT -> {
            dynamicTheme.value = false
            darkTheme.value = isDarkTheme.value
            themeName.value = THEME_DEFAULT
        }

        THEME_RAISIN_BLACK -> {
            dynamicTheme.value = false
            darkTheme.value = isDarkTheme.value
            themeName.value = THEME_RAISIN_BLACK
        }

        THEME_DARK_RED -> {
            dynamicTheme.value = false
            darkTheme.value = isDarkTheme.value
            themeName.value = THEME_DARK_RED
        }

        THEME_RUSTIC_BROWN -> {
            dynamicTheme.value = false
            darkTheme.value = isDarkTheme.value
            themeName.value = THEME_RUSTIC_BROWN
        }
    }
}