package com.arshadshah.nimaz.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import com.arshadshah.nimaz.ui.theme.NimazTheme

class AyaActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //retrieve the number from the intent
        val number = intent.getStringExtra("number")
        //get isSurah from the intent
        val isSurah = intent.getBooleanExtra("isSurah", false)
        //get is English from the intent
        val isEnglish = intent.getBooleanExtra("isEnglish", false)
        setContent {
            NimazTheme {

            }
        }
    }
}