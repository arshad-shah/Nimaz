package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.WorshipReminderCategory
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

/**
 * Worship reminders subscreen (spec §3). Data-driven off [WorshipReminderType]: rows are generated
 * per category (Night / Ramadan / Fasting & Dhikr). The Ramadan group is shown year-round under
 * a notice saying it stays quiet until Ramadan, so it can be set up in advance. A reminder that
 * can be timed is an accordion carrying its own offset; the rest are plain switch rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorshipRemindersScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.notificationState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val minutesFormat = stringResource(R.string.worship_settings_minutes)
    // Drives the notice above the Ramadan group, not whether the group is shown.
    val isRamadan = remember { HijriDateCalculator.today().month == 9 }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.worship_settings_title),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                NimazBanner(
                    message = stringResource(R.string.worship_settings_intro),
                    variant = NimazBannerVariant.INFO,
                    modifier = Modifier
                )
            }

            val onToggle: (String, Boolean) -> Unit =
                { key, enabled -> viewModel.onEvent(SettingsEvent.SetWorshipReminderEnabled(key, enabled)) }
            val onOffset: (String, Int) -> Unit =
                { key, min -> viewModel.onEvent(SettingsEvent.SetWorshipReminderOffset(key, min)) }
            val onMode: (String, String) -> Unit =
                { key, mode -> viewModel.onEvent(SettingsEvent.SetWorshipReminderMode(key, mode)) }

            worshipSection(
                titleRes = R.string.worship_settings_section_night,
                types = WorshipReminderType.entries.filter { it.category == WorshipReminderCategory.NIGHT },
                state = state, minutesFormat = minutesFormat,
                onToggle = onToggle, onOffset = onOffset, onMode = onMode,
            )

            // Ramadan reminders are shown year-round rather than hidden. Hiding them made
            // the app look like it had lost a feature outside Ramadan, and left no way to
            // set them up in advance. Nothing depends on their absence — the scheduler
            // gates them on the Hijri date itself (WorshipReminderCalculator), so one set
            // outside Ramadan simply arms nothing until Ramadan arrives.
            if (!isRamadan) {
                item(key = "ramadan_notice") {
                    NimazBanner(
                        message = stringResource(R.string.worship_settings_ramadan_notice),
                        variant = NimazBannerVariant.INFO,
                        icon = Icons.Default.Schedule,
                        showBorder = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            worshipSection(
                titleRes = R.string.worship_settings_section_ramadan,
                types = WorshipReminderType.entries.filter { it.category == WorshipReminderCategory.RAMADAN },
                state = state, minutesFormat = minutesFormat,
                onToggle = onToggle, onOffset = onOffset, onMode = onMode,
            )

            worshipSection(
                titleRes = R.string.worship_settings_section_fasting,
                types = WorshipReminderType.entries.filter { it.category == WorshipReminderCategory.FASTING_DHIKR },
                state = state, minutesFormat = minutesFormat,
                onToggle = onToggle, onOffset = onOffset, onMode = onMode,
            )

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.worshipSection(
    titleRes: Int,
    types: List<WorshipReminderType>,
    state: com.arshadshah.nimaz.presentation.viewmodel.NotificationSettingsUiState,
    minutesFormat: String,
    onToggle: (String, Boolean) -> Unit,
    onOffset: (String, Int) -> Unit,
    onMode: (String, String) -> Unit,
) {
    if (types.isEmpty()) return
    item(key = "header_$titleRes") { NimazSectionHeader(title = stringResource(titleRes)) }
    item(key = "group_$titleRes") {
        // A reminder that can be timed carries its own accordion, so its offset sits inside
        // the thing it belongs to rather than as a loose row beneath it. The rest stay plain
        // switch rows in a single group.
        val (adjustable, plain) = types.partition {
            it.hasOffset || it == WorshipReminderType.WITR
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (plain.isNotEmpty()) {
                NimazMenuGroup {
                    plain.forEachIndexed { index, type ->
                        NimazSettingsItem(
                            title = stringResource(worshipNameRes(type)),
                            subtitle = stringResource(worshipWhenRes(type)),
                            checked = state.worshipReminders[type.key] ?: false,
                            onCheckedChange = { onToggle(type.key, it) }
                        )
                        if (index < plain.lastIndex) {
                            NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            adjustable.forEach { type ->
                val enabled = state.worshipReminders[type.key] ?: false
                val offset = state.worshipOffsets[type.key] ?: type.defaultOffsetMinutes
                val mode = state.worshipModes[type.key]
                    ?: com.arshadshah.nimaz.core.util.WorshipReminderCalculator.WITR_MODE_AFTER_ISHA
                val beforeFajr =
                    mode == com.arshadshah.nimaz.core.util.WorshipReminderCalculator.WITR_MODE_BEFORE_FAJR

                NimazAccordion(
                    title = stringResource(worshipNameRes(type)),
                    subtitle = if (enabled && type.hasOffset) {
                        minutesFormat.format(offset)
                    } else {
                        stringResource(worshipWhenRes(type))
                    },
                    trailing = {
                        NimazSwitch(checked = enabled, onCheckedChange = { onToggle(type.key, it) })
                    }
                ) {
                    // Witr timing mode (after Isha ↔ before Fajr) — tap to switch. #309.
                    if (type == WorshipReminderType.WITR) {
                        NimazSettingsItem(
                            title = stringResource(R.string.worship_witr_mode_title),
                            subtitle = stringResource(
                                if (beforeFajr) R.string.worship_witr_mode_before_fajr
                                else R.string.worship_witr_mode_after_isha
                            ),
                            onClick = {
                                onMode(
                                    type.key,
                                    if (beforeFajr) com.arshadshah.nimaz.core.util.WorshipReminderCalculator.WITR_MODE_AFTER_ISHA
                                    else com.arshadshah.nimaz.core.util.WorshipReminderCalculator.WITR_MODE_BEFORE_FAJR
                                )
                            },
                            showArrow = true,
                            enabled = enabled
                        )
                    }
                    if (type.hasOffset) {
                        NimazNumberStepper(
                            value = offset,
                            onValueChange = { onOffset(type.key, it) },
                            variant = NimazNumberStepperVariant.INLINE,
                            label = stringResource(R.string.worship_settings_timing),
                            formatValue = { min -> minutesFormat.format(min) },
                            minValue = 0,
                            maxValue = 60,
                            step = 5,
                            editable = enabled,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun worshipNameRes(type: WorshipReminderType): Int = when (type) {
    WorshipReminderType.TAHAJJUD -> R.string.worship_tahajjud_name
    WorshipReminderType.WITR -> R.string.worship_witr_name
    WorshipReminderType.SUHOOR -> R.string.worship_suhoor_name
    WorshipReminderType.IFTAR -> R.string.worship_iftar_name
    WorshipReminderType.TARAWEEH -> R.string.worship_taraweeh_name
    WorshipReminderType.LAYLATUL_QADR -> R.string.worship_laylatul_qadr_name
    WorshipReminderType.ADHKAR_MORNING -> R.string.worship_adhkar_morning_name
    WorshipReminderType.ADHKAR_EVENING -> R.string.worship_adhkar_evening_name
    WorshipReminderType.MONDAY_THURSDAY_FAST -> R.string.worship_mon_thu_name
    WorshipReminderType.WHITE_DAYS_FAST -> R.string.worship_white_days_name
    WorshipReminderType.ARAFAH_ASHURA_FAST -> R.string.worship_arafah_ashura_name
}

private fun worshipWhenRes(type: WorshipReminderType): Int = when (type) {
    WorshipReminderType.TAHAJJUD -> R.string.worship_when_tahajjud
    WorshipReminderType.WITR -> R.string.worship_when_witr
    WorshipReminderType.SUHOOR -> R.string.worship_when_suhoor
    WorshipReminderType.IFTAR -> R.string.worship_when_iftar
    WorshipReminderType.TARAWEEH -> R.string.worship_when_taraweeh
    WorshipReminderType.LAYLATUL_QADR -> R.string.worship_when_laylatul_qadr
    WorshipReminderType.ADHKAR_MORNING -> R.string.worship_when_adhkar_morning
    WorshipReminderType.ADHKAR_EVENING -> R.string.worship_when_adhkar_evening
    WorshipReminderType.MONDAY_THURSDAY_FAST -> R.string.worship_when_mon_thu
    WorshipReminderType.WHITE_DAYS_FAST -> R.string.worship_when_white_days
    WorshipReminderType.ARAFAH_ASHURA_FAST -> R.string.worship_when_arafah_ashura
}
