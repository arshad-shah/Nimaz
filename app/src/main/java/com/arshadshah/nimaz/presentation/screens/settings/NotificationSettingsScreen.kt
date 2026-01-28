package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.arshadshah.nimaz.data.audio.DownloadState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

// Prayer accent colors matching the HTML prototype
private val FajrColor = Color(0xFF6366F1)
private val DhuhrColor = Color(0xFFEAB308)
private val AsrColor = Color(0xFFF97316)
private val MaghribColor = Color(0xFFEF4444)
private val IshaColor = Color(0xFF8B5CF6)

private data class PrayerNotificationData(
    val name: String,
    val key: String,
    val accentColor: Color,
    val isEnabled: Boolean,
    val isSoundOn: Boolean,
    val isSunrise: Boolean = false // Sunrise only gets beep, no sound toggle
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val notificationState by viewModel.notificationState.collectAsState()
    val downloadState by viewModel.adhanAudioManager.downloadState.collectAsState()
    val isPlaying by viewModel.adhanAudioManager.isPlaying.collectAsState()
    val currentlyPlaying by viewModel.adhanAudioManager.currentlyPlaying.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Individual prayer settings with per-prayer adhan toggles
    val prayers = listOf(
        PrayerNotificationData(
            "Fajr", "fajr", FajrColor,
            notificationState.fajrNotification,
            notificationState.adhanEnabled && notificationState.fajrAdhanEnabled
        ),
        PrayerNotificationData(
            "Dhuhr", "dhuhr", DhuhrColor,
            notificationState.dhuhrNotification,
            notificationState.adhanEnabled && notificationState.dhuhrAdhanEnabled
        ),
        PrayerNotificationData(
            "Asr", "asr", AsrColor,
            notificationState.asrNotification,
            notificationState.adhanEnabled && notificationState.asrAdhanEnabled
        ),
        PrayerNotificationData(
            "Maghrib", "maghrib", MaghribColor,
            notificationState.maghribNotification,
            notificationState.adhanEnabled && notificationState.maghribAdhanEnabled
        ),
        PrayerNotificationData(
            "Isha", "isha", IshaColor,
            notificationState.ishaNotification,
            notificationState.adhanEnabled && notificationState.ishaAdhanEnabled
        )
    )

    val adhanSounds = com.arshadshah.nimaz.data.audio.AdhanSound.entries
    val selectedAdhanName = notificationState.selectedAdhanSound

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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Global Toggle
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Notifications",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Receive prayer time alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationState.notificationsEnabled,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.SetNotificationsEnabled(!notificationState.notificationsEnabled))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Prayer Notifications Section
            if (notificationState.notificationsEnabled) {
                item {
                    SectionTitle(title = "PRAYER NOTIFICATIONS")
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column {
                            prayers.forEachIndexed { index, prayer ->
                                PrayerNotificationRow(
                                    prayer = prayer,
                                    onToggle = {
                                        viewModel.onEvent(
                                            SettingsEvent.SetPrayerNotification(
                                                prayer.key,
                                                !prayer.isEnabled
                                            )
                                        )
                                    },
                                    onSoundToggle = {
                                        // Toggle individual prayer's adhan setting
                                        val currentState = when (prayer.key) {
                                            "fajr" -> notificationState.fajrAdhanEnabled
                                            "dhuhr" -> notificationState.dhuhrAdhanEnabled
                                            "asr" -> notificationState.asrAdhanEnabled
                                            "maghrib" -> notificationState.maghribAdhanEnabled
                                            "isha" -> notificationState.ishaAdhanEnabled
                                            else -> true
                                        }
                                        viewModel.onEvent(SettingsEvent.SetPrayerAdhanEnabled(prayer.key, !currentState))
                                    },
                                    globalAdhanEnabled = notificationState.adhanEnabled
                                )
                                if (index < prayers.lastIndex) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Adhan Sound Section
                item {
                    SectionTitle(title = "ADHAN SOUND")
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column {
                            adhanSounds.forEachIndexed { index, sound ->
                                val soundDownloadState = downloadState[sound]
                                val isThisPlaying = isPlaying && currentlyPlaying == sound
                                val isDownloaded = viewModel.adhanAudioManager.isDownloaded(sound, false)

                                AdhanOptionRow(
                                    name = sound.displayName,
                                    location = sound.origin,
                                    isSelected = sound.name == selectedAdhanName,
                                    isDownloaded = isDownloaded,
                                    isDownloading = soundDownloadState is DownloadState.Downloading,
                                    downloadProgress = (soundDownloadState as? DownloadState.Downloading)?.progress,
                                    isPlaying = isThisPlaying,
                                    onSelect = {
                                        viewModel.onEvent(SettingsEvent.SetAdhanSound(sound.name))
                                    },
                                    onPlay = {
                                        if (isThisPlaying) {
                                            viewModel.onEvent(SettingsEvent.StopAdhanPreview)
                                        } else {
                                            viewModel.onEvent(SettingsEvent.SetAdhanSound(sound.name))
                                            viewModel.onEvent(SettingsEvent.PreviewAdhanSound)
                                        }
                                    }
                                )
                                if (index < adhanSounds.lastIndex) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Additional Alerts Section
                item {
                    SectionTitle(title = "ADDITIONAL ALERTS")
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column {
                            // Pre-Adhan Reminder
                            SettingToggleRow(
                                label = "Pre-Adhan Reminder",
                                value = "${notificationState.reminderMinutes} minutes before",
                                isEnabled = notificationState.showReminderBefore,
                                onToggle = {
                                    viewModel.onEvent(SettingsEvent.SetShowReminderBefore(!notificationState.showReminderBefore))
                                }
                            )
                            SettingDivider()

                            // Sunrise Alert
                            SettingToggleRow(
                                label = "Sunrise Alert",
                                value = "End of Fajr prayer time",
                                isEnabled = notificationState.sunriseNotification,
                                onToggle = {
                                    viewModel.onEvent(
                                        SettingsEvent.SetPrayerNotification(
                                            "sunrise",
                                            !notificationState.sunriseNotification
                                        )
                                    )
                                }
                            )
                            SettingDivider()

                            // Friday Prayer Reminder (maps to persistent notification)
                            SettingToggleRow(
                                label = "Friday Prayer Reminder",
                                value = "1 hour before Jummah",
                                isEnabled = notificationState.persistentNotification,
                                onToggle = {
                                    viewModel.onEvent(SettingsEvent.SetPersistentNotification(!notificationState.persistentNotification))
                                }
                            )
                            SettingDivider()

                            // Vibration
                            SettingToggleRow(
                                label = "Vibration",
                                value = "Vibrate with notification",
                                isEnabled = notificationState.vibrationEnabled,
                                onToggle = {
                                    viewModel.onEvent(SettingsEvent.SetVibrationEnabled(!notificationState.vibrationEnabled))
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Troubleshooting Section
                item {
                    SectionTitle(title = "TROUBLESHOOTING")
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.TestNotification)
                                    Toast.makeText(context, "Test notification sent", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test Notification")
                            }

                            Button(
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.TestAllNotifications)
                                    Toast.makeText(context, "Testing all prayer notifications...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test All Prayers")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.ResetNotifications)
                                    Toast.makeText(context, "Notifications reset successfully", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset Notifications")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Info Banner
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(15.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Make sure to enable notifications in your device settings and disable battery optimization for accurate prayer alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 5.dp, bottom = 12.dp)
    )
}

@Composable
private fun PrayerNotificationRow(
    prayer: PrayerNotificationData,
    onToggle: () -> Unit,
    onSoundToggle: () -> Unit,
    globalAdhanEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(prayer.accentColor)
        )

        Spacer(modifier = Modifier.width(15.dp))

        // Prayer info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prayer.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Prayer notification",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Sound button - only show if global adhan is enabled
        IconButton(
            onClick = onSoundToggle,
            enabled = globalAdhanEnabled, // Disable if global adhan is off
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (globalAdhanEnabled)
                    MaterialTheme.colorScheme.surfaceContainerHighest
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                imageVector = if (prayer.isSoundOn) Icons.AutoMirrored.Filled.VolumeUp
                else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = if (prayer.isSoundOn) "Sound on" else "Sound off",
                tint = when {
                    !globalAdhanEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    prayer.isSoundOn -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(15.dp))

        // Toggle
        Switch(
            checked = prayer.isEnabled,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun AdhanOptionRow(
    name: String,
    location: String,
    isSelected: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int?,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Radio indicator
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            modifier = Modifier.size(20.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.width(15.dp))

        // Adhan info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Play/Download button
        IconButton(
            onClick = onPlay,
            enabled = !isDownloading,
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            when {
                isDownloading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                isPlaying -> {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                !isDownloaded -> {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download and Play",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    value: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
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

@Composable
private fun SettingDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    )
}
