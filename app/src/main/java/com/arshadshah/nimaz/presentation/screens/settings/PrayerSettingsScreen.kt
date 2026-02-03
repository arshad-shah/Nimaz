package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazSelectionDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazSelectionOption
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.AsrJuristicMethod
import com.arshadshah.nimaz.presentation.viewmodel.HighLatitudeRule
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prayerState by viewModel.prayerState.collectAsState()
    val locationState by viewModel.locationState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Dialog states for selection screens
    var showCalculationMethodDialog by remember { mutableStateOf(false) }
    var showAsrMethodDialog by remember { mutableStateOf(false) }
    var showHighLatitudeDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Prayer Settings",
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

            // Calculation Method Section
            item {
                NimazSectionHeader(title = "Calculation Method")
            }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        icon = Icons.Default.Schedule,
                        tintIcon = true,
                        title = "Calculation Method",
                        value = prayerState.calculationMethod.displayName(),
                        onClick = { showCalculationMethodDialog = true }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        icon = Icons.Default.WbSunny,
                        title = "Asr Calculation",
                        value = when (prayerState.asrMethod) {
                            AsrJuristicMethod.STANDARD -> "Standard (Shafi'i, Maliki, Hanbali)"
                            AsrJuristicMethod.HANAFI -> "Hanafi"
                        },
                        onClick = { showAsrMethodDialog = true }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        icon = Icons.Default.WbSunny,
                        title = "High Latitude Method",
                        value = when (prayerState.highLatitudeRule) {
                            HighLatitudeRule.MIDDLE_OF_NIGHT -> "Middle of the Night"
                            HighLatitudeRule.SEVENTH_OF_NIGHT -> "Seventh of the Night"
                            HighLatitudeRule.TWILIGHT_ANGLE -> "Twilight Angle"
                        },
                        onClick = { showHighLatitudeDialog = true }
                    )
                }
            }

            // Info Banner
            item {
                NimazBanner(
                    message = buildString {
                        val city = locationState.currentLocation?.city ?: "Your location"
                        append("$city may be at a high latitude. Prayer times may vary significantly during summer months. Consider using Angle Based or One Seventh methods.")
                    },
                    variant = NimazBannerVariant.INFO,
                    icon = Icons.Default.Info
                )
            }

            // Manual Adjustments Section
            item {
                NimazSectionHeader(title = "Manual Adjustments (Minutes)")
            }
            item {
                NimazMenuGroup {
                    NimazNumberStepper(
                        label = "Fajr",
                        value = prayerState.fajrAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("fajr", it))
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazNumberStepper(
                        label = "Sunrise",
                        value = prayerState.sunriseAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("sunrise", it))
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazNumberStepper(
                        label = "Dhuhr",
                        value = prayerState.dhuhrAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("dhuhr", it))
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazNumberStepper(
                        label = "Asr",
                        value = prayerState.asrAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("asr", it))
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazNumberStepper(
                        label = "Maghrib",
                        value = prayerState.maghribAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("maghrib", it))
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazNumberStepper(
                        label = "Isha",
                        value = prayerState.ishaAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("isha", it))
                        }
                    )
                }
            }

            // Notifications Section
            item {
                NimazSectionHeader(title = "Notifications")
            }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Adhan Notifications",
                        value = "All prayers enabled",
                        onClick = onNavigateToNotifications
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        icon = Icons.Default.Schedule,
                        title = "Pre-Adhan Reminder",
                        value = "15 minutes before",
                        onClick = onNavigateToNotifications
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Selection dialogs
    if (showCalculationMethodDialog) {
        val calcOptions = CalculationMethod.entries.map { method ->
            NimazSelectionOption(
                title = method.displayName(),
                description = when (method) {
                    CalculationMethod.MUSLIM_WORLD_LEAGUE -> "Used in Europe, Far East, parts of the US"
                    CalculationMethod.EGYPTIAN -> "Used in Africa, Syria, Lebanon, Malaysia"
                    CalculationMethod.KARACHI -> "Used in Pakistan, Bangladesh, India, Afghanistan"
                    CalculationMethod.UMM_AL_QURA -> "Used in the Arabian Peninsula"
                    CalculationMethod.DUBAI -> "Used in the UAE"
                    CalculationMethod.MOON_SIGHTING_COMMITTEE -> "Used in the UK, parts of Europe"
                    CalculationMethod.NORTH_AMERICA -> "Used in the US and Canada"
                    CalculationMethod.KUWAIT -> "Used in Kuwait"
                    CalculationMethod.QATAR -> "Used in Qatar"
                    CalculationMethod.SINGAPORE -> "Used in Singapore, Malaysia, Indonesia"
                    CalculationMethod.TURKEY -> "Used in Turkey and Central Asia"
                }
            )
        }
        NimazSelectionDialog(
            title = "Calculation Method",
            options = calcOptions,
            selectedIndex = CalculationMethod.entries.indexOf(prayerState.calculationMethod),
            onSelect = { index ->
                viewModel.onEvent(SettingsEvent.SetCalculationMethod(CalculationMethod.entries[index]))
                showCalculationMethodDialog = false
            },
            onDismiss = { showCalculationMethodDialog = false }
        )
    }

    if (showAsrMethodDialog) {
        NimazSelectionDialog(
            title = "Asr Calculation",
            options = listOf(
                NimazSelectionOption("Standard (Shafi'i, Maliki, Hanbali)", "Shadow equals object length"),
                NimazSelectionOption("Hanafi", "Shadow equals twice object length")
            ),
            selectedIndex = AsrJuristicMethod.entries.indexOf(prayerState.asrMethod),
            onSelect = { index ->
                viewModel.onEvent(SettingsEvent.SetAsrMethod(AsrJuristicMethod.entries[index]))
                showAsrMethodDialog = false
            },
            onDismiss = { showAsrMethodDialog = false }
        )
    }

    if (showHighLatitudeDialog) {
        NimazSelectionDialog(
            title = "High Latitude Method",
            options = listOf(
                NimazSelectionOption("Middle of the Night", "Split the night in half from sunset to sunrise"),
                NimazSelectionOption("Seventh of the Night", "Use 1/7th of the night for Fajr and Isha"),
                NimazSelectionOption("Twilight Angle", "Use the angle-based method for Fajr and Isha")
            ),
            selectedIndex = HighLatitudeRule.entries.indexOf(prayerState.highLatitudeRule),
            onSelect = { index ->
                viewModel.onEvent(SettingsEvent.SetHighLatitudeRule(HighLatitudeRule.entries[index]))
                showHighLatitudeDialog = false
            },
            onDismiss = { showHighLatitudeDialog = false }
        )
    }
}
