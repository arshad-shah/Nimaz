package com.arshadshah.nimaz.ui.components.calender.calenderday

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PrayerDot(isPrayed: Boolean?) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .background(
                color = if (isPrayed == true) MaterialTheme.colorScheme.primary
                else Color.Transparent,
                shape = CircleShape
            )
    )
}