package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun CalenderMonth(
    monthState: @Composable (PaddingValues) -> Unit,
) {
    monthState(PaddingValues(bottom = 8.dp))
}