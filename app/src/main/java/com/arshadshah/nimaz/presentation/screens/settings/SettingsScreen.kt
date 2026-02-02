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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

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
    onRestartApp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showResetDialog by remember { mutableStateOf(false) }
    val shouldRestart by viewModel.shouldRestart.collectAsState()

    LaunchedEffect(shouldRestart) {
        if (shouldRestart) onRestartApp()
    }

    Scaffold(
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
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.location),
                        subtitle = stringResource(R.string.location_subtitle),
                        icon = Icons.Default.LocationOn,
                        onClick = onNavigateToLocation
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
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
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.language),
                        subtitle = stringResource(R.string.language_subtitle),
                        icon = Icons.Default.Language,
                        onClick = onNavigateToLanguage
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
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
                        title = stringResource(R.string.reset_settings),
                        subtitle = stringResource(R.string.reset_settings_subtitle),
                        icon = Icons.Default.Restore,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { showResetDialog = true }
                    )
                }
            }

            // Version Info
            item { AppVersionInfo() }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_settings_dialog_title)) },
            text = {
                Text(stringResource(R.string.reset_settings_dialog_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.onEvent(SettingsEvent.ResetToDefaults)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun AppVersionInfo() {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (_: Exception) {
        "Unknown"
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
