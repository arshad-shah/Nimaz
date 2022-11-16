package com.arshadshah.nimaz.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arshadshah.nimaz.ui.features.screens.introduction.IntroPage1
import com.arshadshah.nimaz.ui.theme.NimazTheme

class Introduction : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NimazTheme {
                IntroPage1()
            }
        }
    }
}