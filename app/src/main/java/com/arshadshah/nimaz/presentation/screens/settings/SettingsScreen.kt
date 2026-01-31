package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazTheme

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
                SettingsSection(title = "PRAYER SETTINGS") {
                    SettingsMenuItem(
                        icon = Icons.Default.Calculate,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        title = "Calculation Method",
                        subtitle = "Prayer time calculation settings",
                        onClick = onNavigateToPrayerSettings
                    )
                    SettingsMenuDivider()
                    SettingsMenuItem(
                        icon = Icons.Default.LocationOn,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        title = "Location",
                        subtitle = "Manage prayer time locations",
                        onClick = onNavigateToLocation
                    )
                    SettingsMenuDivider()
                    SettingsMenuItem(
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
                SettingsSection(title = "QURAN") {
                    SettingsMenuItem(
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
                SettingsSection(title = "APP SETTINGS") {
                    SettingsMenuItem(
                        icon = Icons.Default.DarkMode,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        iconBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        title = "Appearance",
                        subtitle = "Theme & display",
                        onClick = onNavigateToAppearance
                    )
                    SettingsMenuDivider()
                    SettingsMenuItem(
                        icon = Icons.Default.Language,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        iconBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        title = "Language",
                        subtitle = "App language preferences",
                        onClick = onNavigateToLanguage
                    )
                    SettingsMenuDivider()
                    SettingsMenuItem(
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
                SettingsSection(title = "DATA") {
                    SettingsMenuItem(
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
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showArrow: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(15.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 73.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
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

@Preview(showBackground = true, widthDp = 400, name = "Settings Section")
@Composable
private fun SettingsSectionPreview() {
    NimazTheme {
        SettingsSection(title = "PRAYER SETTINGS") {
            SettingsMenuItem(
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFF6750A4),
                iconBackground = Color(0xFF6750A4).copy(alpha = 0.12f),
                title = "Notifications",
                subtitle = "Adhan & reminders",
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Settings Menu Item")
@Composable
private fun SettingsMenuItemPreview() {
    NimazTheme {
        SettingsMenuItem(
            icon = Icons.Default.Calculate,
            iconTint = Color(0xFF6750A4),
            iconBackground = Color(0xFF6750A4).copy(alpha = 0.12f),
            title = "Calculation Method",
            subtitle = "Prayer time calculation settings",
            onClick = {}
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
