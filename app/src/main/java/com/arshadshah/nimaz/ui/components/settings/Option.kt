package com.arshadshah.nimaz.ui.components.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun Option(
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    icon: Painter,
    iconDescription: String,
    testTag: String,
    action: @Composable (() -> Unit)? = null,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 32.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .testTag(testTag),
    ) {
        SettingsMenuLink(
            title = title,
            subtitle = subtitle,
            onClick = {
                onClick()
            },
            icon = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = icon,
                    contentDescription = iconDescription
                )
            },
            action = action
        )
    }
}