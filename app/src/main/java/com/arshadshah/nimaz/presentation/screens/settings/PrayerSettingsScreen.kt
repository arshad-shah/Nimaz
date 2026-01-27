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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.AsrJuristicMethod
import com.arshadshah.nimaz.presentation.viewmodel.HighLatitudeRule
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSettingsScreen(
    onNavigateBack: () -> Unit,
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
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Location Section
            item {
                SettingsSection(title = "LOCATION") {
                    SettingsCard {
                        SettingItem(
                            icon = Icons.Default.LocationOn,
                            iconTinted = true,
                            label = "Current Location",
                            value = locationState.currentLocation?.let {
                                listOfNotNull(it.city, it.country).joinToString(", ")
                            } ?: "Not set",
                            onClick = { }
                        )
                        SettingsDivider()
                        SettingToggleItem(
                            icon = Icons.Default.Public,
                            label = "Auto-detect Location",
                            value = "Updates automatically",
                            checked = locationState.autoDetectLocation,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.SetAutoDetectLocation(it))
                            }
                        )
                        SettingsDivider()
                        SettingItem(
                            icon = Icons.Default.Edit,
                            label = "Set Manual Location",
                            value = "Search city or coordinates",
                            onClick = { }
                        )
                    }
                }
            }

            // Calculation Method Section
            item {
                SettingsSection(title = "CALCULATION METHOD") {
                    SettingsCard {
                        SettingItem(
                            icon = Icons.Default.Schedule,
                            iconTinted = true,
                            label = "Calculation Method",
                            value = prayerState.calculationMethod.displayName(),
                            onClick = { showCalculationMethodDialog = true }
                        )
                        SettingsDivider()
                        SettingItem(
                            icon = Icons.Default.WbSunny,
                            label = "Asr Calculation",
                            value = when (prayerState.asrMethod) {
                                AsrJuristicMethod.STANDARD -> "Standard (Shafi'i, Maliki, Hanbali)"
                                AsrJuristicMethod.HANAFI -> "Hanafi"
                            },
                            onClick = { showAsrMethodDialog = true }
                        )
                        SettingsDivider()
                        SettingItem(
                            icon = Icons.Default.WbSunny,
                            label = "High Latitude Method",
                            value = when (prayerState.highLatitudeRule) {
                                HighLatitudeRule.MIDDLE_OF_NIGHT -> "Middle of the Night"
                                HighLatitudeRule.SEVENTH_OF_NIGHT -> "Seventh of the Night"
                                HighLatitudeRule.TWILIGHT_ANGLE -> "Twilight Angle"
                            },
                            onClick = { showHighLatitudeDialog = true }
                        )
                    }
                }
            }

            // Info Banner
            item {
                InfoBanner(
                    text = buildString {
                        val city = locationState.currentLocation?.city ?: "Your location"
                        append("$city may be at a high latitude. Prayer times may vary significantly during summer months. Consider using Angle Based or One Seventh methods.")
                    }
                )
            }

            // Manual Adjustments Section
            item {
                SettingsSection(title = "MANUAL ADJUSTMENTS (MINUTES)") {
                    SettingsCard {
                        AdjustmentRow(
                            label = "Fajr",
                            value = prayerState.fajrAdjustment,
                            onValueChange = {
                                viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("fajr", it))
                            }
                        )
                        SettingsDivider()
                        AdjustmentRow(
                            label = "Sunrise",
                            value = prayerState.sunriseAdjustment,
                            onValueChange = {
                                viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("sunrise", it))
                            }
                        )
                        SettingsDivider()
                        AdjustmentRow(
                            label = "Dhuhr",
                            value = prayerState.dhuhrAdjustment,
                            onValueChange = {
                                viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("dhuhr", it))
                            }
                        )
                        SettingsDivider()
                        AdjustmentRow(
                            label = "Asr",
                            value = prayerState.asrAdjustment,
                            onValueChange = {
                                viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("asr", it))
                            }
                        )
                        SettingsDivider()
                        AdjustmentRow(
                            label = "Maghrib",
                            value = prayerState.maghribAdjustment,
                            onValueChange = {
                                viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("maghrib", it))
                            }
                        )
                        SettingsDivider()
                        AdjustmentRow(
                            label = "Isha",
                            value = prayerState.ishaAdjustment,
                            onValueChange = {
                                viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("isha", it))
                            }
                        )
                    }
                }
            }

            // Notifications Section
            item {
                SettingsSection(title = "NOTIFICATIONS") {
                    SettingsCard {
                        SettingItem(
                            icon = Icons.Default.Notifications,
                            label = "Adhan Notifications",
                            value = "All prayers enabled",
                            onClick = { }
                        )
                        SettingsDivider()
                        SettingItem(
                            icon = Icons.Default.Schedule,
                            label = "Pre-Adhan Reminder",
                            value = "15 minutes before",
                            onClick = { }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Selection dialogs
    if (showCalculationMethodDialog) {
        SelectionDialog(
            title = "Calculation Method",
            options = CalculationMethod.entries.map { it.displayName() },
            selectedIndex = CalculationMethod.entries.indexOf(prayerState.calculationMethod),
            onSelect = { index ->
                viewModel.onEvent(SettingsEvent.SetCalculationMethod(CalculationMethod.entries[index]))
                showCalculationMethodDialog = false
            },
            onDismiss = { showCalculationMethodDialog = false }
        )
    }

    if (showAsrMethodDialog) {
        SelectionDialog(
            title = "Asr Calculation",
            options = listOf("Standard (Shafi'i, Maliki, Hanbali)", "Hanafi"),
            selectedIndex = AsrJuristicMethod.entries.indexOf(prayerState.asrMethod),
            onSelect = { index ->
                viewModel.onEvent(SettingsEvent.SetAsrMethod(AsrJuristicMethod.entries[index]))
                showAsrMethodDialog = false
            },
            onDismiss = { showAsrMethodDialog = false }
        )
    }

    if (showHighLatitudeDialog) {
        SelectionDialog(
            title = "High Latitude Method",
            options = listOf("Middle of the Night", "Seventh of the Night", "Twilight Angle"),
            selectedIndex = HighLatitudeRule.entries.indexOf(prayerState.highLatitudeRule),
            onSelect = { index ->
                viewModel.onEvent(SettingsEvent.SetHighLatitudeRule(HighLatitudeRule.entries[index]))
                showHighLatitudeDialog = false
            },
            onDismiss = { showHighLatitudeDialog = false }
        )
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
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5,
            modifier = Modifier.padding(start = 5.dp, bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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

@Composable
private fun SettingItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconTinted: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (iconTinted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (iconTinted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(15.dp))

        // Label and value
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Arrow
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingToggleItem(
    icon: ImageVector,
    label: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(15.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun AdjustmentRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Minus button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { onValueChange(value - 1) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u2212",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Value display
            Text(
                text = if (value > 0) "+$value" else "$value",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(50.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Plus button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { onValueChange(value + 1) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun InfoBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(15.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        thickness = 0.5.dp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(index) }
                            .background(
                                if (index == selectedIndex)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (index == selectedIndex) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
