package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

internal data class PrayerNotificationData(
    val name: String,
    val key: String,
    val accentColor: Color,
    val isEnabled: Boolean,
    val isSoundOn: Boolean,
)

/**
 * Prayer notifications subscreen (#301): per-prayer notification + adhan toggles, the pre-adhan
 * reminder with its lead-time stepper, and the sunrise alert. Renders slices of the shared
 * [SettingsViewModel] notification state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerNotificationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val notificationState by viewModel.notificationState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val minutesValueFormat = stringResource(R.string.notification_settings_minutes_value)

    val prayers = listOf(
        PrayerNotificationData(
            stringResource(R.string.prayer_fajr), "fajr", NimazColors.PrayerColors.Fajr,
            notificationState.fajrNotification,
            notificationState.adhanEnabled && notificationState.fajrAdhanEnabled
        ),
        PrayerNotificationData(
            stringResource(R.string.prayer_dhuhr), "dhuhr", NimazColors.Gold500,
            notificationState.dhuhrNotification,
            notificationState.adhanEnabled && notificationState.dhuhrAdhanEnabled
        ),
        PrayerNotificationData(
            stringResource(R.string.prayer_asr), "asr", NimazColors.PrayerColors.Asr,
            notificationState.asrNotification,
            notificationState.adhanEnabled && notificationState.asrAdhanEnabled
        ),
        PrayerNotificationData(
            stringResource(R.string.prayer_maghrib), "maghrib", NimazColors.PrayerColors.Maghrib,
            notificationState.maghribNotification,
            notificationState.adhanEnabled && notificationState.maghribAdhanEnabled
        ),
        PrayerNotificationData(
            stringResource(R.string.prayer_isha), "isha", NimazColors.PrayerColors.Isha,
            notificationState.ishaNotification,
            notificationState.adhanEnabled && notificationState.ishaAdhanEnabled
        )
    )

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.notif_hub_prayers_title),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

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
                                    SettingsEvent.SetPrayerNotification(prayer.key, !prayer.isEnabled)
                                )
                            },
                            onSoundToggle = {
                                val currentState = when (prayer.key) {
                                    "fajr" -> notificationState.fajrAdhanEnabled
                                    "dhuhr" -> notificationState.dhuhrAdhanEnabled
                                    "asr" -> notificationState.asrAdhanEnabled
                                    "maghrib" -> notificationState.maghribAdhanEnabled
                                    "isha" -> notificationState.ishaAdhanEnabled
                                    else -> true
                                }
                                viewModel.onEvent(
                                    SettingsEvent.SetPrayerAdhanEnabled(prayer.key, !currentState)
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

            item {
                NimazSectionHeader(title = stringResource(R.string.notification_settings_additional_section))
            }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.notification_settings_pre_adhan),
                        subtitle = pluralStringResource(
                            R.plurals.notification_settings_pre_adhan_subtitle,
                            notificationState.reminderMinutes,
                            notificationState.reminderMinutes
                        ),
                        checked = notificationState.showReminderBefore,
                        onCheckedChange = {
                            viewModel.onEvent(SettingsEvent.SetShowReminderBefore(!notificationState.showReminderBefore))
                        }
                    )
                    if (notificationState.showReminderBefore) {
                        NimazNumberStepper(
                            value = notificationState.reminderMinutes,
                            onValueChange = { viewModel.onEvent(SettingsEvent.SetReminderMinutes(it)) },
                            variant = NimazNumberStepperVariant.INLINE,
                            label = stringResource(R.string.notification_settings_lead_time),
                            formatValue = { min -> minutesValueFormat.format(min) },
                            minValue = 5, maxValue = 60, step = 5,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.notification_settings_sunrise),
                        subtitle = stringResource(R.string.notification_settings_sunrise_subtitle),
                        checked = notificationState.sunriseNotification,
                        onCheckedChange = {
                            viewModel.onEvent(
                                SettingsEvent.SetPrayerNotification("sunrise", !notificationState.sunriseNotification)
                            )
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
internal fun PrayerNotificationRow(
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
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(prayer.accentColor)
        )
        Spacer(modifier = Modifier.width(15.dp))
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
        IconButton(
            onClick = onSoundToggle,
            enabled = globalAdhanEnabled,
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
                contentDescription = if (prayer.isSoundOn) stringResource(R.string.notification_settings_sound_on)
                else stringResource(R.string.notification_settings_sound_off),
                tint = when {
                    !globalAdhanEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    prayer.isSoundOn -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                iconSize = 18.dp
            )
        }
        Spacer(modifier = Modifier.width(15.dp))
        NimazSwitch(checked = prayer.isEnabled, onCheckedChange = { onToggle() })
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun PrayerNotificationRow_Preview() {
    NimazTheme {
        PrayerNotificationRow(
            prayer = PrayerNotificationData("Fajr", "fajr", NimazColors.InfoSoft, true, true),
            onToggle = {}, onSoundToggle = {}, globalAdhanEnabled = true
        )
    }
}
