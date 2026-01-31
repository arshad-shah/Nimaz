package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@Composable
fun NimazSettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBackground: Color = Color.Unspecified,
    tintIcon: Boolean = false,
    value: String? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showArrow: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val clickModifier = when {
        onClick != null -> Modifier.clickable(onClick = onClick)
        checked != null && onCheckedChange != null -> Modifier.clickable { onCheckedChange(!checked) }
        else -> Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            val resolvedBackground = when {
                iconBackground != Color.Unspecified -> iconBackground
                tintIcon -> iconTint.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val resolvedTint = if (tintIcon) iconTint else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(resolvedBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(15.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val displaySubtitle = subtitle ?: value
            if (displaySubtitle != null) {
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            trailingContent != null -> trailingContent()
            checked != null && onCheckedChange != null -> {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
            showArrow || (onClick != null && checked == null) -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazSettingsItem - Navigation")
@Composable
private fun NimazSettingsItemNavPreview() {
    NimazTheme {
        NimazSettingsItem(
            title = "Calculation Method",
            subtitle = "Prayer time calculation settings",
            icon = Icons.Default.Notifications,
            iconTint = MaterialTheme.colorScheme.primary,
            iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazSettingsItem - Toggle")
@Composable
private fun NimazSettingsItemTogglePreview() {
    NimazTheme {
        NimazSettingsItem(
            title = "Haptic Feedback",
            subtitle = "Vibration on interactions",
            checked = true,
            onCheckedChange = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazSettingsItem - With Value")
@Composable
private fun NimazSettingsItemValuePreview() {
    NimazTheme {
        NimazSettingsItem(
            title = "High Latitude Method",
            value = "Middle of the Night",
            onClick = {}
        )
    }
}
