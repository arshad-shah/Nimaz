package com.arshadshah.nimaz.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import com.arshadshah.nimaz.constants.AppConstants.THEME_DEFAULT
import com.arshadshah.nimaz.ui.screens.introduction.IntroPage1
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.rememberSystemUiController

class Introduction : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemUiController = rememberSystemUiController()

            systemUiController.setSystemBarsColor(
                color = MaterialTheme.colorScheme.surface,
                darkIcons = !isSystemInDarkTheme(),
                isNavigationBarContrastEnforced = false
            )
            NimazTheme(
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = true,
                themeName = THEME_DEFAULT
            ) {
                IntroPage1()
            }
        }
    }
}