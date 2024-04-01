package com.arshadshah.nimaz.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants.THEME_DEFAULT
import com.arshadshah.nimaz.ui.screens.introduction.IntroPage1
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.ThemeChoser
import com.arshadshah.nimaz.ui.theme.rememberSystemUiController

class Introduction : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val darkTheme = remember {
                mutableStateOf(false)
            }
            val dynamicTheme = remember {
                mutableStateOf(false)
            }
            val themeName = remember {
                mutableStateOf(THEME_DEFAULT)
            }
            ThemeChoser(
                darkTheme,
                dynamicTheme,
                themeName
            )
            NimazTheme(
                darkTheme = darkTheme.value,
                dynamicColor = dynamicTheme.value,
                themeName = themeName.value
            ) {
                val systemUiController = rememberSystemUiController()

                systemUiController.setSystemBarsColor(
                    color = MaterialTheme.colorScheme.background,
                    darkIcons = !darkTheme.value,
                    isNavigationBarContrastEnforced = false
                )
                systemUiController.setNavigationBarColor(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    darkIcons = !darkTheme.value,
                )
                IntroPage1()
            }
        }
    }
}