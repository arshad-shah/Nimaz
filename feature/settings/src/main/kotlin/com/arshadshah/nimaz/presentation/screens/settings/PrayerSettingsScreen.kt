package com.arshadshah.nimaz.presentation.screens.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.common.formatClockTime
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NimazBanner
import com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuDivider
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazPrayerDayArc
import com.arshadshah.nimaz.presentation.components.molecules.NimazPrayerDayPoint
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazListPicker
import com.arshadshah.nimaz.presentation.components.organisms.NimazPickerItem
import com.arshadshah.nimaz.presentation.theme.LocalUse24HourFormat
import com.arshadshah.nimaz.presentation.viewmodel.settings.NotificationSummary
import com.arshadshah.nimaz.presentation.viewmodel.settings.PrayerPreviewUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.PrayerSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsViewModel
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt
import com.arshadshah.nimaz.feature.settings.R as SettingsR

/**
 * Latitude from which the summer sky can stay too light for Fajr and Isha, so the high-latitude
 * rule starts to matter. Below it the notice would be a warning about nothing.
 */
private const val HIGH_LATITUDE_DEGREES = 48.0

/** The picker illustrations are 3:2; a wider banner keeps the sheet's options in view. */
private const val ILLUSTRATION_ASPECT = 16f / 8.5f

