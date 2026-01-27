package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val notificationState by viewModel.notificationState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val reminderOptions = listOf(
        "5 minutes before" to 5,
        "10 minutes before" to 10,
        "15 minutes before" to 15,
        "30 minutes before" to 30
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Notifications",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Master Toggle
            item {
                NotificationToggleCard(
                    title = "Prayer Notifications",
                    subtitle = "Enable notifications for prayer times",
                    icon = Icons.Default.Notifications,
                    isEnabled = notificationState.notificationsEnabled,
                    onToggle = { viewModel.onEvent(SettingsEvent.SetNotificationsEnabled(!notificationState.notificationsEnabled)) }
                )
            }

            // Prayer-specific toggles
            if (notificationState.notificationsEnabled) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = "Individual Prayers")
                }

                item {
                    PrayerNotificationCard(
                        prayerName = "Fajr",
                        isEnabled = notificationState.fajrNotification,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetPrayerNotification("fajr", !notificationState.fajrNotification)) }
                    )
                }

                item {
                    PrayerNotificationCard(
                        prayerName = "Sunrise",
                        isEnabled = notificationState.sunriseNotification,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetPrayerNotification("sunrise", !notificationState.sunriseNotification)) }
                    )
                }

                item {
                    PrayerNotificationCard(
                        prayerName = "Dhuhr",
                        isEnabled = notificationState.dhuhrNotification,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetPrayerNotification("dhuhr", !notificationState.dhuhrNotification)) }
                    )
                }

                item {
                    PrayerNotificationCard(
                        prayerName = "Asr",
                        isEnabled = notificationState.asrNotification,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetPrayerNotification("asr", !notificationState.asrNotification)) }
                    )
                }

                item {
                    PrayerNotificationCard(
                        prayerName = "Maghrib",
                        isEnabled = notificationState.maghribNotification,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetPrayerNotification("maghrib", !notificationState.maghribNotification)) }
                    )
                }

                item {
                    PrayerNotificationCard(
                        prayerName = "Isha",
                        isEnabled = notificationState.ishaNotification,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetPrayerNotification("isha", !notificationState.ishaNotification)) }
                    )
                }

                // Adhan Sound
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = "Adhan Sound")
                }

                item {
                    NotificationToggleCard(
                        title = "Adhan",
                        subtitle = "Play adhan at prayer times",
                        icon = Icons.Default.MusicNote,
                        isEnabled = notificationState.adhanEnabled,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetAdhanEnabled(!notificationState.adhanEnabled)) }
                    )
                }

                // Reminder
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = "Pre-Prayer Reminder")
                }

                item {
                    NotificationToggleCard(
                        title = "Reminder Before Prayer",
                        subtitle = "Get notified before prayer time",
                        icon = Icons.Default.Schedule,
                        isEnabled = notificationState.showReminderBefore,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetShowReminderBefore(!notificationState.showReminderBefore)) }
                    )
                }

                if (notificationState.showReminderBefore) {
                    item {
                        ReminderTimeCard(
                            options = reminderOptions,
                            selectedMinutes = notificationState.reminderMinutes,
                            onSelected = { viewModel.onEvent(SettingsEvent.SetReminderMinutes(it)) }
                        )
                    }
                }

                // Additional Options
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = "Additional Options")
                }

                item {
                    NotificationToggleCard(
                        title = "Vibration",
                        subtitle = "Vibrate with notification",
                        icon = Icons.Default.Vibration,
                        isEnabled = notificationState.vibrationEnabled,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetVibrationEnabled(!notificationState.vibrationEnabled)) }
                    )
                }

                item {
                    NotificationToggleCard(
                        title = "Persistent Notification",
                        subtitle = "Show ongoing notification with next prayer",
                        icon = Icons.Default.NotificationsActive,
                        isEnabled = notificationState.persistentNotification,
                        onToggle = { viewModel.onEvent(SettingsEvent.SetPersistentNotification(!notificationState.persistentNotification)) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun NotificationToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isEnabled) NimazColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun PrayerNotificationCard(
    prayerName: String,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                tint = if (isEnabled) NimazColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = prayerName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun ReminderTimeCard(
    options: List<Pair<String, Int>>,
    selectedMinutes: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            options.forEach { (displayName, minutes) ->
                ReminderOption(
                    displayName = displayName,
                    isSelected = selectedMinutes == minutes,
                    onClick = { onSelected(minutes) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderOption(
    displayName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            NimazColors.Primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
