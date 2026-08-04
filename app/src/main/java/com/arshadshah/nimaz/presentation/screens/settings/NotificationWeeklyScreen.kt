package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazTime
import com.arshadshah.nimaz.presentation.components.atoms.NimazTimePicker
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperVariant
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel

/**
 * Weekly & reading subscreen (#301): the Friday (Jumu'ah) reminder + its lead-time stepper, and
 * the Khatam daily reading reminder + its time picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationWeeklyScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val notificationState by viewModel.notificationState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val minutesValueFormat = stringResource(R.string.notification_settings_minutes_value)

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.notif_hub_weekly_title),
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

            // Both reminders carry their own timing, so each is an accordion holding the
            // control that sets it. The khatam time used to sit behind a dialog, which meant
            // two taps and a modal to move a reminder by fifteen minutes.
            item {
                NimazAccordion(
                    title = stringResource(R.string.notification_settings_friday_reminder),
                    subtitle = if (notificationState.fridayReminderEnabled) {
                        minutesValueFormat.format(notificationState.fridayReminderMinutes)
                    } else {
                        stringResource(R.string.notification_settings_friday_subtitle)
                    },
                    trailing = {
                        NimazSwitch(
                            checked = notificationState.fridayReminderEnabled,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.SetFridayReminderEnabled(it))
                            }
                        )
                    }
                ) {
                    NimazNumberStepper(
                        value = notificationState.fridayReminderMinutes,
                        onValueChange = { viewModel.onEvent(SettingsEvent.SetFridayReminderMinutes(it)) },
                        variant = NimazNumberStepperVariant.INLINE,
                        label = stringResource(R.string.notification_settings_lead_time),
                        formatValue = { min -> minutesValueFormat.format(min) },
                        minValue = 15, maxValue = 120, step = 15,
                        editable = notificationState.fridayReminderEnabled,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            item {
                NimazAccordion(
                    title = stringResource(R.string.notification_settings_khatam_reminder),
                    subtitle = if (notificationState.khatamReminderEnabled) {
                        notificationState.khatamReminderTime
                    } else {
                        stringResource(R.string.notification_settings_khatam_subtitle)
                    },
                    trailing = {
                        NimazSwitch(
                            checked = notificationState.khatamReminderEnabled,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.SetKhatamReminderEnabled(it))
                            }
                        )
                    }
                ) {
                    NimazTimePicker(
                        value = NimazTime.parse(notificationState.khatamReminderTime),
                        onValueChange = {
                            viewModel.onEvent(SettingsEvent.SetKhatamReminderTime(it.toStorageString()))
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
