package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ContainedIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.tooling.preview.Preview

/**
 * Settings item with toggle switch.
 */
@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    description: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            ContainedIcon(
                imageVector = icon,
                size = NimazIconSize.SMALL,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                backgroundColor = iconTint.copy(alpha = 0.15f),
                iconColor = if (enabled) iconTint else iconTint.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.5f
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        NimazSwitch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled
        )
    }
}

/**
 * Settings item with navigation arrow.
 */
@Composable
fun SettingsNavigationItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    description: String? = null,
    value: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            ContainedIcon(
                imageVector = icon,
                size = NimazIconSize.SMALL,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                backgroundColor = iconTint.copy(alpha = 0.15f),
                iconColor = if (enabled) iconTint else iconTint.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.5f
                    )
                )
            }
        }

        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (enabled) 1f else 0.5f
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = if (enabled) 1f else 0.5f
            ),
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Settings item for displaying info (read-only).
 */
@Composable
fun SettingsInfoItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            ContainedIcon(
                imageVector = icon,
                size = NimazIconSize.SMALL,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                backgroundColor = iconTint.copy(alpha = 0.15f),
                iconColor = iconTint
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Settings action item (destructive action like sign out, delete).
 */
@Composable
fun SettingsActionItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val textColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) textColor else textColor.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f)
        )
    }
}

/**
 * Settings item with custom trailing content.
 */
@Composable
fun SettingsCustomItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            ContainedIcon(
                imageVector = icon,
                size = NimazIconSize.SMALL,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                backgroundColor = iconTint.copy(alpha = 0.15f),
                iconColor = iconTint
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        trailing()
    }
}

@Preview(showBackground = true, name = "Settings Switch Item")
@Composable
private fun SettingsSwitchItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSwitchItem(
                title = "Notifications",
                checked = true,
                onCheckedChange = {},
                icon = Icons.Default.Notifications,
                description = "Receive prayer time alerts"
            )
            SettingsSwitchItem(
                title = "Dark Mode",
                checked = false,
                onCheckedChange = {},
                icon = Icons.Default.DarkMode
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings Navigation Item")
@Composable
private fun SettingsNavigationItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsNavigationItem(
                title = "Language",
                onClick = {},
                icon = Icons.Default.Language,
                value = "English"
            )
            SettingsNavigationItem(
                title = "Prayer Settings",
                onClick = {},
                description = "Configure calculation method and adjustments"
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings Info Item")
@Composable
private fun SettingsInfoItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsInfoItem(
                title = "Version",
                value = "1.0.0",
                icon = Icons.Default.Info
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings Action Item")
@Composable
private fun SettingsActionItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsActionItem(
                title = "Clear Cache",
                onClick = {}
            )
            SettingsActionItem(
                title = "Delete Account",
                onClick = {},
                icon = Icons.Default.Delete,
                isDestructive = true
            )
        }
    }
}
