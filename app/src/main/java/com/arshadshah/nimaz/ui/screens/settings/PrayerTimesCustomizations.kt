package com.arshadshah.nimaz.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.getAsrJuristic
import com.arshadshah.nimaz.constants.AppConstants.getHighLatitudes
import com.arshadshah.nimaz.constants.AppConstants.getMethods
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.BannerDuration
import com.arshadshah.nimaz.ui.components.common.BannerSmall
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.ui.components.common.HeaderWithIcon
import com.arshadshah.nimaz.ui.components.common.SettingsSelectionDialog
import com.arshadshah.nimaz.viewModel.PrayerTimesSettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesSettingsTopSection(
    onNavigateBack: () -> Unit,
) {
    Column {
        LargeTopAppBar(
            title = {
                Column {
                    Text(
                        text = "Prayer Times",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize calculation methods and adjustments",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("backButton")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_icon),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.shadow(elevation = 0.dp)
        )
    }
}


@Composable
private fun PrayerParametersSection(
    calculationMethod: String,
    autoParams: Boolean,
    madhab: String,
    highLatitudeRule: String,
    onAutoParametersChange: (Boolean) -> Unit,
    onCalculationMethodChange: (String) -> Unit,
    onMadhabChange: (String) -> Unit,
    onHighLatitudeRuleChange: (String) -> Unit,
) {

    val showInfoDialog = remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                HeaderWithIcon(
                    title = "Prayer Parameters",
                    contentDescription = "Prayer Parameters",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                FilledIconButton(
                    onClick = { showInfoDialog.value = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.info_icon),
                        contentDescription = "Information",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Auto Parameters Toggle
            Surface(
                onClick = { onAutoParametersChange(!autoParams) },
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (autoParams) "Auto Calculation" else "Manual Calculation",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Auto angles are Experimental",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = autoParams,
                        onCheckedChange = onAutoParametersChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = !autoParams,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                CalculationMethodItem(
                    calculationMethod = calculationMethod,
                    onCalculationMethodChange = onCalculationMethodChange
                )
            }

            MadhabItem(
                madhab = madhab,
                onMadhabChange = onMadhabChange
            )

            HighLatitudeRuleItem(
                highLatitudeRule = highLatitudeRule,
                onHighLatitudeRuleChange = onHighLatitudeRuleChange
            )
        }
    }

    InfoDialog(
        title = "Prayer Parameters",
        description = "Customize the calculation method, madhab, and high latitude rule used to calculate prayer times. " +
                "You can also enable or disable automatic calculation of prayer angles.",
        showDialog = showInfoDialog.value,
        onDismiss = { showInfoDialog.value = false }
    )

}

@Composable
private fun PrayerAnglesSection(
    fajrAngle: String,
    ishaAngle: String,
    ishaInterval: String,
    ishaAngleVisible: Boolean,
    onFajrAngleChange: (Int) -> Unit,
    onIshaAngleChange: (Int) -> Unit
) {
    val showInfoDialog = remember { mutableStateOf(false) }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Prayer Angles",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                FilledIconButton(
                    onClick = { showInfoDialog.value = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.info_icon),
                        contentDescription = "Information",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AngleItem(
                angle = fajrAngle,
                onAngleChange = onFajrAngleChange,
                icon = R.drawable.fajr_angle
            )

            if (ishaAngleVisible) {
                AngleItem(
                    angle = ishaAngle,
                    onAngleChange = onIshaAngleChange,
                    icon = R.drawable.isha_angle
                )
            } else {
                // Isha Interval display
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.isha_angle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Isha Interval",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$ishaInterval minutes after Maghrib",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

        }
    }
    InfoDialog(
        title = "Prayer Angles",
        description = "Adjust the angles used to calculate Fajr and Isha prayer times. " +
                "You can also set a fixed interval for Isha prayer if the angle is not used.",
        showDialog = showInfoDialog.value,
        onDismiss = { showInfoDialog.value = false }
    )
}


@Composable
private fun AngleItem(
    angle: String,
    onAngleChange: (Int) -> Unit,
    icon: Int,
) {
    val showDialog = remember { mutableStateOf(false) }

    Surface(
        onClick = { showDialog.value = true },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(12.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Fajr Angle",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$angle°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    NumberPickerDialog(
        title = "Adjust Fajr Angle",
        currentValue = angle,
        onValueSelected = onAngleChange,
        onDismiss = { showDialog.value = false },
        showDialog = showDialog.value,
        valueRange = (-30..30),
        valuePostfix = "°"
    )
}


@Composable
private fun CalculationMethodItem(
    calculationMethod: String,
    onCalculationMethodChange: (String) -> Unit
) {

    val showDialog = remember { mutableStateOf(false) }
    val methods = remember { getMethods() }
    Surface(
        onClick = { showDialog.value = true },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.time_calculation),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(12.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Calculation Method",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = getMethods()[calculationMethod] ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    SettingsSelectionDialog(
        title = "Select Calculation Method",
        options = methods,
        selectedOption = calculationMethod,
        onOptionSelected = onCalculationMethodChange,
        onDismiss = { showDialog.value = false },
        showDialog = showDialog.value
    )
}


@Composable
private fun InfoDialog(
    title: String,
    description: String,
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialogNimaz(
            title = title,
            contentDescription = "Information",
            confirmButtonText = "Got it",
            showDismissButton = false,
            onDismiss = onDismiss,
            onConfirm = onDismiss,
            onDismissRequest = onDismiss,
            contentToShow = {
                Column {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun PrayerTimeAdjustmentsSection(
    fajrOffset: String,
    sunriseOffset: String,
    dhuhrOffset: String,
    asrOffset: String,
    maghribOffset: String,
    ishaOffset: String,
    onOffsetChange: (prayer: String, offset: String) -> Unit
) {
    val showInfoDialog = remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Prayer Time Adjustments",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                FilledIconButton(
                    onClick = { showInfoDialog.value = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.info_icon),
                        contentDescription = "Information",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            val prayerTimes = listOf(
                PrayerTimeInfo("Fajr", fajrOffset, R.drawable.fajr_icon),
                PrayerTimeInfo("Sunrise", sunriseOffset, R.drawable.sunrise_icon),
                PrayerTimeInfo("Dhuhr", dhuhrOffset, R.drawable.dhuhr_icon),
                PrayerTimeInfo("Asr", asrOffset, R.drawable.asr_icon),
                PrayerTimeInfo("Maghrib", maghribOffset, R.drawable.maghrib_icon),
                PrayerTimeInfo("Isha", ishaOffset, R.drawable.isha_icon)
            )

            prayerTimes.forEach { prayerTime ->
                PrayerTimeAdjustmentItem(
                    name = prayerTime.name,
                    offset = prayerTime.offset,
                    icon = prayerTime.icon,
                    onOffsetChange = { offset ->
                        onOffsetChange(prayerTime.name, offset)
                    }
                )
            }
        }
    }

    InfoDialog(
        title = "Prayer Time Adjustments",
        description = "Adjust the timing of individual prayers to account for local variations or preferences. " +
                "Positive values will delay the prayer time, while negative values will make it earlier. " +
                "These adjustments are applied after the main calculation method.",
        showDialog = showInfoDialog.value,
        onDismiss = { showInfoDialog.value = false }
    )
}

private data class PrayerTimeInfo(
    val name: String,
    val offset: String,
    @DrawableRes val icon: Int
)


@Composable
private fun PrayerTimeAdjustmentItem(
    name: String,
    offset: String,
    @DrawableRes icon: Int,
    onOffsetChange: (String) -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }

    Surface(
        onClick = { showDialog.value = true },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(12.dp)
                        .then(if (name == "Sunrise") Modifier.clip(CircleShape) else Modifier),
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$name Time",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (offset == "0") "No adjustment" else "$offset minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    NumberPickerDialog(
        title = "Adjust $name Time",
        currentValue = offset,
        onValueSelected = { onOffsetChange(it.toString()) },
        onDismiss = { showDialog.value = false },
        showDialog = showDialog.value,
        valueRange = (-60..60),
        valuePostfix = ""
    )
}


@Composable
private fun MadhabItem(
    madhab: String,
    onMadhabChange: (String) -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }
    val madhabs = remember { getAsrJuristic() }
    Surface(
        onClick = { showDialog.value = true },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.school),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(12.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Madhab",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = getAsrJuristic()[madhab] ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    SettingsSelectionDialog(
        title = "Select Madhab",
        options = madhabs,
        selectedOption = madhab,
        onOptionSelected = onMadhabChange,
        onDismiss = { showDialog.value = false },
        showDialog = showDialog.value
    )
}

@Composable
private fun HighLatitudeRuleItem(
    highLatitudeRule: String,
    onHighLatitudeRuleChange: (String) -> Unit
) {

    val showDialog = remember { mutableStateOf(false) }
    val rules = remember { getHighLatitudes() }

    Surface(
        onClick = { showDialog.value = true },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.high_latitude),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(12.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "High Latitude Rule",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = getHighLatitudes()[highLatitudeRule] ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    SettingsSelectionDialog(
        title = "Select High Latitude Rule",
        options = rules,
        selectedOption = highLatitudeRule,
        onOptionSelected = onHighLatitudeRuleChange,
        onDismiss = { showDialog.value = false },
        showDialog = showDialog.value
    )
}

@Composable
fun PrayerTimesCustomizations(
    navController: NavController,
    viewModel: PrayerTimesSettingsViewModel = hiltViewModel()
) {
    val error = viewModel.error.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()

    // Collect all StateFlows
    val calculationMethod = viewModel.calculationMethod.collectAsState()
    val autoParams = viewModel.autoParams.collectAsState()
    val madhab = viewModel.madhab.collectAsState()
    val highLatitudeRule = viewModel.highLatitude.collectAsState()
    val fajrAngle = viewModel.fajrAngle.collectAsState()
    val ishaAngle = viewModel.ishaAngle.collectAsState()
    val ishaInterval = viewModel.ishaInterval.collectAsState()
    val ishaAngleVisible = viewModel.ishaAngleVisibility.collectAsState()

    // Prayer time adjustments
    val fajrOffset = viewModel.fajrOffset.collectAsState()
    val sunriseOffset = viewModel.sunriseOffset.collectAsState()
    val dhuhrOffset = viewModel.dhuhrOffset.collectAsState()
    val asrOffset = viewModel.asrOffset.collectAsState()
    val maghribOffset = viewModel.maghribOffset.collectAsState()
    val ishaOffset = viewModel.ishaOffset.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.handleEvent(PrayerTimesSettingsViewModel.SettingsEvent.LoadSettings)
    }

    Scaffold(
        topBar = {
            PrayerTimesSettingsTopSection(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Show loading indicator if needed
            if (isLoading.value) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            // Show error banner if needed
            if (error.value != null) {
                BannerSmall(
                    variant = BannerVariant.Error,
                    title = "Error",
                    message = error.value ?: "",
                    dismissable = true,
                    showFor = BannerDuration.FOREVER.value
                )
            }

            // Prayer Parameters Section
            PrayerParametersSection(
                calculationMethod = calculationMethod.value,
                autoParams = autoParams.value,
                madhab = madhab.value,
                highLatitudeRule = highLatitudeRule.value,
                onAutoParametersChange = { newValue ->
                    viewModel.handleEvent(
                        PrayerTimesSettingsViewModel.SettingsEvent.AutoParameters(newValue)
                    )
                },
                onCalculationMethodChange = { method ->
                    viewModel.handleEvent(
                        PrayerTimesSettingsViewModel.SettingsEvent.CalculationMethod(method)
                    )
                },
                onMadhabChange = { newMadhab ->
                    viewModel.handleEvent(
                        PrayerTimesSettingsViewModel.SettingsEvent.Madhab(newMadhab)
                    )
                },
                onHighLatitudeRuleChange = { rule ->
                    viewModel.handleEvent(
                        PrayerTimesSettingsViewModel.SettingsEvent.HighLatitude(rule)
                    )
                }
            )

            // Prayer Angles Section
            PrayerAnglesSection(
                fajrAngle = fajrAngle.value,
                ishaAngle = ishaAngle.value,
                ishaInterval = ishaInterval.value,
                ishaAngleVisible = ishaAngleVisible.value,
                onFajrAngleChange = { angle ->
                    viewModel.handleEvent(
                        PrayerTimesSettingsViewModel.SettingsEvent.FajrAngle(angle.toString())
                    )
                },
                onIshaAngleChange = { angle ->
                    viewModel.handleEvent(
                        PrayerTimesSettingsViewModel.SettingsEvent.IshaAngle(angle.toString())
                    )
                }
            )

            // Prayer Time Adjustments Section
            PrayerTimeAdjustmentsSection(
                fajrOffset = fajrOffset.value,
                sunriseOffset = sunriseOffset.value,
                dhuhrOffset = dhuhrOffset.value,
                asrOffset = asrOffset.value,
                maghribOffset = maghribOffset.value,
                ishaOffset = ishaOffset.value,
                onOffsetChange = { prayer, offset ->
                    val event = when (prayer) {
                        "Fajr" -> PrayerTimesSettingsViewModel.SettingsEvent.FajrOffset(offset)
                        "Sunrise" -> PrayerTimesSettingsViewModel.SettingsEvent.SunriseOffset(offset)
                        "Dhuhr" -> PrayerTimesSettingsViewModel.SettingsEvent.DhuhrOffset(offset)
                        "Asr" -> PrayerTimesSettingsViewModel.SettingsEvent.AsrOffset(offset)
                        "Maghrib" -> PrayerTimesSettingsViewModel.SettingsEvent.MaghribOffset(offset)
                        "Isha" -> PrayerTimesSettingsViewModel.SettingsEvent.IshaOffset(offset)
                        else -> null
                    }
                    event?.let { viewModel.handleEvent(it) }
                }
            )

            // Add some bottom padding
            Spacer(modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberPickerDialog(
    title: String,
    currentValue: String,
    onValueSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    showDialog: Boolean,
    valueRange: IntRange = (-60..60),
    valuePostfix: String = "",
    neutralValue: Int = 0
) {
    if (showDialog) {
        var selectedValue by remember(currentValue) {
            mutableIntStateOf(currentValue.replace(valuePostfix, "").toIntOrNull() ?: neutralValue)
        }

        AlertDialogNimaz(
            title = title,
            description = "Select a value to $title",
            contentDescription = "Adjust $title",
            confirmButtonText = "Apply",
            onDismiss = onDismiss,
            onConfirm = {
                onValueSelected(selectedValue)
                onDismiss()
            },
            onDismissRequest = onDismiss,
            contentToShow = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // Number display and quick controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decrease button
                        FilledIconButton(
                            onClick = {
                                if (selectedValue > valueRange.first) {
                                    selectedValue--
                                }
                            },
                            enabled = selectedValue > valueRange.first,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Decrease"
                            )
                        }

                        // Current value display
                        Surface(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = "$selectedValue$valuePostfix",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )
                        }

                        // Increase button
                        FilledIconButton(
                            onClick = {
                                if (selectedValue < valueRange.last) {
                                    selectedValue++
                                }
                            },
                            enabled = selectedValue < valueRange.last,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Increase"
                            )
                        }
                    }


                    // Reset button (if not at neutral value)
                    AnimatedVisibility(
                        visible = selectedValue != neutralValue,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OutlinedButton(
                            onClick = { selectedValue = neutralValue },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset to $neutralValue$valuePostfix")
                        }
                    }
                }

            }
        )
    }
}