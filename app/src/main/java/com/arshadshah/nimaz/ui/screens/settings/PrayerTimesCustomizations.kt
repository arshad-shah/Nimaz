package com.arshadshah.nimaz.ui.screens.settings

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.FAJR_ANGLE
import com.arshadshah.nimaz.ui.components.common.CalculationMethodUI
import com.arshadshah.nimaz.ui.components.common.ToolTip
import com.arshadshah.nimaz.ui.components.settings.SettingsGroup
import com.arshadshah.nimaz.ui.components.settings.SettingsList
import com.arshadshah.nimaz.ui.components.settings.SettingsMenuLink
import com.arshadshah.nimaz.ui.components.settings.SettingsNumberPickerDialog
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrayerTimesParamMapper.getParams
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.viewModel.SettingsViewModel

@Composable
fun PrayerTimesCustomizations(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val sharedPreferences = PrivateSharedPreferences(context)

    val viewModel = viewModel(
        key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY,
        initializer = { PrayerTimesViewModel(context) },
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val settingViewModel = viewModel(
        key = AppConstants.SETTINGS_VIEWMODEL_KEY,
        initializer = { SettingsViewModel(context) },
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )


    LaunchedEffect(Unit) {
        settingViewModel.handleEvent(SettingsViewModel.SettingsEvent.LoadSettings)
    }
    val mapOfMadhabs = AppConstants.getAsrJuristic()
    val mapOfHighLatitudeRules = AppConstants.getHighLatitudes()

    val madhabState =
        rememberPreferenceStringSettingState(AppConstants.MADHAB, "SHAFI", sharedPreferences)
    val highLatitudeRuleState = rememberPreferenceStringSettingState(
        AppConstants.HIGH_LATITUDE_RULE,
        "MIDDLE_OF_THE_NIGHT",
        sharedPreferences
    )
    val fajrAngleState =
        rememberPreferenceStringSettingState(FAJR_ANGLE, "18", sharedPreferences)
    val ishaAngleState =
        rememberPreferenceStringSettingState(AppConstants.ISHA_ANGLE, "18", sharedPreferences)

    //isha interval
    val ishaIntervalState =
        rememberPreferenceStringSettingState(AppConstants.ISHA_INTERVAL, "0", sharedPreferences)

    val fajrAdjustment =
        rememberPreferenceStringSettingState(AppConstants.FAJR_ADJUSTMENT, "0", sharedPreferences)
    val sunriseAdjustment = rememberPreferenceStringSettingState(
        AppConstants.SUNRISE_ADJUSTMENT,
        "0",
        sharedPreferences
    )
    val dhuhrAdjustment = rememberPreferenceStringSettingState(
        AppConstants.DHUHR_ADJUSTMENT,
        "0",
        sharedPreferences
    )
    val asrAdjustment =
        rememberPreferenceStringSettingState(AppConstants.ASR_ADJUSTMENT, "0", sharedPreferences)
    val maghribAdjustment = rememberPreferenceStringSettingState(
        AppConstants.MAGHRIB_ADJUSTMENT,
        "0",
        sharedPreferences
    )
    val ishaAdjustment =
        rememberPreferenceStringSettingState(AppConstants.ISHA_ADJUSTMENT, "0", sharedPreferences)


    val ishaaAngleVisible = remember {
        settingViewModel.ishaAngleVisibility
    }.collectAsState()

    val ishaInterval = remember {
        settingViewModel.ishaInterval
    }.collectAsState()

    val madhab = remember {
        settingViewModel.madhab
    }.collectAsState()

    val highLatitudeRule = remember {
        settingViewModel.highLatitude
    }.collectAsState()

    val fajrAngle = remember {
        settingViewModel.fajrAngle
    }.collectAsState()

    val ishaAngle = remember {
        settingViewModel.ishaAngle
    }.collectAsState()

    val fajrAdjustmentValue = remember {
        settingViewModel.fajrOffset
    }.collectAsState()

    val sunriseAdjustmentValue = remember {
        settingViewModel.sunriseOffset
    }.collectAsState()

    val dhuhrAdjustmentValue = remember {
        settingViewModel.dhuhrOffset
    }.collectAsState()

    val asrAdjustmentValue = remember {
        settingViewModel.asrOffset
    }.collectAsState()

    val maghribAdjustmentValue = remember {
        settingViewModel.maghribOffset
    }.collectAsState()

    val ishaAdjustmentValue = remember {
        settingViewModel.ishaOffset
    }.collectAsState()

    madhabState.value = madhab.value
    highLatitudeRuleState.value = highLatitudeRule.value
    fajrAngleState.value = fajrAngle.value
    ishaAngleState.value = ishaAngle.value
    ishaIntervalState.value = ishaInterval.value
    fajrAdjustment.value = fajrAdjustmentValue.value
    sunriseAdjustment.value = sunriseAdjustmentValue.value
    dhuhrAdjustment.value = dhuhrAdjustmentValue.value
    asrAdjustment.value = asrAdjustmentValue.value
    maghribAdjustment.value = maghribAdjustmentValue.value
    ishaAdjustment.value = ishaAdjustmentValue.value

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState(), true)
            .padding(paddingValues)
            .testTag(AppConstants.TEST_TAG_PRAYER_TIMES_CUSTOMIZATION)
    ) {

        SettingsGroup(title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Prayer Parameters")
                ToolTip(
                    icon = painterResource(id = R.drawable.info_icon),
                    tipText = "Prayer times Calculation Parameters",
                )
            }
        }) {
            CalculationMethodUI()
            ElevatedCard(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
            ) {
                SettingsList(
                    valueState = madhabState,
                    title = "Madhab",
                    description = "The madhab used to calculate the asr prayer times.",
                    items = mapOfMadhabs,
                    subtitle = madhabState.value,
                    height = 120.dp
                ) { madhab: String ->
                    settingViewModel.handleEvent(
                        SettingsViewModel.SettingsEvent.Madhab(
                            madhab
                        )
                    )
                    viewModel.handleEvent(
                        context,
                        PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
                            getParams(context)
                        )
                    )
                    viewModel.handleEvent(
                        context,
                        PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
                            context
                        )
                    )
                    PrivateSharedPreferences(context).saveDataBoolean(
                        AppConstants.ALARM_LOCK,
                        false
                    )
                }
            }
            ElevatedCard(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
            ) {
                SettingsList(
                    valueState = highLatitudeRuleState,
                    title = "High Latitude Rule",
                    description = "The high latitude rule used to calculate the prayer times.",
                    items = mapOfHighLatitudeRules,
                    subtitle = highLatitudeRuleState.value,
                    height = 180.dp
                ) { highLatRule: String ->
                    settingViewModel.handleEvent(
                        SettingsViewModel.SettingsEvent.HighLatitude(
                            highLatRule
                        )
                    )
                    viewModel.handleEvent(
                        context,
                        PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
                            getParams(context)
                        )
                    )
                    viewModel.handleEvent(
                        context,
                        PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
                            context
                        )
                    )
                    PrivateSharedPreferences(context).saveDataBoolean(
                        AppConstants.ALARM_LOCK,
                        false
                    )
                }
            }
        }

        SettingsGroup(title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Prayer Angles")
                ToolTip(
                    icon = painterResource(id = R.drawable.info_icon),
                    tipText = "Angles of the sun used to calculate Fair and Isha prayer times",
                )
            }
        }) {

            ElevatedCard(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
            ) {
                SettingsNumberPickerDialog(
                    title = "Fajr Angle",
                    subtitle = {
                        Text(text = fajrAngleState.value)
                    },
                    description = "The angle of the sun at which the Fajr prayer begins",
                    items = (0..50).map { (it - 25) },
                    valueState = fajrAngleState,
                    height = 150.dp,
                    onChange = { angle: Int ->
                        settingViewModel.handleEvent(
                            SettingsViewModel.SettingsEvent.FajrAngle(
                                angle.toString()
                            )
                        )
                        viewModel.handleEvent(
                            context,
                            PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
                                getParams(context)
                            )
                        )
                        viewModel.handleEvent(
                            context,
                            PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
                                context
                            )
                        )
                        PrivateSharedPreferences(context).saveDataBoolean(
                            AppConstants.ALARM_LOCK,
                            false
                        )
                    }
                )
            }
            if (ishaaAngleVisible.value) {
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsNumberPickerDialog(
                        title = "Isha Angle",
                        description = "The angle of the sun at which the Isha prayer begins",
                        items = (0..50).map { (it - 25) },
                        subtitle = {
                            Text(text = ishaAngleState.value)
                        },
                        valueState = ishaAngleState,
                        onChange = { angle: Int ->
                            settingViewModel.handleEvent(
                                SettingsViewModel.SettingsEvent.IshaAngle(
                                    angle.toString()
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
                                    getParams(context)
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
                                    context
                                )
                            )
                            PrivateSharedPreferences(context).saveDataBoolean(
                                AppConstants.ALARM_LOCK,
                                false
                            )
                        },
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
                        title = { Text(text = "Isha Interval of ${ishaIntervalState.value} Minutes") },
                        subtitle = { Text(text = "The interval of time after Maghrib at which the Isha prayer begins") },
                        onClick = { },
                    )
                }
            }
        }



        SettingsGroup(
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Prayer Time")
                    Spacer(modifier = Modifier.width(8.dp))
                    ToolTip(
                        icon = painterResource(id = R.drawable.info_icon),
                        tipText = "Manual adjustment of prayer times",
                    )
                }
            },
            content = {
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsNumberPickerDialog(
                        title = "Fajr Time",
                        description = "Adjust the time of the Fajr prayer",
                        items = (0..120).map { (it - 60) },
                        icon = {
                            Image(
                                modifier = Modifier
                                    .size(48.dp),
                                painter = painterResource(id = R.drawable.fajr_icon),
                                contentDescription = "Fajr Time"
                            )
                        },
                        subtitle = {
                            Text(text = fajrAdjustment.value)
                        },
                        valueState = fajrAdjustment,
                        onChange = { adjustment: Int ->
                            settingViewModel.handleEvent(
                                SettingsViewModel.SettingsEvent.FajrOffset(
                                    adjustment.toString()
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
                                    getParams(context)
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
                                    context
                                )
                            )
                            PrivateSharedPreferences(context).saveDataBoolean(
                                AppConstants.ALARM_LOCK,
                                false
                            )
                        },
                        height = 150.dp,
                    )
                }
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsNumberPickerDialog(
                        title = "Sunrise Time",
                        description = "Adjust the time of the Sunrise prayer",
                        items = (0..120).map { (it - 60) },
                        icon = {
                            Image(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                                painter = painterResource(id = R.drawable.sunrise_icon),
                                contentDescription = "Sunrise Time"
                            )
                        },
                        subtitle = {
                            Text(text = sunriseAdjustment.value)
                        },
                        valueState = sunriseAdjustment,
                        onChange = { adjustment: Int ->
                            settingViewModel.handleEvent(
                                SettingsViewModel.SettingsEvent.SunriseOffset(
                                    adjustment.toString()
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
                                    getParams(context)
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
                                    context
                                )
                            )
                            PrivateSharedPreferences(context).saveDataBoolean(
                                AppConstants.ALARM_LOCK,
                                false
                            )
                        },
                        height = 150.dp,
                    )
                }
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsNumberPickerDialog(
                        title = "Dhuhr Time",
                        description = "Adjust the time of the Dhuhr prayer",
                        items = (0..120).map { (it - 60) },
                        icon = {
                            Image(
                                modifier = Modifier
                                    .size(48.dp),
                                painter = painterResource(id = R.drawable.dhuhr_icon),
                                contentDescription = "Dhuhr Time"
                            )
                        },
                        subtitle = {
                            Text(text = dhuhrAdjustment.value)
                        },
                        valueState = dhuhrAdjustment,
                        onChange = { adjustment: Int ->
                            settingViewModel.handleEvent(
                                SettingsViewModel.SettingsEvent.DhuhrOffset(
                                    adjustment.toString()
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
                                    getParams(context)
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
                                    context
                                )
                            )
                            PrivateSharedPreferences(context).saveDataBoolean(
                                AppConstants.ALARM_LOCK,
                                false
                            )
                        },
                        height = 150.dp,
                    )
                }
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsNumberPickerDialog(
                        title = "Asr Time",
                        description = "Adjust the time of the Asr prayer",
                        items = (0..120).map { (it - 60) },
                        icon = {
                            Image(
                                modifier = Modifier
                                    .size(48.dp),
                                painter = painterResource(id = R.drawable.asr_icon),
                                contentDescription = "Asr Time"
                            )
                        },
                        subtitle = {
                            Text(text = asrAdjustment.value)
                        },
                        valueState = asrAdjustment,
                        onChange = { adjustment: Int ->
                            settingViewModel.handleEvent(
                                SettingsViewModel.SettingsEvent.AsrOffset(
                                    adjustment.toString()
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
                                    getParams(context)
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
                                    context
                                )
                            )
                            PrivateSharedPreferences(context).saveDataBoolean(
                                AppConstants.ALARM_LOCK,
                                false
                            )
                        },
                        height = 150.dp,
                    )
                }
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsNumberPickerDialog(
                        title = "Maghrib Time",
                        description = "Adjust the time of the Maghrib prayer",
                        items = (0..120).map { (it - 60) },
                        icon = {
                            Image(
                                modifier = Modifier
                                    .size(48.dp),
                                painter = painterResource(id = R.drawable.maghrib_icon),
                                contentDescription = "Maghrib Time"
                            )
                        },
                        subtitle = {
                            Text(text = maghribAdjustment.value)
                        },
                        valueState = maghribAdjustment,
                        onChange = { adjustment: Int ->
                            settingViewModel.handleEvent(
                                SettingsViewModel.SettingsEvent.MaghribOffset(
                                    adjustment.toString()
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
                                    getParams(context)
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
                                    context
                                )
                            )
                            PrivateSharedPreferences(context).saveDataBoolean(
                                AppConstants.ALARM_LOCK,
                                false
                            )
                        },
                        height = 150.dp,
                    )
                }
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsNumberPickerDialog(
                        title = "Isha Time",
                        description = "Adjust the time of the Isha prayer",
                        items = (0..120).map { (it - 60) },
                        icon = {
                            Image(
                                modifier = Modifier
                                    .size(48.dp),
                                painter = painterResource(id = R.drawable.isha_icon),
                                contentDescription = "Isha Time"
                            )
                        },
                        subtitle = {
                            Text(text = ishaAdjustment.value)
                        },
                        valueState = ishaAdjustment,
                        onChange = { adjustment: Int ->
                            Log.d("SettingsScreen", "ishaAdjustment: $adjustment")
                            settingViewModel.handleEvent(
                                SettingsViewModel.SettingsEvent.IshaOffset(
                                    adjustment.toString()
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
                                    getParams(context)
                                )
                            )
                            viewModel.handleEvent(
                                context,
                                PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
                                    context
                                )
                            )
                            PrivateSharedPreferences(context).saveDataBoolean(
                                AppConstants.ALARM_LOCK,
                                false
                            )
                        },
                        height = 150.dp,
                    )
                }
            }
        )
    }
}


@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NimazTheme {
        PrayerTimesCustomizations(paddingValues = PaddingValues(16.dp))
    }
}