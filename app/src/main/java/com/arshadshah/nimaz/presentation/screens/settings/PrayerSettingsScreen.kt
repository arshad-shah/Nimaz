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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
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
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
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
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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
                            onClick = onNavigateToNotifications
                        )
                        SettingsDivider()
                        SettingItem(
                            icon = Icons.Default.Schedule,
                            label = "Pre-Adhan Reminder",
                            value = "15 minutes before",
                            onClick = onNavigateToNotifications
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
        val calcOptions = CalculationMethod.entries.map { method ->
            SelectionOption(
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
        SelectionDialog(
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
        SelectionDialog(
            title = "Asr Calculation",
            options = listOf(
                SelectionOption("Standard (Shafi'i, Maliki, Hanbali)", "Shadow equals object length"),
                SelectionOption("Hanafi", "Shadow equals twice object length")
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
        SelectionDialog(
            title = "High Latitude Method",
            options = listOf(
                SelectionOption("Middle of the Night", "Split the night in half from sunset to sunrise"),
                SelectionOption("Seventh of the Night", "Use 1/7th of the night for Fajr and Isha"),
                SelectionOption("Twilight Angle", "Use the angle-based method for Fajr and Isha")
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
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        thickness = 0.5.dp
    )
}

data class SelectionOption(
    val title: String,
    val description: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDialog(
    title: String,
    options: List<SelectionOption>,
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(options) { index, option ->
                    val isSelected = index == selectedIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary
                            )
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                if (option.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
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
