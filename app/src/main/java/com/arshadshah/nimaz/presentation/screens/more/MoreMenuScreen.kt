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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
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
                title = stringResource(R.string.more),
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
                NimazSectionHeader(title = stringResource(R.string.daily_practice))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.prayer_tracker),
                        subtitle = stringResource(R.string.prayer_tracker_subtitle),
                        icon = Icons.Default.Schedule,
                        onClick = onNavigateToPrayerTracker
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.fasting),
                        subtitle = stringResource(R.string.fasting_subtitle),
                        icon = Icons.Default.Fastfood,
                        onClick = onNavigateToFasting
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.khatam_quran),
                        subtitle = stringResource(R.string.khatam_quran_subtitle),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToKhatam
                    )
                }
            }

            // Learning Section
            item {
                NimazSectionHeader(title = stringResource(R.string.learning))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.allahs_99_names),
                        subtitle = stringResource(R.string.allahs_99_names_subtitle),
                        icon = Icons.Default.Star,
                        onClick = onNavigateToAsmaUlHusna
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.prophets_99_names),
                        subtitle = stringResource(R.string.prophets_99_names_subtitle),
                        icon = Icons.Default.Person,
                        onClick = onNavigateToAsmaUnNabi
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.prophets_of_islam),
                        subtitle = stringResource(R.string.prophets_of_islam_subtitle),
                        icon = Icons.Default.Groups,
                        onClick = onNavigateToProphets
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.hadith),
                        subtitle = stringResource(R.string.hadith_subtitle),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToHadith
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.duas),
                        subtitle = stringResource(R.string.duas_subtitle),
                        icon = Icons.Default.Mosque,
                        onClick = onNavigateToDuas
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.tafseer),
                        subtitle = stringResource(R.string.tafseer_subtitle),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToTafseer
                    )
                }
            }

            // Tools Section
            item {
                NimazSectionHeader(title = stringResource(R.string.tools))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.calendar),
                        subtitle = stringResource(R.string.calendar_subtitle),
                        icon = Icons.Default.CalendarMonth,
                        onClick = onNavigateToCalendar
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.monthly_prayer_times),
                        subtitle = stringResource(R.string.monthly_prayer_times_subtitle),
                        icon = Icons.Default.CalendarMonth,
                        onClick = onNavigateToMonthlyPrayerTimes
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.zakat),
                        subtitle = stringResource(R.string.zakat_subtitle),
                        icon = Icons.Default.Calculate,
                        onClick = onNavigateToZakat
                    )
                }
            }

            // Prayer Settings Section
            item {
                NimazSectionHeader(title = stringResource(R.string.prayer_settings))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.calculation_method),
                        subtitle = stringResource(R.string.calculation_method_menu_subtitle),
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToCalculationMethod
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.notifications),
                        subtitle = stringResource(R.string.notifications_menu_subtitle),
                        icon = Icons.Default.Notifications,
                        onClick = onNavigateToNotifications
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.location),
                        subtitle = stringResource(R.string.location_menu_subtitle),
                        icon = Icons.Default.LocationOn,
                        onClick = onNavigateToLocation
                    )
                }
            }

            // App Settings Section
            item {
                NimazSectionHeader(title = stringResource(R.string.app_settings))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.appearance),
                        subtitle = stringResource(R.string.appearance_menu_subtitle),
                        icon = Icons.Default.DarkMode,
                        onClick = onNavigateToAppearance
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.language),
                        subtitle = stringResource(R.string.language_menu_subtitle),
                        icon = Icons.Default.Language,
                        onClick = onNavigateToLanguage
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.widgets),
                        subtitle = stringResource(R.string.widgets_menu_subtitle),
                        icon = Icons.Default.Widgets,
                        onClick = onNavigateToWidgets
                    )
                }
            }

            // Support Section
            item {
                NimazSectionHeader(title = stringResource(R.string.support))
            }
            item {
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.about_nimaz),
                        subtitle = stringResource(R.string.about_nimaz_subtitle),
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.help_support),
                        subtitle = stringResource(R.string.help_support_subtitle),
                        icon = Icons.AutoMirrored.Filled.Help,
                        onClick = onNavigateToHelp
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.share_app),
                        subtitle = stringResource(R.string.share_app_subtitle),
                        icon = Icons.Default.Share,
                        onClick = onShareApp
                    )
                    NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                    NimazMenuItem(
                        title = stringResource(R.string.rate_us),
                        subtitle = stringResource(R.string.rate_us_subtitle),
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
        text = stringResource(R.string.version_format, versionName),
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
        Text(stringResource(R.string.delete_all_data))
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.delete_all_data_dialog_title)) },
            text = {
                Text(stringResource(R.string.delete_all_data_dialog_message))
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
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
