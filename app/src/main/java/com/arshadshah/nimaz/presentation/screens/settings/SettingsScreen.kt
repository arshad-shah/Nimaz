package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Prayer Settings Section
            item {
                SectionHeader(title = "Prayer Times")
            }

            item {
                SettingsNavigationItem(
                    title = "Calculation Method",
                    subtitle = "Prayer time calculation settings",
                    icon = Icons.Default.Calculate,
                    onClick = onNavigateToPrayerSettings
                )
            }

            item {
                SettingsNavigationItem(
                    title = "Location",
                    subtitle = "Manage prayer time locations",
                    icon = Icons.Default.LocationOn,
                    onClick = onNavigateToLocation
                )
            }

            item {
                SettingsNavigationItem(
                    title = "Notifications",
                    subtitle = "Prayer alerts and reminders",
                    icon = Icons.Default.Notifications,
                    onClick = onNavigateToNotifications
                )
            }

            // Quran Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Quran")
            }

            item {
                SettingsNavigationItem(
                    title = "Quran Settings",
                    subtitle = "Reading and audio preferences",
                    icon = Icons.Default.MenuBook,
                    onClick = onNavigateToQuranSettings
                )
            }

            // App Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "App")
            }

            item {
                SettingsNavigationItem(
                    title = "Appearance",
                    subtitle = "Theme, colors, and display",
                    icon = Icons.Default.DarkMode,
                    onClick = onNavigateToAppearance
                )
            }

            item {
                SettingsNavigationItem(
                    title = "Language",
                    subtitle = "App language preferences",
                    icon = Icons.Default.Language,
                    onClick = onNavigateToLanguage
                )
            }

            item {
                SettingsNavigationItem(
                    title = "Widgets",
                    subtitle = "Home screen widget settings",
                    icon = Icons.Default.Widgets,
                    onClick = onNavigateToWidgets
                )
            }

            // Data Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Data")
            }

            item {
                SettingsActionItem(
                    title = "Reset Settings",
                    subtitle = "Restore default settings",
                    icon = Icons.Default.Restore,
                    iconColor = MaterialTheme.colorScheme.error,
                    onClick = { viewModel.onEvent(SettingsEvent.ResetToDefaults) }
                )
            }

            // Version Info
            item {
                Spacer(modifier = Modifier.height(16.dp))
                VersionInfo()
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NimazColors.Primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = NimazColors.Primary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = iconColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VersionInfo(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nimaz Pro",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
