package com.arshadshah.nimaz.presentation.screens.more

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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToWidgets: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onShareApp: () -> Unit,
    onRateApp: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazTopAppBar(
                title = "More",
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick Access Section
            item {
                SectionHeader(title = "Quick Access")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAccessCard(
                        title = "Bookmarks",
                        icon = Icons.Default.Bookmark,
                        color = NimazColors.QuranColors.Meccan,
                        onClick = onNavigateToBookmarks,
                        modifier = Modifier.weight(1f)
                    )
                    QuickAccessCard(
                        title = "Calendar",
                        icon = Icons.Default.CalendarMonth,
                        color = NimazColors.Primary,
                        onClick = onNavigateToCalendar,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Settings Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Settings")
            }

            item {
                MenuItemCard(
                    title = "General Settings",
                    subtitle = "Prayer calculation, adjustments",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToSettings
                )
            }

            item {
                MenuItemCard(
                    title = "Location",
                    subtitle = "Manage locations for prayer times",
                    icon = Icons.Default.LocationOn,
                    onClick = onNavigateToLocation
                )
            }

            item {
                MenuItemCard(
                    title = "Notifications",
                    subtitle = "Prayer alerts and reminders",
                    icon = Icons.Default.Notifications,
                    onClick = onNavigateToNotifications
                )
            }

            item {
                MenuItemCard(
                    title = "Appearance",
                    subtitle = "Theme, colors, and display",
                    icon = Icons.Default.DarkMode,
                    onClick = onNavigateToAppearance
                )
            }

            item {
                MenuItemCard(
                    title = "Language",
                    subtitle = "App language preferences",
                    icon = Icons.Default.Language,
                    onClick = onNavigateToLanguage
                )
            }

            item {
                MenuItemCard(
                    title = "Widgets",
                    subtitle = "Home screen widget settings",
                    icon = Icons.Default.Widgets,
                    onClick = onNavigateToWidgets
                )
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "About")
            }

            item {
                MenuItemCard(
                    title = "About Nimaz Pro",
                    subtitle = "Version, credits, and info",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )
            }

            item {
                MenuItemCard(
                    title = "Help & Support",
                    subtitle = "FAQs and contact us",
                    icon = Icons.AutoMirrored.Filled.Help,
                    onClick = onNavigateToHelp
                )
            }

            // Share Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Share")
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Share App",
                        icon = Icons.Default.Share,
                        color = NimazColors.Secondary,
                        onClick = onShareApp,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Rate Us",
                        icon = Icons.Default.Star,
                        color = NimazColors.StatusColors.Late,
                        onClick = onRateApp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
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
private fun QuickAccessCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuItemCard(
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
                tint = MaterialTheme.colorScheme.primary,
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}
