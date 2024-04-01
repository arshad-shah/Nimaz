package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun CalenderMonth(
    monthState: @Composable (PaddingValues) -> Unit,
) {
        monthState(PaddingValues(bottom = 8.dp))
}