package com.arshadshah.nimaz.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.getMethods
import com.arshadshah.nimaz.ui.components.common.BannerDuration
import com.arshadshah.nimaz.ui.components.common.BannerLarge
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.ui.components.common.ToolTip
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.settings.SettingsGroup
import com.arshadshah.nimaz.ui.components.settings.SettingsList
import com.arshadshah.nimaz.ui.components.settings.SettingsMenuLink
import com.arshadshah.nimaz.ui.components.settings.SettingsNumberPickerDialog
import com.arshadshah.nimaz.ui.components.settings.SettingsSwitch
import com.arshadshah.nimaz.viewModel.PrayerTimesSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesCustomizations(navController: NavController) {
    val settingViewModel: PrayerTimesSettingsViewModel = hiltViewModel()

    // Constants and Maps
    val mapOfMadhabs = AppConstants.getAsrJuristic()
    val mapOfHighLatitudeRules = AppConstants.getHighLatitudes()

    val error = settingViewModel.error.collectAsState()
    val isLoading = settingViewModel.isLoading.collectAsState()

    // Collect all StateFlows
    val calculationMethod = settingViewModel.calculationMethod.collectAsState()
    val autoParams = settingViewModel.autoParams.collectAsState()
    val madhab = settingViewModel.madhab.collectAsState()
    val highLatitudeRule = settingViewModel.highLatitude.collectAsState()
    val fajrAngle = settingViewModel.fajrAngle.collectAsState()
    val ishaAngle = settingViewModel.ishaAngle.collectAsState()
    val ishaInterval = settingViewModel.ishaInterval.collectAsState()
    val ishaAngleVisible = settingViewModel.ishaAngleVisibility.collectAsState()

    // Prayer time adjustments
    val fajrOffset = settingViewModel.fajrOffset.collectAsState()
    val sunriseOffset = settingViewModel.sunriseOffset.collectAsState()
    val dhuhrOffset = settingViewModel.dhuhrOffset.collectAsState()
    val asrOffset = settingViewModel.asrOffset.collectAsState()
    val maghribOffset = settingViewModel.maghribOffset.collectAsState()
    val ishaOffset = settingViewModel.ishaOffset.collectAsState()

    // Load settings when the screen is first displayed
    LaunchedEffect(Unit) {
        settingViewModel.handleEvent(PrayerTimesSettingsViewModel.SettingsEvent.LoadSettings)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Prayer Times Settings")
                },
                navigationIcon = {
                    OutlinedIconButton(
                        modifier = Modifier
                            .testTag("backButton")
                            .padding(start = 8.dp),
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        // Main scrollable content
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .testTag(AppConstants.TEST_TAG_PRAYER_TIMES_CUSTOMIZATION)
        ) {
            if (isLoading.value) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (error.value != null) {
                val dismissBanner = remember { mutableStateOf(true) }
                BannerLarge(
                    variant = BannerVariant.Error,
                    title = "Error",
                    message = error.value,
                    showFor = BannerDuration.FOREVER.value,
                    isOpen = dismissBanner,
                    onDismiss = {
                        dismissBanner.value = true
                        settingViewModel.handleEvent(PrayerTimesSettingsViewModel.SettingsEvent.ClearError)
                    }
                )
            }
            // Prayer Parameters Section
            SettingsGroup(
                title = {
                    SettingsGroupHeader(
                        title = "Prayer Parameters",
                        tipText = "Prayer times Calculation Parameters"
                    )
                }
            ) {
                PrayerParametersContent(
                    calculationMethod = calculationMethod.value,
                    autoParams = autoParams.value,
                    madhab = madhab.value,
                    highLatitudeRule = highLatitudeRule.value,
                    mapOfMadhabs = mapOfMadhabs,
                    mapOfHighLatitudeRules = mapOfHighLatitudeRules,
                    onCalculationMethodChange = { newMethod: String ->
                        settingViewModel.handleEvent(
                            PrayerTimesSettingsViewModel.SettingsEvent.CalculationMethod(newMethod)
                        )
                    },
                    onAutoParametersChange = { newAutoParams: Boolean ->
                        settingViewModel.handleEvent(
                            PrayerTimesSettingsViewModel.SettingsEvent.AutoParameters(newAutoParams)
                        )
                    },
                    onMadhabChange = { newMadhab ->
                        settingViewModel.handleEvent(
                            PrayerTimesSettingsViewModel.SettingsEvent.Madhab(newMadhab)
                        )
                    },
                    onHighLatitudeRuleChange = { newRule ->
                        settingViewModel.handleEvent(
                            PrayerTimesSettingsViewModel.SettingsEvent.HighLatitude(newRule)
                        )
                    }
                )
            }

            // Prayer Angles Section
            SettingsGroup(
                title = {
                    SettingsGroupHeader(
                        title = "Prayer Angles",
                        tipText = "Angles of the sun used to calculate Fajr and Isha prayer times"
                    )
                }
            ) {
                PrayerAnglesContent(
                    fajrAngle = fajrAngle.value,
                    ishaAngle = ishaAngle.value,
                    ishaInterval = ishaInterval.value,
                    ishaAngleVisible = ishaAngleVisible.value,
                    onFajrAngleChange = { angle ->
                        settingViewModel.handleEvent(
                            PrayerTimesSettingsViewModel.SettingsEvent.FajrAngle(angle.toString())
                        )
                    },
                    onIshaAngleChange = { angle ->
                        settingViewModel.handleEvent(
                            PrayerTimesSettingsViewModel.SettingsEvent.IshaAngle(angle.toString())
                        )
                    }
                )
            }

            // Prayer Time Adjustments Section
            SettingsGroup(
                title = {
                    SettingsGroupHeader(
                        title = "Prayer Time",
                        tipText = "Manual adjustment of prayer times"
                    )
                }
            ) {
                PrayerTimeAdjustmentsContent(
                    fajrOffset = fajrOffset.value,
                    sunriseOffset = sunriseOffset.value,
                    dhuhrOffset = dhuhrOffset.value,
                    asrOffset = asrOffset.value,
                    maghribOffset = maghribOffset.value,
                    ishaOffset = ishaOffset.value,
                    onOffsetChange = { prayer, offset ->
                        val event = when (prayer) {
                            "Fajr" -> PrayerTimesSettingsViewModel.SettingsEvent.FajrOffset(offset)
                            "Sunrise" -> PrayerTimesSettingsViewModel.SettingsEvent.SunriseOffset(
                                offset
                            )

                            "Dhuhr" -> PrayerTimesSettingsViewModel.SettingsEvent.DhuhrOffset(offset)
                            "Asr" -> PrayerTimesSettingsViewModel.SettingsEvent.AsrOffset(offset)
                            "Maghrib" -> PrayerTimesSettingsViewModel.SettingsEvent.MaghribOffset(
                                offset
                            )

                            "Isha" -> PrayerTimesSettingsViewModel.SettingsEvent.IshaOffset(offset)
                            else -> null
                        }
                        event?.let {
                            settingViewModel.handleEvent(it)
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun SettingsGroupHeader(
    title: String,
    tipText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title)
        ToolTip(
            icon = painterResource(id = R.drawable.info_icon),
            tipText = tipText,
            contentDescription = "Information"
        )
    }
}


@Composable
private fun PrayerParametersContent(
    autoParams: Boolean,
    madhab: String,
    calculationMethod: String,
    highLatitudeRule: String,
    onAutoParametersChange: (Boolean) -> Unit,
    onCalculationMethodChange: (String) -> Unit,
    mapOfMadhabs: Map<String, String>,
    mapOfHighLatitudeRules: Map<String, String>,
    onMadhabChange: (String) -> Unit,
    onHighLatitudeRuleChange: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        SettingsSwitch(
            state = createBooleanState(autoParams),
            title = {
                if (autoParams) {
                    Text(text = "Auto Calculation")
                } else {
                    Text(text = "Manual Calculation")
                }
            },
            subtitle = {
                Text(text = "Auto angles are Experimental")
            },
            onCheckedChange = {
                onAutoParametersChange(it)
            }
        )
    }
    AnimatedVisibility(
        visible = !autoParams,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        ElevatedCard(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            SettingsList(
                title = "Calculation Method",
                subtitle = calculationMethod,
                description = "The method used to calculate the prayer times.",
                icon = {
                    Image(
                        modifier = Modifier.size(34.dp),
                        painter = painterResource(id = R.drawable.time_calculation),
                        contentDescription = "Calculation Method"
                    )
                },
                items = getMethods(),
                valueState = createValueState(calculationMethod),
                onChange = { method: String ->
                    onCalculationMethodChange(method)
                },
                height = 300.dp
            )
        }
    }

    ElevatedCard(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        SettingsList(
            valueState = createValueState(madhab),
            title = "Madhab",
            description = "The madhab used to calculate the asr prayer times.",
            items = mapOfMadhabs,
            icon = {
                Image(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(id = R.drawable.school),
                    contentDescription = "Madhab"
                )
            },
            subtitle = madhab,
            height = 120.dp,
            onChange = onMadhabChange
        )
    }

    ElevatedCard(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        SettingsList(
            valueState = createValueState(highLatitudeRule),
            title = "High Latitude Rule",
            description = "The high latitude rule used to calculate the prayer times.",
            items = mapOfHighLatitudeRules,
            icon = {
                Image(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(id = R.drawable.high_latitude),
                    contentDescription = "High Latitude Rule"
                )
            },
            subtitle = highLatitudeRule,
            height = 180.dp,
            onChange = onHighLatitudeRuleChange
        )
    }
}

@Composable
private fun PrayerAnglesContent(
    fajrAngle: String,
    ishaAngle: String,
    ishaInterval: String,
    ishaAngleVisible: Boolean,
    onFajrAngleChange: (Int) -> Unit,
    onIshaAngleChange: (Int) -> Unit
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        SettingsNumberPickerDialog(
            title = "Fajr Angle",
            subtitle = { Text(text = fajrAngle) },
            description = "The angle of the sun at which the Fajr prayer begins",
            items = (0..50).map { (it - 25) },
            icon = {
                Image(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(id = R.drawable.fajr_angle),
                    contentDescription = "Fajr Angle"
                )
            },
            valueState = createValueState(fajrAngle),
            height = 150.dp,
            onChange = onFajrAngleChange
        )
    }

    if (ishaAngleVisible) {
        ElevatedCard(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            SettingsNumberPickerDialog(
                title = "Isha Angle",
                description = "The angle of the sun at which the Isha prayer begins",
                items = (0..50).map { (it - 25) },
                subtitle = { Text(text = ishaAngle) },
                icon = {
                    Image(
                        modifier = Modifier.size(48.dp),
                        painter = painterResource(id = R.drawable.isha_angle),
                        contentDescription = "Isha Angle"
                    )
                },
                valueState = createValueState(ishaAngle),
                onChange = onIshaAngleChange,
                height = 150.dp,
            )
        }
    } else {
        ElevatedCard(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            SettingsMenuLink(
                modifier = Modifier.padding(8.dp),
                title = { Text(text = "Isha Interval of $ishaInterval Minutes") },
                subtitle = { Text(text = "The interval of time after Maghrib at which the Isha prayer begins") },
                onClick = { /* Implement interval change logic if needed */ }
            )
        }
    }
}

// Helper component for consistent setting card appearance
@Composable
private fun SettingCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        content()
    }
}

// Helper component for setting icons
@Composable
private fun SettingIcon(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Image(
        modifier = modifier.size(48.dp),
        painter = painterResource(id = iconRes),
        contentDescription = contentDescription
    )
}


@Composable
private fun PrayerTimeAdjustmentsContent(
    fajrOffset: String,
    sunriseOffset: String,
    dhuhrOffset: String,
    asrOffset: String,
    maghribOffset: String,
    ishaOffset: String,
    onOffsetChange: (prayer: String, offset: String) -> Unit
) {
    val prayerTimes = listOf(
        PrayerTimeAdjustment(
            name = "Fajr",
            value = fajrOffset,
            iconRes = R.drawable.fajr_icon,
            description = "Adjust the time of the Fajr prayer"
        ),
        PrayerTimeAdjustment(
            name = "Sunrise",
            value = sunriseOffset,
            iconRes = R.drawable.sunrise_icon,
            description = "Adjust the time of the Sunrise",
            isCircular = true
        ),
        PrayerTimeAdjustment(
            name = "Dhuhr",
            value = dhuhrOffset,
            iconRes = R.drawable.dhuhr_icon,
            description = "Adjust the time of the Dhuhr prayer"
        ),
        PrayerTimeAdjustment(
            name = "Asr",
            value = asrOffset,
            iconRes = R.drawable.asr_icon,
            description = "Adjust the time of the Asr prayer"
        ),
        PrayerTimeAdjustment(
            name = "Maghrib",
            value = maghribOffset,
            iconRes = R.drawable.maghrib_icon,
            description = "Adjust the time of the Maghrib prayer"
        ),
        PrayerTimeAdjustment(
            name = "Isha",
            value = ishaOffset,
            iconRes = R.drawable.isha_icon,
            description = "Adjust the time of the Isha prayer"
        )
    )

    prayerTimes.forEach { prayer ->
        PrayerTimeAdjustmentItem(
            prayerTime = prayer,
            onOffsetChange = { offset -> onOffsetChange(prayer.name, offset) }
        )
    }
}

private data class PrayerTimeAdjustment(
    val name: String,
    val value: String,
    @DrawableRes val iconRes: Int,
    val description: String,
    val isCircular: Boolean = false
)

@Composable
private fun PrayerTimeAdjustmentItem(
    prayerTime: PrayerTimeAdjustment,
    onOffsetChange: (String) -> Unit
) {
    SettingCard {
        SettingsNumberPickerDialog(
            title = "${prayerTime.name} Time",
            description = prayerTime.description,
            items = (0..120).map { (it - 60) },
            icon = {
                Image(
                    modifier = Modifier
                        .size(48.dp)
                        .run {
                            if (prayerTime.isCircular) {
                                clip(CircleShape)
                            } else {
                                this
                            }
                        },
                    painter = painterResource(id = prayerTime.iconRes),
                    contentDescription = "${prayerTime.name} Time"
                )
            },
            subtitle = { Text(text = prayerTime.value) },
            valueState = createValueState(prayerTime.value),
            onChange = { adjustment ->
                onOffsetChange(adjustment.toString())
            },
            height = 150.dp
        )
    }
}

private fun getAdjustmentKey(prayerName: String): String {
    return when (prayerName) {
        "Fajr" -> AppConstants.FAJR_ADJUSTMENT
        "Sunrise" -> AppConstants.SUNRISE_ADJUSTMENT
        "Dhuhr" -> AppConstants.DHUHR_ADJUSTMENT
        "Asr" -> AppConstants.ASR_ADJUSTMENT
        "Maghrib" -> AppConstants.MAGHRIB_ADJUSTMENT
        "Isha" -> AppConstants.ISHA_ADJUSTMENT
        else -> throw IllegalArgumentException("Unknown prayer: $prayerName")
    }
}

// Preview
@Preview(showBackground = true)
@Composable
private fun PrayerTimeAdjustmentsPreview() {
    PrayerTimeAdjustmentsContent(
        fajrOffset = "0",
        sunriseOffset = "0",
        dhuhrOffset = "0",
        asrOffset = "0",
        maghribOffset = "0",
        ishaOffset = "0",
        onOffsetChange = { _, _ -> }
    )
}

// Add these at the bottom of your file
private fun createValueState(value: String) = object : SettingValueState<String> {
    override var value: String = value
    override fun reset() {}
}

private fun createBooleanState(value: Boolean) = object : SettingValueState<Boolean> {
    override var value: Boolean = value
    override fun reset() {}
}

private fun createDoubleState(value: Double) = object : SettingValueState<Double> {
    override var value: Double = value
    override fun reset() {}
}