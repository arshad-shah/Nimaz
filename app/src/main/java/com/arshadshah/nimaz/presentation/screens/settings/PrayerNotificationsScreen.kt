package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.organisms.NimazListPicker
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.organisms.NimazPickerItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.settings.NotificationSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** The lead times the reminder picker offers. Null is "no reminder". */
private val REMINDER_CHOICES = listOf(null, 5, 10, 15, 20, 30, 45, 60)

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * One prayer's row, resolved from the shared notification state so the row renders from a
 * single value rather than five parallel lookups.
 */
internal data class PrayerNotificationRowState(
    val key: String,
    val name: String,
    val accentColor: Color,
    val time: LocalDateTime?,
    val isEnabled: Boolean,
    val alertStyle: PrayerAlertStyle,
    val reminderMinutes: Int?,
)

/**
 * Prayer notifications: one accordion per prayer, each carrying its own alert style and its
 * own reminder.
 *
 * Both used to be global — a single adhan on/off pair and one pre-adhan lead time for all
 * five — which meant a person who wanted the adhan at Fajr and silence at Dhuhr could not
 * have it. The header states what the prayer is set to without being opened, because the
 * question this screen answers is usually "what happens at Asr?" rather than "what can I
 * change?".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerNotificationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val notificationState by viewModel.notificationState.collectAsStateWithLifecycle()
    val prayerTimes by viewModel.todayPrayerTimes.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Which sheet is open, if any. One nullable value rather than a boolean per prayer per
    // setting — ten booleans that can contradict each other.
    var openSheet by remember { mutableStateOf<PrayerSettingSheet?>(null) }

    val rows = rememberPrayerRows(notificationState, prayerTimes)

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.notif_hub_prayers_title),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                NimazSectionHeader(title = stringResource(R.string.notif_all_prayers_section))
            }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.notif_all_prayers_reminder_title),
                        subtitle = stringResource(R.string.notif_all_prayers_reminder_subtitle),
                        checked = notificationState.showReminderBefore,
                        onCheckedChange = { enabled ->
                            viewModel.applyReminderToAllPrayers(
                                enabled = enabled,
                                minutes = notificationState.reminderMinutes,
                            )
                        }
                    )
                    NimazSettingsItem(
                        title = stringResource(R.string.notif_all_prayers_lead_title),
                        value = reminderLabel(
                            notificationState.reminderMinutes
                                .takeIf { notificationState.showReminderBefore }
                        ),
                        onClick = { openSheet = PrayerSettingSheet.AllPrayersReminder },
                        showArrow = true,
                    )
                }
            }
            item {
                NimazSectionHeader(title = stringResource(R.string.notif_prayers_section))
            }

            items(rows.size, key = { rows[it].key }) { index ->
                val row = rows[index]
                PrayerAccordion(
                    row = row,
                    sunriseEnabled = notificationState.sunriseNotification,
                    onToggle = { enabled ->
                        viewModel.onEvent(SettingsEvent.SetPrayerNotification(row.key, enabled))
                    },
                    onToggleSunrise = { enabled ->
                        viewModel.onEvent(SettingsEvent.SetPrayerNotification("sunrise", enabled))
                    },
                    onOpenAlertStyle = { openSheet = PrayerSettingSheet.AlertStyle(row.key) },
                    onOpenReminder = { openSheet = PrayerSettingSheet.Reminder(row.key) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    when (val sheet = openSheet) {
        is PrayerSettingSheet.AlertStyle -> AlertStylePicker(
            selected = notificationState.alertStyles[sheet.prayer] ?: PrayerAlertStyle.NOTIFICATION,
            onSelected = { style ->
                viewModel.onEvent(SettingsEvent.SetPrayerAlertStyle(sheet.prayer, style))
            },
            onDismiss = { openSheet = null }
        )

        PrayerSettingSheet.AllPrayersReminder -> ReminderPicker(
            selected = notificationState.reminderMinutes
                .takeIf { notificationState.showReminderBefore },
            onSelected = { minutes ->
                viewModel.applyReminderToAllPrayers(
                    enabled = minutes != null,
                    minutes = minutes ?: notificationState.reminderMinutes,
                )
            },
            onDismiss = { openSheet = null }
        )

        is PrayerSettingSheet.Reminder -> ReminderPicker(
            selected = notificationState.reminderMinutesFor(sheet.prayer),
            onSelected = { minutes ->
                viewModel.onEvent(
                    SettingsEvent.SetPrayerReminderEnabled(sheet.prayer, minutes != null)
                )
                if (minutes != null) {
                    viewModel.onEvent(
                        SettingsEvent.SetPrayerReminderMinutes(sheet.prayer, minutes)
                    )
                }
            },
            onDismiss = { openSheet = null }
        )

        null -> Unit
    }
}

/** Which picker sheet is open, and for which prayer. */
private sealed interface PrayerSettingSheet {
    data class AlertStyle(val prayer: String) : PrayerSettingSheet
    data class Reminder(val prayer: String) : PrayerSettingSheet