/**
 * Prayer-time calculation: how the times are worked out, how far to shift them, and where their
 * reminders live — under a live preview of today's times, so every change shows what it does.
 *
 * The preview is [SettingsViewModel.prayerPreview], built from the same use cases reminders read,
 * so it cannot disagree with them. A time the latest change moved is highlighted on the arc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prayerState by viewModel.prayerState.collectAsStateWithLifecycle()
    val preview by viewModel.prayerPreview.collectAsStateWithLifecycle()
    // Reactive summary sourced from DataStore, so the reminders row reflects edits made on the
    // notifications hub (a separate ViewModel instance) the moment we return here.
    val notificationSummary by viewModel.notificationSummary.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showMethodPicker by rememberSaveable { mutableStateOf(false) }
    var showAsrPicker by rememberSaveable { mutableStateOf(false) }
    var showHighLatitudePicker by rememberSaveable { mutableStateOf(false) }
    var showAdjustments by rememberSaveable { mutableStateOf(false) }

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

            item { TodayPreviewCard(preview, prayerState.calculationMethod) }

            item { NimazSectionHeader(title = stringResource(R.string.prayer_settings_section_calculation)) }
            item {
                // Each row has its own icon — a globe for who calculates, a low sun for the
                // afternoon shadow, a moon for the short summer night.
                NimazMenuGroup {
                    NimazSettingsItem(
                        icon = Icons.Default.Public,
                        tintIcon = true,
                        title = stringResource(R.string.calculation_method),
                        value = prayerState.calculationMethod.displayName(),
                        onClick = { showMethodPicker = true }
                    )
                    NimazMenuDivider(inset = false)
                    NimazSettingsItem(
                        icon = Icons.Default.WbTwilight,
                        tintIcon = true,
                        title = stringResource(R.string.asr_calculation),
                        value = asrLabel(prayerState.asrMethod),
                        onClick = { showAsrPicker = true }
                    )
                    NimazMenuDivider(inset = false)
                    NimazSettingsItem(
                        icon = Icons.Outlined.Nightlight,
                        tintIcon = true,
                        title = stringResource(R.string.high_latitude_method),
                        value = highLatitudeLabel(prayerState.highLatitudeRule),
                        onClick = { showHighLatitudePicker = true }
                    )
                }
            }

            // Only where the rule actually matters, and only for a place the reader really has —
            // naming a fallback city's latitude would be a claim about somewhere they are not.
            val latitude = preview.latitude
            val city = preview.locationName
            if (latitude != null && city != null && abs(latitude) >= HIGH_LATITUDE_DEGREES) {
                item {
                    NimazBanner(
                        title = stringResource(
                            R.string.prayer_settings_high_lat_notice,
                            city,
                            abs(latitude).roundToInt(),
                            stringResource(if (latitude >= 0) R.string.prayer_latitude_north else R.string.prayer_latitude_south),
                            highLatitudeLabel(prayerState.highLatitudeRule),
                        ),
                        variant = NimazBannerVariant.INFO,
                    )
                }
            }

            item { NimazSectionHeader(title = stringResource(R.string.prayer_settings_section_finetune)) }
            item {
                val adjusted = prayerState.adjustments().count { it.second != 0 }
                NimazMenuGroup {
                    NimazSettingsItem(
                        icon = Icons.Default.Tune,
                        tintIcon = true,
                        title = stringResource(R.string.prayer_settings_adjust_title),
                        value = if (adjusted == 0) {
                            stringResource(R.string.prayer_settings_adjust_hint)
                        } else {
                            pluralStringResource(R.plurals.prayer_settings_adjusted_count, adjusted, adjusted)
                        },
                        onClick = { showAdjustments = true }
                    )
                }
            }

            item { NimazSectionHeader(title = stringResource(R.string.prayer_settings_section_reminders)) }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        icon = Icons.Default.NotificationsActive,
                        tintIcon = true,
                        title = stringResource(R.string.prayer_settings_reminders_title),
                        value = remindersSummary(notificationSummary),
                        onClick = onNavigateToNotifications
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Pickers reuse NimazListPicker — type-safe in T, searchable for long lists — with an
    // illustration above the options and, where it helps, today's resulting time beside each.
    if (showMethodPicker) {
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
            onDismiss = { showMethodPicker = false },
            header = {
                PickerIntro(SettingsR.drawable.prayer_settings_twilight_angle, R.string.prayer_method_picker_body)
            },
            trailingContent = { item ->
                preview.methodFajr[item.value]?.let { fajr ->
                    OptionTime(stringResource(R.string.prayer_picker_fajr_at, clock(fajr)))
                }
            },
        )
    }

    if (showAsrPicker) {
        // Stays open so the illustration can change with the choice: every tap applies, Done
        // keeps it, Cancel puts back the rule the sheet opened with.
        val openedWith = remember { prayerState.asrMethod }
        NimazListPicker(
            title = stringResource(R.string.asr_calculation),
            items = listOf(
                NimazPickerItem(
                    value = AsrCalculation.STANDARD,
                    title = stringResource(R.string.asr_standard),
                    description = stringResource(R.string.asr_standard_desc),
                ),
                NimazPickerItem(
                    value = AsrCalculation.HANAFI,
                    title = stringResource(R.string.asr_hanafi),
                    description = stringResource(R.string.asr_hanafi_desc),
                ),
            ),
            selected = prayerState.asrMethod,
            onSelected = { viewModel.onEvent(SettingsEvent.SetAsrMethod(it)) },
            onDismiss = { showAsrPicker = false },
            onCancel = {
                if (prayerState.asrMethod != openedWith) viewModel.onEvent(SettingsEvent.SetAsrMethod(openedWith))
                showAsrPicker = false
            },
            autoDismiss = false,
            header = {
                Column {
                    Crossfade(targetState = prayerState.asrMethod, animationSpec = tween(600), label = "asr-illustration") { rule ->
                        Illustration(
                            if (rule == AsrCalculation.HANAFI) SettingsR.drawable.prayer_settings_asr_hanafi
                            else SettingsR.drawable.prayer_settings_asr_standard,
                        )
                    }
                    IntroText(R.string.prayer_asr_picker_body)
                }
            },
            trailingContent = { item ->
                preview.asrTimes[item.value]?.let { OptionTime(clock(it)) }
            },
        )
    }

    if (showHighLatitudePicker) {
        NimazListPicker(
            title = stringResource(R.string.high_latitude_method),
            items = listOf(
                NimazPickerItem(
                    value = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
                    title = stringResource(R.string.middle_of_night),
                    description = stringResource(R.string.high_lat_middle_desc),
                ),
                NimazPickerItem(
                    value = HighLatitudeRule.SEVENTH_OF_THE_NIGHT,
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
            onDismiss = { showHighLatitudePicker = false },
            header = {
                PickerIntro(SettingsR.drawable.prayer_settings_high_latitude, R.string.prayer_high_lat_picker_body)
            },
        )
    }

    if (showAdjustments) {
        NimazBottomSheet(
            onDismissRequest = { showAdjustments = false },
            title = stringResource(R.string.prayer_settings_adjust_title),
            subtitle = stringResource(R.string.prayer_settings_adjust_body),
            onClose = { showAdjustments = false },
        ) {
            // A sheet of its own, so scrolling the settings page can no longer land on a stepper
            // and move a prayer time without the reader noticing.
            NimazMenuGroup {
                prayerState.adjustments().forEachIndexed { index, (prayer, minutes) ->
                    if (index > 0) NimazMenuDivider(inset = false)
                    AdjustmentRow(
                        prayer = prayer,
                        minutes = minutes,
                        result = preview.times[prayer.type],
                        changed = prayer.type in preview.changed,
                        onChange = { viewModel.onEvent(SettingsEvent.SetPrayerAdjustment(prayer.key, it)) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** A manual offset's prayer, the key [SettingsEvent.SetPrayerAdjustment] writes, and its label. */
