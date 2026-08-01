package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
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
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val clickModifier = when {
        !enabled -> Modifier
        onClick != null -> Modifier.clickable(onClick = onClick)
        checked != null && onCheckedChange != null -> Modifier.clickable { onCheckedChange(!checked) }
        else -> Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
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
            val resolvedTint =
                if (tintIcon) iconTint else MaterialTheme.colorScheme.onSurfaceVariant

            NimazIcon(
                imageVector = icon,
                contentDescription = null,
                type = NimazIconType.CONTAINED,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                tint = resolvedTint,
                containerColor = resolvedBackground,
                containerSize = 42.dp,
                iconSize = 22.dp,
                cornerRadius = 12.dp,
            )
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

        // `value` alone is the second line (above). When a caller supplies *both*, it means
        // two different things — a description and the current setting — so the value moves
        // to the trailing slot instead of being dropped on the floor, which is what
        // `subtitle ?: value` used to do: the Quran settings translation row passed
        // `value = translator` and only ever rendered the language.
        if (subtitle != null && value != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        when {
            trailingContent != null -> trailingContent()
            checked != null && onCheckedChange != null -> {
                // The enclosing Row already toggles via clickModifier, so the switch
                // itself defers its click (onCheckedChange = null) to avoid a double-fire.
                NimazSwitch(
                    checked = checked,
                    onCheckedChange = null
                )
            }

            showArrow || (onClick != null && checked == null) -> {
                NimazIcon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    size = NimazIconSize.MEDIUM
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
        Column {
            NimazSettingsItem(
                title = "Haptic Feedback",
                subtitle = "Vibration on interactions",
                checked = true,
                onCheckedChange = {}
            )
            NimazSettingsItem(
                title = "Haptic Feedback",
                subtitle = "Vibration on interactions",
                checked = false,
                onCheckedChange = {}
            )
        }
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
