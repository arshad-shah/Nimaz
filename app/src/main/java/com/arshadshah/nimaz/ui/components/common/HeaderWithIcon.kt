package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// create header component to be reused everywhere:
@Composable
fun HeaderWithIcon(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector? = null,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    iconColor: Color = contentColor
) {
    // Header with Calendar Icon
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(18.dp),
                    tint = iconColor
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}