private enum class AdjustedPrayer(val key: String, val type: PrayerType, val label: Int) {
    FAJR("fajr", PrayerType.FAJR, R.string.prayer_fajr),
    SUNRISE("sunrise", PrayerType.SUNRISE, R.string.prayer_sunrise),
    DHUHR("dhuhr", PrayerType.DHUHR, R.string.prayer_dhuhr),
    ASR("asr", PrayerType.ASR, R.string.prayer_asr),
    MAGHRIB("maghrib", PrayerType.MAGHRIB, R.string.prayer_maghrib),
    ISHA("isha", PrayerType.ISHA, R.string.prayer_isha),
}

private fun PrayerSettingsUiState.adjustments(): List<Pair<AdjustedPrayer, Int>> = listOf(
    AdjustedPrayer.FAJR to fajrAdjustment,
    AdjustedPrayer.SUNRISE to sunriseAdjustment,
    AdjustedPrayer.DHUHR to dhuhrAdjustment,
    AdjustedPrayer.ASR to asrAdjustment,
    AdjustedPrayer.MAGHRIB to maghribAdjustment,
    AdjustedPrayer.ISHA to ishaAdjustment,
)

/** Today's six times on the sun's path, recomputed as the reader changes a setting. */
@Composable
private fun TodayPreviewCard(preview: PrayerPreviewUiState, method: CalculationMethod) {
    val times = preview.times
    val sunrise = times[PrayerType.SUNRISE]
    val maghrib = times[PrayerType.MAGHRIB]
    NimazCard(
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(20.dp),
        tone = NimazTone.NEUTRAL,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = preview.locationName?.let { stringResource(R.string.prayer_preview_today, it) }
                        ?: stringResource(R.string.prayer_preview_today_plain),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = method.displayName(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
            if (sunrise != null && maghrib != null) {
                val points = PrayerType.entries.mapNotNull { type ->
                    val at = times[type] ?: return@mapNotNull null
                    NimazPrayerDayPoint(
                        position = at.dayFraction(),
                        name = prayerLabel(type),
                        time = clock(at),
                        isHorizon = type == PrayerType.SUNRISE || type == PrayerType.MAGHRIB,
                        tone = when (type) {
                            PrayerType.DHUHR -> NimazTone.PROMINENT
                            PrayerType.ASR, PrayerType.MAGHRIB -> NimazTone.WARNING
                            PrayerType.SUNRISE -> NimazTone.ACCENT
                            else -> NimazTone.MUTED
                        },
                        highlighted = type in preview.changed,
                    )
                }
                NimazPrayerDayArc(
                    points = points,
                    sunriseFraction = sunrise.dayFraction(),
                    sunsetFraction = maghrib.dayFraction(),
                    contentDescription = stringResource(
                        R.string.prayer_preview_cd,
                        points.joinToString { "${it.name} ${it.time}" },
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                text = stringResource(
                    if (preview.changed.isEmpty()) R.string.prayer_preview_note_default
                    else R.string.prayer_preview_note_changed,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (preview.changed.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun AdjustmentRow(
    prayer: AdjustedPrayer,
    minutes: Int,
    result: LocalDateTime?,
    changed: Boolean,
    onChange: (Int) -> Unit,
) {
    NimazNumberStepper(
        value = minutes,
        onValueChange = onChange,
        labelContent = {
            Column {
                Text(stringResource(prayer.label), style = MaterialTheme.typography.titleSmall)
                if (result != null) {
                    Text(
                        text = clock(result),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (changed) FontWeight.SemiBold else null,
                        color = if (changed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

/** An illustration above a picker's options, then a sentence that says what the choice is. */
@Composable
private fun PickerIntro(@DrawableRes art: Int, body: Int) {
    Column {
        Illustration(art)
        IntroText(body)
    }
}

@Composable
private fun Illustration(@DrawableRes art: Int) {
    Image(
        painter = painterResource(art),
        // Decorative: the sentence under it says what it shows.
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = BiasAlignment(0f, 0.25f),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ILLUSTRATION_ASPECT)
            .clip(RoundedCornerShape(16.dp)),
    )
}

@Composable
private fun IntroText(body: Int) {
    Text(
        text = stringResource(body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun OptionTime(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp),
    )
}

/** A wall-clock time in the reader's 12/24-hour preference, like every other time in the app. */
@Composable
private fun clock(at: LocalDateTime): String =
    formatClockTime(at.hour, at.minute, LocalUse24HourFormat.current)

private fun LocalDateTime.dayFraction(): Float = (hour * 60 + minute) / 1440f

@Composable
private fun prayerLabel(type: PrayerType): String = stringResource(
    when (type) {
        PrayerType.FAJR -> R.string.prayer_fajr
        PrayerType.SUNRISE -> R.string.prayer_sunrise
        PrayerType.DHUHR -> R.string.prayer_dhuhr
        PrayerType.ASR -> R.string.prayer_asr
        PrayerType.MAGHRIB -> R.string.prayer_maghrib
        PrayerType.ISHA -> R.string.prayer_isha
    },
)

@Composable
private fun asrLabel(rule: AsrCalculation): String = stringResource(
    when (rule) {
        AsrCalculation.STANDARD -> R.string.asr_standard
        AsrCalculation.HANAFI -> R.string.asr_hanafi
    },
)

@Composable
private fun highLatitudeLabel(rule: HighLatitudeRule): String = stringResource(
    when (rule) {
        HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> R.string.middle_of_night
        HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> R.string.seventh_of_night
        HighLatitudeRule.TWILIGHT_ANGLE -> R.string.twilight_angle
    },
)

/**
 * The Adhan state and Fajr's reminder in one line — the full controls are one tap away on the
 * notifications hub. A master switch that is off outranks the per-prayer count: five prayers
 * "enabled" under it deliver nothing.
 */
@Composable
private fun remindersSummary(summary: NotificationSummary): String {
    val adhan = when {
        !summary.notificationsMasterEnabled -> stringResource(R.string.prayer_settings_notifications_off)
        summary.enabledPrayerCount == NotificationSummary.TOTAL_PRAYER_COUNT ->
            stringResource(R.string.prayer_settings_all_prayers_enabled)
        summary.enabledPrayerCount == 0 -> stringResource(R.string.prayer_settings_no_prayers_enabled)
        else -> stringResource(
            R.string.prayer_settings_prayers_enabled_count,
            summary.enabledPrayerCount,
            NotificationSummary.TOTAL_PRAYER_COUNT,
        )
    }
    if (!summary.notificationsMasterEnabled) return adhan
    val fajr = if (summary.reminderEnabled) {
        stringResource(R.string.prayer_picker_fajr_at,
            pluralStringResource(R.plurals.notif_reminder_minutes_before, summary.reminderMinutes, summary.reminderMinutes))
    } else {
        stringResource(R.string.prayer_settings_reminder_off)
    }
    return "$adhan · $fajr"
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
