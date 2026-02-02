package com.arshadshah.nimaz.presentation.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuScreen(
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
    onNavigateToTafseer: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToCalculationMethod: () -> Unit,
    onNavigateToPrayerTracker: () -> Unit,
    onNavigateToMonthlyPrayerTimes: () -> Unit,
    onNavigateToKhatam: () -> Unit,
    onNavigateToAsmaUlHusna: () -> Unit,
    onNavigateToAsmaUnNabi: () -> Unit,
    onNavigateToProphets: () -> Unit,
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

            // Daily Practice Section
            item {
                NimazSectionHeader(title = "Daily Practice")
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = "Prayer Tracker",
                        subtitle = "Track your daily prayers & qada",
                        icon = Icons.Default.Schedule,
                        onClick = onNavigateToPrayerTracker
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Fasting",
                        subtitle = "Fasting tracker and schedule",
                        icon = Icons.Default.Fastfood,
                        onClick = onNavigateToFasting
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Khatam Quran",
                        subtitle = "Track your Quran completion",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToKhatam
                    )
                }
            }

            // Learning Section
            item {
                NimazSectionHeader(title = "Learning")
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = "Allah's 99 Names",
                        subtitle = "Learn the beautiful names of Allah",
                        icon = Icons.Default.Star,
                        onClick = onNavigateToAsmaUlHusna
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Prophet's 99 Names",
                        subtitle = "Names and attributes of Prophet Muhammad (PBUH)",
                        icon = Icons.Default.Person,
                        onClick = onNavigateToAsmaUnNabi
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Prophets of Islam",
                        subtitle = "Stories and lessons from 25 prophets",
                        icon = Icons.Default.Groups,
                        onClick = onNavigateToProphets
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Hadith",
                        subtitle = "Authentic hadith collections",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToHadith
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Duas",
                        subtitle = "Daily duas and supplications",
                        icon = Icons.Default.Mosque,
                        onClick = onNavigateToDuas
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Tafseer",
                        subtitle = "Quran interpretation and commentary",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToTafseer
                    )
                }
            }

            // Tools Section
            item {
                NimazSectionHeader(title = "Tools")
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = "Calendar",
                        subtitle = "Islamic calendar and events",
                        icon = Icons.Default.CalendarMonth,
                        onClick = onNavigateToCalendar
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Monthly Prayer Times",
                        subtitle = "Prayer times for the month",
                        icon = Icons.Default.CalendarMonth,
                        onClick = onNavigateToMonthlyPrayerTimes
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Zakat",
                        subtitle = "Zakat calculator",
                        icon = Icons.Default.Calculate,
                        onClick = onNavigateToZakat
                    )
                }
            }

            // Prayer Settings Section
            item {
                NimazSectionHeader(title = "Prayer Settings")
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = "Calculation Method",
                        subtitle = "Prayer time calculation parameters",
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToCalculationMethod
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Notifications",
                        subtitle = "Prayer alerts and reminders",
                        icon = Icons.Default.Notifications,
                        onClick = onNavigateToNotifications
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Location",
                        subtitle = "Manage locations for prayer times",
                        icon = Icons.Default.LocationOn,
                        onClick = onNavigateToLocation
                    )
                }
            }

            // App Settings Section
            item {
                NimazSectionHeader(title = "App Settings")
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = "Appearance",
                        subtitle = "Theme, colors, and display",
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
                        subtitle = "Home screen widget settings",
                        icon = Icons.Default.Widgets,
                        onClick = onNavigateToWidgets
                    )
                }
            }

            // Support Section
            item {
                NimazSectionHeader(title = "Support")
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = "About Nimaz",
                        subtitle = "Version, credits, and info",
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Help & Support",
                        subtitle = "FAQs and contact us",
                        icon = Icons.AutoMirrored.Filled.Help,
                        onClick = onNavigateToHelp
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Share App",
                        subtitle = "Share Nimaz with friends",
                        icon = Icons.Default.Share,
                        onClick = onShareApp
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = "Rate Us",
                        subtitle = "Rate Nimaz on the store",
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
private fun AppVersionCard() {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (e: Exception) {
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

@Preview(showBackground = true, widthDp = 400, name = "DeleteAllDataCard")
@Composable
private fun DeleteAllDataCardPreview() {
    NimazTheme {
        DeleteAllDataCard(
            onDeleteAllData = {}
        )
    }
}
