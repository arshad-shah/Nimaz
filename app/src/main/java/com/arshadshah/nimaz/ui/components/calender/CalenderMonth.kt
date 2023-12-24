package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CornerSize
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
    ElevatedCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        ),
        shape = MaterialTheme.shapes.extraLarge.copy(
            topStart = CornerSize(0.dp),
            topEnd = CornerSize(0.dp),
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp),
        ),
    ) {
        monthState(PaddingValues(bottom = 8.dp))
    }
}