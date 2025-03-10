package com.arshadshah.nimaz.ui.components.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.ArrowRight
import com.arshadshah.nimaz.ui.components.common.HeaderWithIcon
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun SettingsPrayerTimesSection(
    onNavigateToPrayerSettings: () -> Unit,
    onResetAlarms: () -> Unit,
    onTestAlarm: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            HeaderWithIcon(
                icon = ImageVector.vectorResource(id = R.drawable.alarm_clock_icon),
                title = "Prayer Times",
                contentDescription = "Prayer Times Icon",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Settings Button
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPrayerSettings() }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.settings_sliders_icon),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxSize(),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Configure Prayer Times",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Adjust calculation methods and preferences",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ArrowRight()
                }
            }

            // Quick Actions
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    //quick actions header
                    HeaderWithIcon(
                        icon = ImageVector.vectorResource(id = R.drawable.settings_icon),
                        title = "Quick Actions",
                        contentDescription = "Quick Actions Icon",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    QuickActionButton(
                        icon = R.drawable.alarm_clock_icon,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Reset Prayer Alarms",
                        subtitle = "Re-configure all prayer time notifications",
                        onClick = onResetAlarms
                    )

                    QuickActionButton(
                        icon = R.drawable.alarm_set_icon,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Test Notification",
                        subtitle = "Send a test notification in 10 seconds",
                        onClick = onTestAlarm
                    )

                    QuickActionButton(
                        icon = R.drawable.settings_icon,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Notification Settings",
                        subtitle = "Configure system notification settings",
                        onClick = onOpenNotificationSettings,
                        showArrow = true
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    @DrawableRes icon: Int,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showArrow: Boolean = false
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showArrow) {
                ArrowRight()
            }
        }
    }
}


@Preview
@Composable
fun SettingsPrayerTimesSectionPreview() {
    NimazTheme {
        SettingsPrayerTimesSection(
            onNavigateToPrayerSettings = {},
            onResetAlarms = {},
            onTestAlarm = {},
            onOpenNotificationSettings = {}
        )
    }
}

@Preview
@Composable
fun QuickActionButtonPreview() {
    NimazTheme {
        QuickActionButton(
            icon = R.drawable.alarm_clock_icon,
            iconTint = MaterialTheme.colorScheme.primary,
            title = "Reset Prayer Alarms",
            subtitle = "Re-configure all prayer time notifications",
            onClick = {}
        )
    }
}