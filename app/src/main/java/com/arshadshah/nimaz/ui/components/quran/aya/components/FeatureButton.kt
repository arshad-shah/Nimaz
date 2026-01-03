package com.arshadshah.nimaz.ui.components.quran.aya.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer

/**
 * Feature action button following design system.
 * Uses FilledTonalIconButton with 36dp size.
 *
 * @param isActive Whether the feature is currently active (e.g., bookmarked)
 * @param activeContainerColor Color to use when active
 */
@Composable
fun FeatureButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    isActive: Boolean = false,
    activeContainerColor: androidx.compose.ui.graphics.Color? = null,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.size(36.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (isActive && activeContainerColor != null)
                activeContainerColor.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            contentColor = if (isActive && activeContainerColor != null)
                activeContainerColor
            else
                MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier
                .size(18.dp)
                .placeholder(
                    visible = isLoading,
                    highlight = PlaceholderHighlight.shimmer()
                )
        )
    }
}