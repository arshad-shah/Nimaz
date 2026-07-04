package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import com.arshadshah.nimaz.presentation.components.molecules.VoiceOptionCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arshadshah.nimaz.core.navigation.ScreenTags
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.audio.DownloadState
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

// Prayer accent colors matching the HTML prototype
private val FajrColor = NimazColors.PrayerColors.Fajr
private val DhuhrColor = NimazColors.Gold500
private val AsrColor = NimazColors.PrayerColors.Asr
private val MaghribColor = NimazColors.PrayerColors.Maghrib
private val IshaColor = NimazColors.PrayerColors.Isha

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
    val adhanPreviewError by viewModel.adhanPreviewError.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // Resolved in composable scope: the steppers' formatValue callback is not
    // composable, so it cannot call stringResource itself.
    val minutesValueFormat = stringResource(R.string.notification_settings_minutes_value)

    LaunchedEffect(adhanPreviewError) {
        adhanPreviewError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearAdhanPreviewError()
        }
    }

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
                title = stringResource(R.string.notifications),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ScreenTags.NotificationsList)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Global Toggle
            item {
                NimazMenuGroup {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.notification_settings_enable),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.notification_settings_enable_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        NimazSwitch(
                            checked = notificationState.notificationsEnabled,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.SetNotificationsEnabled(!notificationState.notificationsEnabled))
                            }
                        )
                    }
                }
            }

            // Prayer Notifications Section
            if (notificationState.notificationsEnabled) {
                item {
                    NimazSectionHeader(title = stringResource(R.string.notification_settings_prayer_section))
                }

                item {
                    NimazMenuGroup {
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
                                    viewModel.onEvent(
                                        SettingsEvent.SetPrayerAdhanEnabled(
                                            prayer.key,
                                            !currentState
                                        )
                                    )
                                },
                                globalAdhanEnabled = notificationState.adhanEnabled
                            )
                            if (index < prayers.lastIndex) {
                                NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }

                // Adhan Sound Section
                item {
                    NimazSectionHeader(title = stringResource(R.string.notification_settings_adhan_section))
                }

                // Global Adhan Toggle
                item {
                    NimazMenuGroup {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.notification_settings_enable_adhan),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.notification_settings_enable_adhan_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            NimazSwitch(
                                checked = notificationState.adhanEnabled,
                                onCheckedChange = {
                                    viewModel.onEvent(SettingsEvent.SetAdhanEnabled(!notificationState.adhanEnabled))
                                }
                            )
                        }
                    }
                }

                // Muezzin Selection (only show if adhan is enabled)
                if (notificationState.adhanEnabled) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            adhanSounds.forEach { sound ->
                                val soundDownloadState = downloadState[sound]
                                val isThisPlaying = isPlaying && currentlyPlaying == sound
                                val isDownloaded =
                                    viewModel.adhanAudioManager.isDownloaded(sound, false)

                                VoiceOptionCard(
                                    name = sound.displayName,
                                    primaryTag = sound.origin,
                                    isSelected = sound.name == selectedAdhanName,
                                    isPlaying = isThisPlaying,
                                    isDownloading = soundDownloadState is DownloadState.Downloading,
                                    isDownloaded = isDownloaded,
                                    previewContentDescription = stringResource(R.string.notification_settings_preview),
                                    onClick = {
                                        viewModel.onEvent(SettingsEvent.SetAdhanSound(sound.name))
                                    },
                                    onPreviewClick = {
                                        if (isThisPlaying) {
                                            viewModel.onEvent(SettingsEvent.StopAdhanPreview)
                                        } else {
                                            viewModel.onEvent(SettingsEvent.SetAdhanSound(sound.name))
                                            viewModel.onEvent(SettingsEvent.PreviewAdhanSound)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } // End of if (notificationState.adhanEnabled)

                // Additional Alerts Section
                item {
                    NimazSectionHeader(title = stringResource(R.string.notification_settings_additional_section))
                }

                item {
                    NimazMenuGroup {
                        // Pre-Adhan Reminder
                        NimazSettingsItem(
                            title = stringResource(R.string.notification_settings_pre_adhan),
                            subtitle = stringResource(
                                R.string.notification_settings_pre_adhan_subtitle,
                                notificationState.reminderMinutes
                            ),
                            checked = notificationState.showReminderBefore,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.SetShowReminderBefore(!notificationState.showReminderBefore))
                            }
                        )
                        // Editable lead time — revealed when the reminder is on.
                        if (notificationState.showReminderBefore) {
                            NimazNumberStepper(
                                value = notificationState.reminderMinutes,
                                onValueChange = {
                                    viewModel.onEvent(SettingsEvent.SetReminderMinutes(it))
                                },
                                variant = NimazNumberStepperVariant.INLINE,
                                label = stringResource(R.string.notification_settings_lead_time),
                                formatValue = { min ->
                                    minutesValueFormat.format(min)
                                },
                                minValue = 5,
                                maxValue = 60,
                                step = 5,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Sunrise Alert
                        NimazSettingsItem(
                            title = stringResource(R.string.notification_settings_sunrise),
                            subtitle = stringResource(R.string.notification_settings_sunrise_subtitle),
                            checked = notificationState.sunriseNotification,
                            onCheckedChange = {
                                viewModel.onEvent(
                                    SettingsEvent.SetPrayerNotification(
                                        "sunrise",
                                        !notificationState.sunriseNotification
                                    )
                                )
                            }
                        )
                        NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Friday (Jummah) Reminder — weekly, before Dhuhr on Friday
                        NimazSettingsItem(
                            title = stringResource(R.string.notification_settings_friday_reminder),
                            subtitle = stringResource(R.string.notification_settings_friday_subtitle),
                            checked = notificationState.fridayReminderEnabled,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.SetFridayReminderEnabled(!notificationState.fridayReminderEnabled))
                            }
                        )
                        if (notificationState.fridayReminderEnabled) {
                            NimazNumberStepper(
                                value = notificationState.fridayReminderMinutes,
                                onValueChange = {
                                    viewModel.onEvent(SettingsEvent.SetFridayReminderMinutes(it))
                                },
                                variant = NimazNumberStepperVariant.INLINE,
                                label = stringResource(R.string.notification_settings_lead_time),
                                formatValue = { min ->
                                    minutesValueFormat.format(min)
                                },
                                minValue = 15,
                                maxValue = 120,
                                step = 15,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Vibration
                        NimazSettingsItem(
                            title = stringResource(R.string.notification_settings_vibration),
                            subtitle = stringResource(R.string.notification_settings_vibration_subtitle),
                            checked = notificationState.vibrationEnabled,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.SetVibrationEnabled(!notificationState.vibrationEnabled))
                            }
                        )
                        NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Honor Do Not Disturb
                        NimazSettingsItem(
                            title = stringResource(R.string.notification_settings_dnd),
                            subtitle = stringResource(R.string.notification_settings_dnd_subtitle),
                            checked = notificationState.respectDnd,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.SetRespectDnd(!notificationState.respectDnd))
                            }
                        )
                    }
                }

                // Troubleshooting Section
                item {
                    NimazSectionHeader(title = stringResource(R.string.notification_settings_troubleshooting_section))
                }

                item {
                    val testSentMsg = stringResource(R.string.notification_settings_test_sent)
                    val testAllSentMsg =
                        stringResource(R.string.notification_settings_test_all_sent)
                    val resetSuccessMsg =
                        stringResource(R.string.notification_settings_reset_success)
                    NimazMenuGroup {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NimazButton(
                                text = stringResource(R.string.notification_settings_test),
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.TestNotification)
                                    Toast.makeText(context, testSentMsg, Toast.LENGTH_SHORT).show()
                                },
                                variant = NimazButtonVariant.FILLED,
                                leadingIcon = Icons.Default.Notifications,
                                fullWidth = true
                            )

                            NimazButton(
                                text = stringResource(R.string.notification_settings_test_all),
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.TestAllNotifications)
                                    Toast.makeText(context, testAllSentMsg, Toast.LENGTH_SHORT)
                                        .show()
                                },
                                variant = NimazButtonVariant.TONAL,
                                leadingIcon = Icons.Default.Notifications,
                                fullWidth = true
                            )

                            NimazButton(
                                text = stringResource(R.string.notification_settings_reset),
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.ResetNotifications)
                                    Toast.makeText(context, resetSuccessMsg, Toast.LENGTH_SHORT)
                                        .show()
                                },
                                variant = NimazButtonVariant.OUTLINED,
                                leadingIcon = Icons.Default.Refresh,
                                fullWidth = true
                            )
                        }
                    }
                }

                // Battery Optimization Section
                item {
                    NimazSectionHeader(title = stringResource(R.string.notification_settings_battery_section))
                }

                item {
                    val powerManager =
                        context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
                    val isExempted =
                        powerManager.isIgnoringBatteryOptimizations(context.packageName)

                    NimazMenuGroup {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.notification_settings_battery_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isExempted) stringResource(R.string.notification_settings_battery_disabled) else stringResource(
                                            R.string.notification_settings_battery_enabled
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isExempted) MaterialTheme.colorScheme.primary
                                        else NimazColors.Warning
                                    )
                                }
                                if (isExempted) {
                                    NimazIcon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.notification_settings_battery_exempted),
                                        variant = NimazIconVariant.PRIMARY,
                                        size = NimazIconSize.LARGE
                                    )
                                }
                            }
                            if (!isExempted) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.notification_settings_battery_explanation),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                NimazButton(
                                    text = stringResource(R.string.notification_settings_disable_battery),
                                    onClick = {
                                        val intent =
                                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        context.startActivity(intent)
                                    },
                                    variant = NimazButtonVariant.FILLED,
                                    fullWidth = true
                                )
                            }
                        }
                    }
                }

                // Info Banner
                item {
                    NimazBanner(
                        message = stringResource(R.string.notification_settings_info_banner),
                        variant = NimazBannerVariant.INFO,
                        icon = Icons.Default.Info,
                        showBorder = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
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
                text = stringResource(R.string.notification_settings_prayer_notification),
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
            NimazIcon(
                imageVector = if (prayer.isSoundOn) Icons.AutoMirrored.Filled.VolumeUp
                else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = if (prayer.isSoundOn) stringResource(R.string.notification_settings_sound_on) else stringResource(
                    R.string.notification_settings_sound_off
                ),
                tint = when {
                    !globalAdhanEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    prayer.isSoundOn -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                iconSize = 18.dp
            )
        }

        Spacer(modifier = Modifier.width(15.dp))

        // Toggle
        NimazSwitch(
            checked = prayer.isEnabled,
            onCheckedChange = { onToggle() }
        )
    }
}


// Previews

@Preview(showBackground = true, widthDp = 400, name = "Prayer Notification Row - Enabled")
@Composable
private fun PrayerNotificationRowEnabledPreview() {
    NimazTheme {
        PrayerNotificationRow(
            prayer = PrayerNotificationData(
                name = "Fajr",
                key = "fajr",
                accentColor = NimazColors.InfoSoft,
                isEnabled = true,
                isSoundOn = true
            ),
            onToggle = {},
            onSoundToggle = {},
            globalAdhanEnabled = true
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Prayer Notification Row - Disabled")
@Composable
private fun PrayerNotificationRowDisabledPreview() {
    NimazTheme {
        PrayerNotificationRow(
            prayer = PrayerNotificationData(
                name = "Dhuhr",
                key = "dhuhr",
                accentColor = NimazColors.Gold400,
                isEnabled = false,
                isSoundOn = false
            ),
            onToggle = {},
            onSoundToggle = {},
            globalAdhanEnabled = true
        )
    }
}
