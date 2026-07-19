package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazListPicker
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazPickerItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.AsrJuristicMethod
import com.arshadshah.nimaz.presentation.viewmodel.HighLatitudeRule
import com.arshadshah.nimaz.presentation.viewmodel.NotificationSummary
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
    // Reactive summary sourced from DataStore, so these subtitles reflect edits made on the
    // Notification Settings screen (a separate ViewModel instance) the moment we return here.
    val notificationSummary by viewModel.notificationSummary.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Dialog states for selection screens
    var showCalculationMethodDialog by remember { mutableStateOf(false) }
    var showAsrMethodDialog by remember { mutableStateOf(false) }
    var showHighLatitudeDialog by remember { mutableStateOf(false) }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.prayer_settings_title),
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
                NimazSectionHeader(title = stringResource(R.string.calculation_method))
            }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        icon = Icons.Default.Schedule,
                        tintIcon = true,
                        title = stringResource(R.string.calculation_method),
                        value = prayerState.calculationMethod.displayName(),
                        onClick = { showCalculationMethodDialog = true }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        icon = Icons.Default.WbSunny,
                        title = stringResource(R.string.asr_calculation),
                        value = when (prayerState.asrMethod) {
                            AsrJuristicMethod.STANDARD -> stringResource(R.string.asr_standard)
                            AsrJuristicMethod.HANAFI -> stringResource(R.string.asr_hanafi)
                        },
                        onClick = { showAsrMethodDialog = true }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        icon = Icons.Default.WbSunny,
                        title = stringResource(R.string.high_latitude_method),
                        value = when (prayerState.highLatitudeRule) {
                            HighLatitudeRule.MIDDLE_OF_NIGHT -> stringResource(R.string.middle_of_night)
                            HighLatitudeRule.SEVENTH_OF_NIGHT -> stringResource(R.string.seventh_of_night)
                            HighLatitudeRule.TWILIGHT_ANGLE -> stringResource(R.string.twilight_angle)
                        },
                        onClick = { showHighLatitudeDialog = true }
                    )
                }
            }

            // Info Banner
            item {
                NimazBanner(
                    message = stringResource(
                        R.string.prayer_settings_high_latitude_notice_format,
                        locationState.currentLocation?.city
                            ?: stringResource(R.string.prayer_settings_your_location)
                    ),
                    variant = NimazBannerVariant.INFO,
                    icon = Icons.Default.Info
                )
            }

            // Manual Adjustments Section
            item {
                NimazSectionHeader(title = stringResource(R.string.manual_adjustments))
            }
            item {
                NimazMenuGroup {
                    NimazNumberStepper(
                        label = stringResource(R.string.prayer_fajr),
                        value = prayerState.fajrAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("fajr", it))
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazNumberStepper(
                        label = stringResource(R.string.prayer_sunrise),
                        value = prayerState.sunriseAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("sunrise", it))
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazNumberStepper(
                        label = stringResource(R.string.prayer_dhuhr),
                        value = prayerState.dhuhrAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("dhuhr", it))
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazNumberStepper(
                        label = stringResource(R.string.prayer_asr),
                        value = prayerState.asrAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("asr", it))
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazNumberStepper(
                        label = stringResource(R.string.prayer_maghrib),
                        value = prayerState.maghribAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("maghrib", it))
                        }
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazNumberStepper(
                        label = stringResource(R.string.prayer_isha),
                        value = prayerState.ishaAdjustment,
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetPrayerAdjustment("isha", it))
                        }
                    )
                }
            }

            // Notifications Section
            item {
                NimazSectionHeader(title = stringResource(R.string.notifications))
            }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.adhan_notifications),
                        value = when {
                            !notificationSummary.notificationsMasterEnabled ->
                                stringResource(R.string.prayer_settings_notifications_off)

                            notificationSummary.enabledPrayerCount == NotificationSummary.TOTAL_PRAYER_COUNT ->
                                stringResource(R.string.prayer_settings_all_prayers_enabled)

                            notificationSummary.enabledPrayerCount == 0 ->
                                stringResource(R.string.prayer_settings_no_prayers_enabled)

                            else -> stringResource(
                                R.string.prayer_settings_prayers_enabled_count,
                                notificationSummary.enabledPrayerCount,
                                NotificationSummary.TOTAL_PRAYER_COUNT
                            )
                        },
                        onClick = onNavigateToNotifications
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        icon = Icons.Default.Schedule,
                        title = stringResource(R.string.pre_adhan_reminder),
                        value = if (notificationSummary.reminderEnabled) {
                            pluralStringResource(
                                R.plurals.notification_settings_pre_adhan_subtitle,
                                notificationSummary.reminderMinutes,
                                notificationSummary.reminderMinutes
                            )
                        } else {
                            stringResource(R.string.prayer_settings_reminder_off)
                        },
                        onClick = onNavigateToNotifications
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Selection dialogs use the generic NimazListPicker — type-safe in T, with
    // auto-dismiss on selection (no extra Done tap) and built-in search for
    // long lists.
    if (showCalculationMethodDialog) {
        NimazListPicker(
            title = stringResource(R.string.calculation_method),
            items = CalculationMethod.entries.map { method ->
                NimazPickerItem(
                    value = method,
                    title = method.displayName(),
                    description = calculationMethodRegion(method),
                )
            },
            selected = prayerState.calculationMethod,
            onSelected = { viewModel.onEvent(SettingsEvent.SetCalculationMethod(it)) },
            onDismiss = { showCalculationMethodDialog = false },
        )
    }

    if (showAsrMethodDialog) {
        NimazListPicker(
            title = stringResource(R.string.asr_calculation),
            items = listOf(
                NimazPickerItem(
                    value = AsrJuristicMethod.STANDARD,
                    title = stringResource(R.string.asr_standard),
                    description = stringResource(R.string.asr_standard_desc),
                ),
                NimazPickerItem(
                    value = AsrJuristicMethod.HANAFI,
                    title = stringResource(R.string.asr_hanafi),
                    description = stringResource(R.string.asr_hanafi_desc),
                ),
            ),
            selected = prayerState.asrMethod,
            onSelected = { viewModel.onEvent(SettingsEvent.SetAsrMethod(it)) },
            onDismiss = { showAsrMethodDialog = false },
        )
    }

    if (showHighLatitudeDialog) {
        NimazListPicker(
            title = stringResource(R.string.high_latitude_method),
            items = listOf(
                NimazPickerItem(
                    value = HighLatitudeRule.MIDDLE_OF_NIGHT,
                    title = stringResource(R.string.middle_of_night),
                    description = stringResource(R.string.high_lat_middle_desc),
                ),
                NimazPickerItem(
                    value = HighLatitudeRule.SEVENTH_OF_NIGHT,
                    title = stringResource(R.string.seventh_of_night),
                    description = stringResource(R.string.high_lat_seventh_desc),
                ),
                NimazPickerItem(
                    value = HighLatitudeRule.TWILIGHT_ANGLE,
                    title = stringResource(R.string.twilight_angle),
                    description = stringResource(R.string.high_lat_twilight_desc),
                ),
            ),
            selected = prayerState.highLatitudeRule,
            onSelected = { viewModel.onEvent(SettingsEvent.SetHighLatitudeRule(it)) },
            onDismiss = { showHighLatitudeDialog = false },
        )
    }
}