    /** The bulk lead-time picker, which is not about one prayer. */
    data object AllPrayersReminder : PrayerSettingSheet
}

/**
 * Set every prayer's reminder at once, and remember the choice as the app-wide default.
 *
 * The reminder became per prayer in the notifications rework, which left no way to say "warn
 * me before all five" without opening five accordions — and left the app-wide preference
 * ([SettingsEvent.SetShowReminderBefore] / [SettingsEvent.SetReminderMinutes]) with nothing
 * writing it, so it sat at its default while the per-prayer values moved. Both are written
 * here: the app-wide pair is what a new prayer's reminder falls back to and what a delivered
 * reminder reads when its alarm predates the per-prayer split, and the five per-prayer events
 * are what actually reschedules the alarms. Writing only the app-wide pair would ship a
 * control that changes no notification.
 *
 * The lead time is written even when the reminder is being turned off, so switching it back on
 * restores the number the user last chose rather than the default.
 */
private fun SettingsViewModel.applyReminderToAllPrayers(enabled: Boolean, minutes: Int) {
    onEvent(SettingsEvent.SetShowReminderBefore(enabled))
    onEvent(SettingsEvent.SetReminderMinutes(minutes))
    PrayerAlertStyle.PRAYER_KEYS.forEach { prayer ->
        onEvent(SettingsEvent.SetPrayerReminderEnabled(prayer, enabled))
        if (enabled) {
            onEvent(SettingsEvent.SetPrayerReminderMinutes(prayer, minutes))
        }
    }
}

@Composable
private fun PrayerAccordion(
    row: PrayerNotificationRowState,
    sunriseEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onToggleSunrise: (Boolean) -> Unit,
    onOpenAlertStyle: () -> Unit,
    onOpenReminder: () -> Unit,
) {
    NimazAccordion(
        title = row.name,
        subtitle = prayerSummary(row),
        trailing = {
            // The accent bar and the time read as one unit: this prayer, at this hour.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(row.accentColor)
            )
            Spacer(Modifier.width(8.dp))
            if (row.time != null) {
                Text(
                    text = row.time.format(TIME_FORMAT),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
            }
            NimazSwitch(checked = row.isEnabled, onCheckedChange = onToggle)
        }
    ) {
        NimazSettingsItem(
            title = stringResource(R.string.notif_alert_style),
            value = stringResource(alertStyleLabel(row.alertStyle)),
            onClick = onOpenAlertStyle,
            showArrow = true,
            enabled = row.isEnabled
        )
        NimazSettingsItem(
            title = stringResource(R.string.notif_reminder_before),
            value = reminderLabel(row.reminderMinutes),
            onClick = onOpenReminder,
            showArrow = true,
            enabled = row.isEnabled
        )
        // Sunrise is not a prayer with an alert style of its own — it is the end of Fajr's
        // window, so it belongs under Fajr rather than in a section of global leftovers.
        if (row.key == "fajr") {
            NimazSettingsItem(
                title = stringResource(R.string.notif_sunrise),
                subtitle = stringResource(R.string.notif_sunrise_subtitle),
                checked = sunriseEnabled,
                onCheckedChange = onToggleSunrise
            )
        }
    }
}

