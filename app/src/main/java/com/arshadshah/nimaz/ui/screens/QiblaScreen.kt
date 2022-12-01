package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.arshadshah.nimaz.ui.components.bLogic.compass.Dial

@Composable
fun QiblaScreen(paddingValues : PaddingValues)
{
	Text(text = "Qibla Screen")
	Dial()
}