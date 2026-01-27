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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Calculation Method
            item {
                SectionHeader(
                    title = "Calculation Method",
                    icon = Icons.Default.Calculate
                )
            }

            item {
                CalculationMethodCard(
                    selectedMethod = prayerState.calculationMethod,
                    onMethodSelected = { viewModel.onEvent(SettingsEvent.SetCalculationMethod(it)) }
                )
            }

            // Jurisprudence
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Jurisprudence (Madhab)",
                    icon = Icons.Default.School
                )
            }

            item {
                Text(
                    text = "Affects Asr prayer time calculation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                AsrMethodCard(
                    selectedMethod = prayerState.asrMethod,
                    onMethodSelected = { viewModel.onEvent(SettingsEvent.SetAsrMethod(it)) }
                )
            }

            // High Latitude Rule
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "High Latitude Rule",
                    icon = Icons.Default.AccessTime
                )
            }

            item {
                Text(
                    text = "For locations with extreme day/night lengths",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                HighLatitudeRuleCard(
                    selectedRule = prayerState.highLatitudeRule,
                    onRuleSelected = { viewModel.onEvent(SettingsEvent.SetHighLatitudeRule(it)) }
                )
            }

            // Manual Adjustments
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Manual Adjustments",
                    icon = Icons.Default.Tune
                )
            }

            item {
                Text(
                    text = "Fine-tune prayer times (minutes)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                PrayerAdjustmentCard(
                    prayerName = "Fajr",
                    adjustment = prayerState.fajrAdjustment,
                    onAdjustmentChange = { viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("fajr", it)) }
                )
            }

            item {
                PrayerAdjustmentCard(
                    prayerName = "Sunrise",
                    adjustment = prayerState.sunriseAdjustment,
                    onAdjustmentChange = { viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("sunrise", it)) }
                )
            }

            item {
                PrayerAdjustmentCard(
                    prayerName = "Dhuhr",
                    adjustment = prayerState.dhuhrAdjustment,
                    onAdjustmentChange = { viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("dhuhr", it)) }
                )
            }

            item {
                PrayerAdjustmentCard(
                    prayerName = "Asr",
                    adjustment = prayerState.asrAdjustment,
                    onAdjustmentChange = { viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("asr", it)) }
                )
            }

            item {
                PrayerAdjustmentCard(
                    prayerName = "Maghrib",
                    adjustment = prayerState.maghribAdjustment,
                    onAdjustmentChange = { viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("maghrib", it)) }
                )
            }

            item {
                PrayerAdjustmentCard(
                    prayerName = "Isha",
                    adjustment = prayerState.ishaAdjustment,
                    onAdjustmentChange = { viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("isha", it)) }
                )
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
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NimazColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CalculationMethodCard(
    selectedMethod: CalculationMethod,
    onMethodSelected: (CalculationMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            CalculationMethod.entries.forEach { method ->
                MethodOption(
                    displayName = method.displayName(),
                    isSelected = selectedMethod == method,
                    onClick = { onMethodSelected(method) }
                )
            }
        }
    }
}

@Composable
private fun AsrMethodCard(
    selectedMethod: AsrJuristicMethod,
    onMethodSelected: (AsrJuristicMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            AsrJuristicMethod.entries.forEach { method ->
                val displayName = when (method) {
                    AsrJuristicMethod.STANDARD -> "Standard (Shafi'i, Maliki, Hanbali)"
                    AsrJuristicMethod.HANAFI -> "Hanafi"
                }
                MethodOption(
                    displayName = displayName,
                    isSelected = selectedMethod == method,
                    onClick = { onMethodSelected(method) }
                )
            }
        }
    }
}

@Composable
private fun HighLatitudeRuleCard(
    selectedRule: HighLatitudeRule,
    onRuleSelected: (HighLatitudeRule) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            HighLatitudeRule.entries.forEach { rule ->
                val displayName = when (rule) {
                    HighLatitudeRule.MIDDLE_OF_NIGHT -> "Middle of the Night"
                    HighLatitudeRule.SEVENTH_OF_NIGHT -> "Seventh of the Night"
                    HighLatitudeRule.TWILIGHT_ANGLE -> "Twilight Angle"
                }
                MethodOption(
                    displayName = displayName,
                    isSelected = selectedRule == rule,
                    onClick = { onRuleSelected(rule) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MethodOption(
    displayName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            NimazColors.Primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = NimazColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PrayerAdjustmentCard(
    prayerName: String,
    adjustment: Int,
    onAdjustmentChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prayerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = if (adjustment >= 0) "+$adjustment min" else "$adjustment min",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = NimazColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = adjustment.toFloat(),
                onValueChange = { onAdjustmentChange(it.toInt()) },
                valueRange = -30f..30f,
                steps = 59
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "-30",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+30",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
