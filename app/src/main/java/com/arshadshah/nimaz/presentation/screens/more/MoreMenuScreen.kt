package com.arshadshah.nimaz.presentation.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuScreen(
    onNavigateToBookmarks: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToWidgets: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onShareApp: () -> Unit,
    onRateApp: () -> Unit,
    onNavigateToHadith: () -> Unit,
    onNavigateToFasting: () -> Unit,
    onNavigateToZakat: () -> Unit,
    onNavigateToDuas: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToCalculationMethod: () -> Unit,
    onNavigateToPrayerTracker: () -> Unit,
    onNavigateToPrayerStats: () -> Unit,
    onNavigateToQadaPrayers: () -> Unit,
    onNavigateToMakeupFasts: () -> Unit,
    onDeleteAllData: () -> Unit
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
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Features Section
            item {
                SectionHeader(title = "Features")
            }
            item {
                GroupedCard {
                    MenuItem(
                        title = "Prayer Tracker",
                        subtitle = "Track your daily prayers",
                        icon = Icons.Default.Schedule,
                        onClick = onNavigateToPrayerTracker
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Prayer Stats",
                        subtitle = "Prayer history and streaks",
                        icon = Icons.Default.QueryStats,
                        onClick = onNavigateToPrayerStats
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Qada Prayers",
                        subtitle = "Missed prayers to make up",
                        icon = Icons.Default.Restore,
                        onClick = onNavigateToQadaPrayers
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Bookmarks",
                        subtitle = "Saved ayahs, hadith, and duas",
                        icon = Icons.Default.Bookmark,
                        onClick = onNavigateToBookmarks
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Calendar",
                        subtitle = "Islamic calendar and events",
                        icon = Icons.Default.CalendarMonth,
                        onClick = onNavigateToCalendar
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Hadith",
                        subtitle = "Authentic hadith collections",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToHadith
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Fasting",
                        subtitle = "Fasting tracker and schedule",
                        icon = Icons.Default.Fastfood,
                        onClick = onNavigateToFasting
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Makeup Fasts",
                        subtitle = "Track missed fasts",
                        icon = Icons.Default.Restore,
                        onClick = onNavigateToMakeupFasts
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Zakat",
                        subtitle = "Zakat calculator",
                        icon = Icons.Default.Calculate,
                        onClick = onNavigateToZakat
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Duas",
                        subtitle = "Daily duas and supplications",
                        icon = Icons.Default.Mosque,
                        onClick = onNavigateToDuas
                    )
                }
            }

            // Prayer Settings Section
            item {
                SectionHeader(title = "Prayer Settings")
            }
            item {
                GroupedCard {
                    MenuItem(
                        title = "Calculation Method",
                        subtitle = "Prayer time calculation parameters",
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToCalculationMethod
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Notifications",
                        subtitle = "Prayer alerts and reminders",
                        icon = Icons.Default.Notifications,
                        onClick = onNavigateToNotifications
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Location",
                        subtitle = "Manage locations for prayer times",
                        icon = Icons.Default.LocationOn,
                        onClick = onNavigateToLocation
                    )
                }
            }

            // App Settings Section
            item {
                SectionHeader(title = "App Settings")
            }
            item {
                GroupedCard {
                    MenuItem(
                        title = "Appearance",
                        subtitle = "Theme, colors, and display",
                        icon = Icons.Default.DarkMode,
                        onClick = onNavigateToAppearance
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Language",
                        subtitle = "App language preferences",
                        icon = Icons.Default.Language,
                        onClick = onNavigateToLanguage
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Widgets",
                        subtitle = "Home screen widget settings",
                        icon = Icons.Default.Widgets,
                        onClick = onNavigateToWidgets
                    )
                }
            }

            // Support Section
            item {
                SectionHeader(title = "Support")
            }
            item {
                GroupedCard {
                    MenuItem(
                        title = "About Nimaz Pro",
                        subtitle = "Version, credits, and info",
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Help & Support",
                        subtitle = "FAQs and contact us",
                        icon = Icons.AutoMirrored.Filled.Help,
                        onClick = onNavigateToHelp
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Share App",
                        subtitle = "Share Nimaz Pro with friends",
                        icon = Icons.Default.Share,
                        onClick = onShareApp
                    )
                    MenuDivider()
                    MenuItem(
                        title = "Rate Us",
                        subtitle = "Rate Nimaz Pro on the store",
                        icon = Icons.Default.Star,
                        onClick = onRateApp
                    )
                }
            }

            // App Info Section
            item {
                AppVersionCard()
            }

            item {
                DeleteAllDataCard(onDeleteAllData = onDeleteAllData)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
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
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun GroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
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
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AppVersionCard() {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }

    Text(
        text = "Nimaz Pro v$versionName",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun DeleteAllDataCard(onDeleteAllData: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Delete All Data")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete All Data") },
            text = {
                Text("This will permanently delete all your data including prayer tracking, bookmarks, tasbih history, and settings. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onDeleteAllData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
