package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

/**
 * Troubleshooting subscreen (#301): send test notifications, reset, and the battery-optimization
 * exemption prompt (read live from the OS, same as before).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTroubleshootingScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.notif_hub_troubleshooting_title),
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

            item { NimazSectionHeader(title = stringResource(R.string.notification_settings_troubleshooting_section)) }
            item {
                val testSentMsg = stringResource(R.string.notification_settings_test_sent)
                val testAllSentMsg = stringResource(R.string.notification_settings_test_all_sent)
                val resetSuccessMsg = stringResource(R.string.notification_settings_reset_success)
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
                                Toast.makeText(context, testAllSentMsg, Toast.LENGTH_SHORT).show()
                            },
                            variant = NimazButtonVariant.TONAL,
                            leadingIcon = Icons.Default.Notifications,
                            fullWidth = true
                        )
                        NimazButton(
                            text = stringResource(R.string.notification_settings_reset),
                            onClick = {
                                viewModel.onEvent(SettingsEvent.ResetNotifications)
                                Toast.makeText(context, resetSuccessMsg, Toast.LENGTH_SHORT).show()
                            },
                            variant = NimazButtonVariant.OUTLINED,
                            leadingIcon = Icons.Default.Refresh,
                            fullWidth = true
                        )
                    }
                }
            }

            item { NimazSectionHeader(title = stringResource(R.string.notification_settings_battery_section)) }
            item {
                val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
                val isExempted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
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
                                    text = if (isExempted) stringResource(R.string.notification_settings_battery_disabled)
                                    else stringResource(R.string.notification_settings_battery_enabled),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isExempted) MaterialTheme.colorScheme.primary else NimazColors.Warning
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
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                },
                                variant = NimazButtonVariant.FILLED,
                                fullWidth = true
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