@Composable
private fun AlertStylePicker(
    selected: PrayerAlertStyle,
    onSelected: (PrayerAlertStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val items = listOf(
        NimazPickerItem(
            value = PrayerAlertStyle.ADHAN,
            title = stringResource(R.string.notif_alert_style_adhan),
            description = stringResource(R.string.notif_alert_style_adhan_subtitle),
            icon = Icons.Default.Campaign,
        ),
        NimazPickerItem(
            value = PrayerAlertStyle.NOTIFICATION,
            title = stringResource(R.string.notif_alert_style_notification),
            description = stringResource(R.string.notif_alert_style_notification_subtitle),
            icon = Icons.Default.NotificationsActive,
        ),
        NimazPickerItem(
            value = PrayerAlertStyle.SILENT,
            title = stringResource(R.string.notif_alert_style_silent),
            description = stringResource(R.string.notif_alert_style_silent_subtitle),
            icon = Icons.AutoMirrored.Filled.VolumeOff,
        ),
    )

    NimazListPicker(
        title = stringResource(R.string.notif_alert_style),
        items = items,
        selected = selected,
        onSelected = onSelected,
        onDismiss = onDismiss,
    )
}

@Composable
private fun ReminderPicker(
    selected: Int?,
    onSelected: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val items = REMINDER_CHOICES.map { minutes ->
        NimazPickerItem(
            value = minutes ?: 0,
            title = reminderLabel(minutes),
            icon = if (minutes == null) null else Icons.Default.Schedule,
        )
    }

    NimazListPicker(
        title = stringResource(R.string.notif_reminder_before),
        items = items,
        selected = selected ?: 0,
        onSelected = { onSelected(it.takeIf { value -> value != 0 }) },
        onDismiss = onDismiss,
        searchable = false,
    )
}

/** The one-line summary in the header: what this prayer does, and how far ahead it warns. */
@Composable
private fun prayerSummary(row: PrayerNotificationRowState): String {
    if (!row.isEnabled) return stringResource(R.string.notif_prayer_off)

    val style = stringResource(alertStyleLabel(row.alertStyle))
    val reminder = row.reminderMinutes ?: return style
    return stringResource(
        R.string.notif_prayer_summary,
        style,
        pluralStringResource(R.plurals.notif_reminder_minutes_before, reminder, reminder)
    )
}

@Composable
private fun reminderLabel(minutes: Int?): String =
    if (minutes == null) stringResource(R.string.notif_reminder_none)
    else pluralStringResource(R.plurals.notif_reminder_minutes_before, minutes, minutes)

private fun alertStyleLabel(style: PrayerAlertStyle): Int = when (style) {
    PrayerAlertStyle.ADHAN -> R.string.notif_alert_style_adhan
    PrayerAlertStyle.NOTIFICATION -> R.string.notif_alert_style_notification
    PrayerAlertStyle.SILENT -> R.string.notif_alert_style_silent
}

/** This prayer's lead time, or null when its reminder is off. */
private fun NotificationSettingsUiState.reminderMinutesFor(prayer: String): Int? =
    if (reminderEnabled[prayer] == true) {
        reminderOffsets[prayer] ?: PrayerAlertStyle.DEFAULT_REMINDER_MINUTES
    } else {
        null
    }

@Composable
private fun rememberPrayerRows(
    state: NotificationSettingsUiState,
    times: PrayerTimes?,
): List<PrayerNotificationRowState> {
    val names = listOf(
        stringResource(R.string.prayer_fajr),
        stringResource(R.string.prayer_dhuhr),
        stringResource(R.string.prayer_asr),
        stringResource(R.string.prayer_maghrib),
        stringResource(R.string.prayer_isha),
    )
    val enabled = listOf(
        state.fajrNotification,
        state.dhuhrNotification,
        state.asrNotification,
        state.maghribNotification,
        state.ishaNotification,
    )
    val accents = listOf(
        NimazColors.PrayerColors.Fajr,
        NimazColors.Gold500,
        NimazColors.PrayerColors.Asr,
        NimazColors.PrayerColors.Maghrib,
        NimazColors.PrayerColors.Isha,
    )
    val clockTimes = listOf(
        times?.fajr, times?.dhuhr, times?.asr, times?.maghrib, times?.isha
    )

    return PrayerAlertStyle.PRAYER_KEYS.mapIndexed { index, key ->
        PrayerNotificationRowState(
            key = key,
            name = names[index],
            accentColor = accents[index],
            time = clockTimes[index],
            isEnabled = enabled[index],
            alertStyle = state.alertStyles[key] ?: PrayerAlertStyle.NOTIFICATION,
            reminderMinutes = state.reminderMinutesFor(key),
        )
    }
}