/**
 * Region description for each calculation method, used as the picker item's
 * subtitle so users can pick by where they live rather than by an unfamiliar
 * acronym ("Used in Pakistan" beats "Karachi" if you don't already know it).
 */
@Composable
private fun calculationMethodRegion(method: CalculationMethod): String = when (method) {
    CalculationMethod.MUSLIM_WORLD_LEAGUE -> stringResource(R.string.calc_region_mwl)
    CalculationMethod.EGYPTIAN -> stringResource(R.string.calc_region_egyptian)
    CalculationMethod.KARACHI -> stringResource(R.string.calc_region_karachi)
    CalculationMethod.UMM_AL_QURA -> stringResource(R.string.calc_region_umm_al_qura)
    CalculationMethod.DUBAI -> stringResource(R.string.calc_region_dubai)
    CalculationMethod.MOON_SIGHTING_COMMITTEE -> stringResource(R.string.calc_region_moon_sighting)
    CalculationMethod.NORTH_AMERICA -> stringResource(R.string.calc_region_north_america)
    CalculationMethod.KUWAIT -> stringResource(R.string.calc_region_kuwait)
    CalculationMethod.QATAR -> stringResource(R.string.calc_region_qatar)
    CalculationMethod.SINGAPORE -> stringResource(R.string.calc_region_singapore)
    CalculationMethod.TURKEY -> stringResource(R.string.calc_region_turkey)
}
