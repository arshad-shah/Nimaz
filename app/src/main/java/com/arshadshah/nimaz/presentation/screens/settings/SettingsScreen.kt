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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                title = "Settings",
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
            item { NimazSectionHeader(title = "Prayer Settings") }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = "Calculation Method",
                        subtitle = "Prayer time calculation settings",
                        icon = Icons.Default.Calculate,
                        onClick = onNavigateToPrayerSettings
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Location",
                        subtitle = "Manage prayer time locations",
                        icon = Icons.Default.LocationOn,
                        onClick = onNavigateToLocation
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Notifications",
                        subtitle = "Adhan & reminders",
                        icon = Icons.Default.Notifications,
                        onClick = onNavigateToNotifications
                    )
                }
            }

            // Quran
            item { NimazSectionHeader(title = "Quran") }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = "Quran Settings",
                        subtitle = "Reading and audio preferences",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToQuranSettings
                    )
                }
            }

            // App Settings
            item { NimazSectionHeader(title = "App Settings") }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = "Appearance",
                        subtitle = "Theme & display",
                        icon = Icons.Default.DarkMode,
                        onClick = onNavigateToAppearance
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Language",
                        subtitle = "App language preferences",
                        icon = Icons.Default.Language,
                        onClick = onNavigateToLanguage
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Widgets",
                        subtitle = "Home screen widgets",
                        icon = Icons.Default.Widgets,
                        onClick = onNavigateToWidgets
                    )
                }
            }

            // Data
            item { NimazSectionHeader(title = "Data") }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = "Reset Settings",
                        subtitle = "Restore default settings",
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
            title = { Text("Reset Settings") },
            text = {
                Text("This will restore all settings to their defaults. Your prayer tracking data, bookmarks, and other content will not be affected. The app will restart.")
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
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
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
        text = "Nimaz v$versionName",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}
