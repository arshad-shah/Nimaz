package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.NotificationDiagnostics
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazBanner
import com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsViewModel

/**
 * Diagnostics: what the OS is currently allowing, and the three actions that fix it.
 *
 * Every row states a value read from the device — a row that always said "OK" would send
 * someone looking for the fault somewhere else. Anything the app cannot actually check is
 * not listed at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var confirmingReset by remember { mutableStateOf(false) }

    // Re-read whenever the screen comes back: these are the settings the user leaves to
    // change, so a stale value here is the one thing that would make the screen useless.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow
        .collectAsStateWithLifecycle()
    val diagnostics = remember(lifecycleState) { NotificationDiagnostics.read(context) }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.notif_hub_diagnostics_title),
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

            item { NimazSectionHeader(title = stringResource(R.string.notif_diag_status_section)) }
            item {
                NimazMenuGroup {
                    DiagnosticRow(
                        title = stringResource(R.string.notif_diag_permission),
                        ok = diagnostics.notificationsPermitted,
                        okLabel = stringResource(R.string.notif_diag_granted),
                        problemLabel = stringResource(R.string.notif_diag_blocked),
                        icon = Icons.Default.NotificationsActive,
                        onOpenSystemSettings = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        }
                    )
                    DiagnosticRow(
                        title = stringResource(R.string.notif_diag_exact_alarms),
                        ok = diagnostics.exactAlarmsAllowed,
                        okLabel = stringResource(R.string.notif_diag_allowed),
                        problemLabel = stringResource(R.string.notif_diag_not_allowed),
                        icon = Icons.Default.Schedule,
                        onOpenSystemSettings = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            )
                        }
                    )
                    DiagnosticRow(
                        title = stringResource(R.string.notif_diag_battery),
                        ok = diagnostics.batteryUnrestricted,
                        okLabel = stringResource(R.string.notif_diag_unrestricted),
                        problemLabel = stringResource(R.string.notif_diag_restricted),
                        icon = Icons.Default.BatteryAlert,
                        onOpenSystemSettings = {
                            context.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            )
                        }
                    )
                }
            }

            item { NimazSectionHeader(title = stringResource(R.string.notif_diag_actions_section)) }
            item {
                val testSentMsg = stringResource(R.string.notification_settings_test_sent)
                val testAllSentMsg = stringResource(R.string.notification_settings_test_all_sent)
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
                            // Reset cancels and rebuilds every armed alarm, so it carries
                            // the destructive colour and asks before doing it.
                            onClick = { confirmingReset = true },
                            variant = NimazButtonVariant.DESTRUCTIVE,
                            leadingIcon = Icons.Default.Refresh,
                            fullWidth = true
                        )
                    }
                }
            }

            // Why battery optimisation matters, as a banner rather than loose grey text —
            // it is guidance about the checks above, so it should read as one.
            item {
                NimazBanner(
                    message = stringResource(R.string.notification_settings_battery_explanation),
                    variant = NimazBannerVariant.INFO,
                    icon = Icons.Default.Info,
                    showBorder = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (confirmingReset) {
        val resetSuccessMsg = stringResource(R.string.notification_settings_reset_success)
        // Destructive: it cancels every armed alarm before rebuilding them, so the accent
        // stripe and the confirm button carry the risk in colour as well as in words.
        NimazConfirmDialog(
            title = stringResource(R.string.notif_diag_reset_confirm_title),
            message = stringResource(R.string.notif_diag_reset_confirm_body),
            confirmText = stringResource(R.string.notif_diag_reset_confirm_action),
            cancelText = stringResource(R.string.cancel),
            titleIcon = Icons.Default.Refresh,
            isDestructive = true,
            onConfirm = {
                viewModel.onEvent(SettingsEvent.ResetNotifications)
                Toast.makeText(context, resetSuccessMsg, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { confirmingReset = false }
        )
    }
}

/**
 * One checked prerequisite, as a menu row with its state as a badge.
 *
 * Every row opens the system screen it reports on, passing or failing. Making only the
 * failing ones tappable was tempting, but it leaves a row that looks like the others and
 * does nothing when tapped — and someone who wants to see *why* a check passes has nowhere
 * to go. The badge already says which rows need attention.
 */
@Composable
private fun DiagnosticRow(
    title: String,
    ok: Boolean,
    okLabel: String,
    problemLabel: String,
    icon: ImageVector,
    onOpenSystemSettings: () -> Unit,
) {
    NimazMenuItem(
        title = title,
        icon = icon,
        onClick = onOpenSystemSettings,
        trailing = {
            NimazBadge(
                text = if (ok) okLabel else problemLabel,
                tone = if (ok) NimazTone.SUCCESS else NimazTone.WARNING
            )
        }
    )
}
