package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuDivider
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrayerSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToQuranSettings: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToWidgets: () -> Unit,
    onNavigateToSync: () -> Unit = {},
    onNavigateToSearchSettings: () -> Unit = {},
    onNavigateToZakatSettings: () -> Unit = {},
    onRestartApp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val shouldRestart by viewModel.shouldRestart.collectAsStateWithLifecycle()

    LaunchedEffect(shouldRestart) {
        if (shouldRestart) onRestartApp()
    }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.settings),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ScreenTags.SettingsList)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Prayer Settings
            item { NimazSectionHeader(title = stringResource(R.string.prayer_settings)) }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.calculation_method),
                        subtitle = stringResource(R.string.calculation_method_subtitle),
                        icon = Icons.Default.Calculate,
                        onClick = onNavigateToPrayerSettings
                    )
                    NimazMenuDivider()
                    NimazMenuItem(
                        title = stringResource(R.string.location),
                        subtitle = stringResource(R.string.location_subtitle),
                        icon = Icons.Default.LocationOn,
                        onClick = onNavigateToLocation
                    )
                    NimazMenuDivider()
                    NimazMenuItem(
                        title = stringResource(R.string.notifications),
                        subtitle = stringResource(R.string.notifications_subtitle),
                        icon = Icons.Default.Notifications,
                        onClick = onNavigateToNotifications
                    )
                }
            }

            // Quran
            item { NimazSectionHeader(title = stringResource(R.string.quran)) }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.quran_settings),
                        subtitle = stringResource(R.string.quran_settings_subtitle),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToQuranSettings
                    )
                }
            }

            // Zakat — the nisab basis, the metal prices and the currency. These were an
            // accordion inside the calculator's form; they are preferences, so they belong
            // here alongside the other per-feature settings.
            item { NimazSectionHeader(title = stringResource(R.string.zakat)) }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.zakat_settings),
                        subtitle = stringResource(R.string.zakat_settings_subtitle),
                        icon = Icons.Default.Savings,
                        onClick = onNavigateToZakatSettings
                    )
                }
            }

            // Search & AI
            item { NimazSectionHeader(title = stringResource(R.string.search_settings)) }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.search_settings),
                        subtitle = stringResource(R.string.search_settings_subtitle),
                        icon = Icons.Default.Search,
                        onClick = onNavigateToSearchSettings
                    )
                }
            }

            // App Settings
            item { NimazSectionHeader(title = stringResource(R.string.app_settings)) }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.appearance),
                        subtitle = stringResource(R.string.appearance_subtitle),
                        icon = Icons.Default.DarkMode,
                        onClick = onNavigateToAppearance
                    )
                    NimazMenuDivider()
                    NimazMenuItem(
                        title = stringResource(R.string.language),
                        subtitle = stringResource(R.string.language_subtitle),
                        icon = Icons.Default.Language,
                        onClick = onNavigateToLanguage
                    )
                    NimazMenuDivider()
                    NimazMenuItem(
                        title = stringResource(R.string.widgets),
                        subtitle = stringResource(R.string.widgets_subtitle),
                        icon = Icons.Default.Widgets,
                        onClick = onNavigateToWidgets
                    )
                }
            }

            // Data
            item { NimazSectionHeader(title = stringResource(R.string.data)) }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.sync_data),
                        subtitle = stringResource(R.string.settings_sync_subtitle),
                        icon = Icons.Default.Sync,
                        onClick = onNavigateToSync
                    )
                    NimazMenuDivider()
                    NimazMenuItem(
                        title = stringResource(R.string.reset_settings),
                        subtitle = stringResource(R.string.reset_settings_subtitle),
                        icon = Icons.Default.Restore,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { showResetDialog = true }
                    )
                    NimazMenuDivider()
                    NimazMenuItem(
                        title = stringResource(R.string.delete_all_data),
                        subtitle = stringResource(R.string.delete_all_data_subtitle),
                        icon = Icons.Default.Delete,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { showDeleteDialog = true }
                    )
                }
            }

            // Version Info
            item { AppVersionInfo() }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showResetDialog) {
        NimazConfirmDialog(
            title = stringResource(R.string.reset_settings_dialog_title),
            message = stringResource(R.string.reset_settings_dialog_message),
            confirmText = stringResource(R.string.reset),
            cancelText = stringResource(R.string.cancel),
            titleIcon = Icons.Default.Restore,
            isDestructive = true,
            onConfirm = { viewModel.onEvent(SettingsEvent.ResetToDefaults) },
            onDismiss = { showResetDialog = false },
        )
    }

    if (showDeleteDialog) {
        NimazConfirmDialog(
            title = stringResource(R.string.delete_all_data_dialog_title),
            message = stringResource(R.string.delete_all_data_dialog_message),
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            titleIcon = Icons.Default.Delete,
            isDestructive = true,
            onConfirm = { viewModel.onEvent(SettingsEvent.DeleteAllData) },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun AppVersionInfo() {
    val context = LocalContext.current
    val unknownVersion = stringResource(R.string.version_unknown)
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: unknownVersion
    } catch (_: Exception) {
        unknownVersion
    }

    Text(
        text = stringResource(R.string.version_format, versionName),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}
