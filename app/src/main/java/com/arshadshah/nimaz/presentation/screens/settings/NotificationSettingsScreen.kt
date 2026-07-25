package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

/**
 * Notifications hub (#301): a master switch plus links into focused subscreens (Prayer, Worship
 * reminders, Weekly & reading, Sound & delivery, Troubleshooting). All state lives in the shared
 * [SettingsViewModel]; each subscreen renders a slice of it. Replaced the previous single long
 * scroll with no behaviour change.
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
    val notificationState by viewModel.notificationState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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

            // Master toggle
            item {
                NimazMenuGroup {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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

            if (notificationState.notificationsEnabled) {
                item {
                    NimazMenuGroup {
                        NimazSettingsItem(
                            title = stringResource(R.string.notif_hub_prayers_title),
                            subtitle = stringResource(R.string.notif_hub_prayers_subtitle),
                            onClick = onNavigateToPrayers,
                            showArrow = true
                        )
                        NimazSettingsItem(
                            title = stringResource(R.string.worship_settings_title),
                            subtitle = stringResource(R.string.worship_settings_subtitle),
                            onClick = onNavigateToWorshipReminders,
                            showArrow = true
                        )
                        NimazSettingsItem(
                            title = stringResource(R.string.notif_hub_weekly_title),
                            subtitle = stringResource(R.string.notif_hub_weekly_subtitle),
                            onClick = onNavigateToWeekly,
                            showArrow = true
                        )
                    }
                }
                item {
                    NimazMenuGroup {
                        NimazSettingsItem(
                            title = stringResource(R.string.notif_hub_sound_title),
                            subtitle = stringResource(R.string.notif_hub_sound_subtitle),
                            onClick = onNavigateToSound,
                            showArrow = true
                        )
                        NimazSettingsItem(
                            title = stringResource(R.string.notif_hub_troubleshooting_title),
                            subtitle = stringResource(R.string.notif_hub_troubleshooting_subtitle),
                            onClick = onNavigateToTroubleshooting,
                            showArrow = true
                        )
                    }
                }
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
