package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsSection
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme
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
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Prayer Settings Section
            item {
                NimazSettingsSection(title = "PRAYER SETTINGS") {
                    NimazSettingsItem(
                        icon = Icons.Default.Calculate,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        title = "Calculation Method",
                        subtitle = "Prayer time calculation settings",
                        onClick = onNavigateToPrayerSettings
                    )
                    NimazDivider(modifier = Modifier.padding(start = 73.dp))
                    NimazSettingsItem(
                        icon = Icons.Default.LocationOn,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        title = "Location",
                        subtitle = "Manage prayer time locations",
                        onClick = onNavigateToLocation
                    )
                    NimazDivider(modifier = Modifier.padding(start = 73.dp))
                    NimazSettingsItem(
                        icon = Icons.Default.Notifications,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        title = "Notifications",
                        subtitle = "Adhan & reminders",
                        onClick = onNavigateToNotifications
                    )
                }
            }

            // Quran Section
            item {
                NimazSettingsSection(title = "QURAN") {
                    NimazSettingsItem(
                        icon = Icons.Default.MenuBook,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        iconBackground = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        title = "Quran Settings",
                        subtitle = "Reading and audio preferences",
                        onClick = onNavigateToQuranSettings
                    )
                }
            }

            // App Settings Section
            item {
                NimazSettingsSection(title = "APP SETTINGS") {
                    NimazSettingsItem(
                        icon = Icons.Default.DarkMode,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        iconBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        title = "Appearance",
                        subtitle = "Theme & display",
                        onClick = onNavigateToAppearance
                    )
                    NimazDivider(modifier = Modifier.padding(start = 73.dp))
                    NimazSettingsItem(
                        icon = Icons.Default.Language,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        iconBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        title = "Language",
                        subtitle = "App language preferences",
                        onClick = onNavigateToLanguage
                    )
                    NimazDivider(modifier = Modifier.padding(start = 73.dp))
                    NimazSettingsItem(
                        icon = Icons.Default.Widgets,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        iconBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        title = "Widgets",
                        subtitle = "Home screen widgets",
                        onClick = onNavigateToWidgets
                    )
                }
            }

            // Data Section
            item {
                NimazSettingsSection(title = "DATA") {
                    NimazSettingsItem(
                        icon = Icons.Default.Restore,
                        iconTint = MaterialTheme.colorScheme.error,
                        iconBackground = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        title = "Reset Settings",
                        subtitle = "Restore default settings",
                        onClick = { viewModel.onEvent(SettingsEvent.ResetToDefaults) },
                        showArrow = false
                    )
                }
            }

            // Version Info
            item {
                VersionInfo()
            }
        }
    }
}

@Composable
private fun VersionInfo(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nimaz",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Version Info")
@Composable
private fun VersionInfoPreview() {
    NimazTheme {
        VersionInfo()
    }
}
