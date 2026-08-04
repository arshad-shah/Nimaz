package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.util.NotificationDiagnostics
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.NotificationSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

/**
 * Notifications hub (#301): a master switch and five rows into focused subscreens.
 *
 * Each row's subtitle reports what is actually set rather than describing what the screen
 * behind it contains — the hub is where you check your settings, not just a menu. The
 * warning banner appears only when the device is genuinely in a state that would delay or
 * drop alerts; it is read from the OS on every resume, and it links to Diagnostics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrayers: () -> Unit = {},
    onNavigateToWorshipReminders: () -> Unit = {},
    onNavigateToWeekly: () -> Unit = {},
    onNavigateToSound: () -> Unit = {},
    onNavigateToTroubleshooting: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val notificationState by viewModel.notificationState.collectAsStateWithLifecycle()
    val summary by viewModel.notificationSummary.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Re-read on every resume: the user may have just left to grant a permission, and a
    // banner still warning about it would read as the fix not having worked.
    val context = LocalContext.current
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow
        .collectAsStateWithLifecycle()
    val diagnostics = remember(lifecycleState) {
        if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            NotificationDiagnostics.read(context)
        } else {
            null
        }
    }

    NimazScreenScaffold(
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

            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.notification_settings_enable),
                        subtitle = stringResource(R.string.notification_settings_enable_subtitle),
                        checked = notificationState.notificationsEnabled,
                        onCheckedChange = {
                            viewModel.onEvent(SettingsEvent.SetNotificationsEnabled(it))
                        }
                    )
                }
            }

            if (notificationState.notificationsEnabled) {
                if (diagnostics?.hasProblem == true) {
                    item {
                        NimazBanner(
                            message = stringResource(R.string.notif_hub_delivery_warning),
                            variant = NimazBannerVariant.WARNING,
                            icon = Icons.Default.WarningAmber,
                            showBorder = true,
                            onClick = onNavigateToTroubleshooting,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    NimazMenuGroup {
                        NimazSettingsItem(
                            title = stringResource(R.string.notif_hub_prayers_title),
                            subtitle = prayersSubtitle(summary.fajrAlertStyle, summary.reminderEnabled, summary.reminderMinutes),
                            value = stringResource(
                                R.string.notif_hub_count_of,
                                summary.enabledPrayerCount,
                                5
                            ),
                            onClick = onNavigateToPrayers,
                            showArrow = true
                        )
                        NimazSettingsItem(
                            title = stringResource(R.string.notif_hub_sound_title),
                            subtitle = soundSubtitle(notificationState),
                            onClick = onNavigateToSound,
                            showArrow = true
                        )
                    }
                }

                item {
                    NimazMenuGroup {
                        NimazSettingsItem(
                            title = stringResource(R.string.worship_settings_title),
                            subtitle = stringResource(R.string.notif_hub_worship_subtitle),
                            value = stringResource(
                                R.string.notif_hub_count_on,
                                notificationState.worshipReminders.count { it.value }
                            ),
                            onClick = onNavigateToWorshipReminders,
                            showArrow = true
                        )
                        NimazSettingsItem(
                            title = stringResource(R.string.notif_hub_weekly_title),
                            subtitle = weeklySubtitle(notificationState),
                            onClick = onNavigateToWeekly,
                            showArrow = true
                        )
                    }
                }

                item {
                    NimazMenuGroup {
                        NimazSettingsItem(
                            title = stringResource(R.string.notif_hub_diagnostics_title),
                            subtitle = stringResource(R.string.notif_hub_diagnostics_subtitle),
                            onClick = onNavigateToTroubleshooting,
                            showArrow = true,
                            trailingContent = if (diagnostics?.hasProblem == true) {
                                {
                                    NimazBadge(
                                        text = stringResource(R.string.notif_diag_needs_attention),
                                        tone = NimazTone.WARNING
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/** Fajr's settings, standing in for the set: one line cannot report five different ones. */
@Composable
private fun prayersSubtitle(
    style: PrayerAlertStyle,
    reminderEnabled: Boolean,
    reminderMinutes: Int,
): String {
    val styleLabel = stringResource(NotificationHubSubtitles.alertStyle(style))
    if (!reminderEnabled) return styleLabel
    return stringResource(
        R.string.notif_prayer_summary,
        styleLabel,
        pluralStringResource(
            R.plurals.notif_reminder_minutes_before,
            reminderMinutes,
            reminderMinutes
        )
    )
}

@Composable
private fun soundSubtitle(state: NotificationSettingsUiState): String = stringResource(
    NotificationHubSubtitles.sound(state.respectDnd, state.vibrationEnabled),
    AdhanSound.fromName(state.selectedAdhanSound).displayName
)

@Composable
private fun weeklySubtitle(state: NotificationSettingsUiState): String = stringResource(
    NotificationHubSubtitles.weekly(
        jumuahEnabled = state.fridayReminderEnabled,
        khatamEnabled = state.khatamReminderEnabled
    )
)
