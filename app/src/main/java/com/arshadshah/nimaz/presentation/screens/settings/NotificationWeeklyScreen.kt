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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTime
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazTimePickerDialog
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
    val notificationState by viewModel.notificationState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val minutesValueFormat = stringResource(R.string.notification_settings_minutes_value)
    var showKhatamTimePicker by remember { mutableStateOf(false) }

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
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.notification_settings_friday_reminder),
                        subtitle = stringResource(R.string.notification_settings_friday_subtitle),
                        checked = notificationState.fridayReminderEnabled,
                        onCheckedChange = {
                            viewModel.onEvent(SettingsEvent.SetFridayReminderEnabled(!notificationState.fridayReminderEnabled))
                        }
                    )
                    if (notificationState.fridayReminderEnabled) {
                        NimazNumberStepper(
                            value = notificationState.fridayReminderMinutes,
                            onValueChange = { viewModel.onEvent(SettingsEvent.SetFridayReminderMinutes(it)) },
                            variant = NimazNumberStepperVariant.INLINE,
                            label = stringResource(R.string.notification_settings_lead_time),
                            formatValue = { min -> minutesValueFormat.format(min) },
                            minValue = 15, maxValue = 120, step = 15,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.notification_settings_khatam_reminder),
                        subtitle = stringResource(R.string.notification_settings_khatam_subtitle),
                        checked = notificationState.khatamReminderEnabled,
                        onCheckedChange = {
                            viewModel.onEvent(SettingsEvent.SetKhatamReminderEnabled(!notificationState.khatamReminderEnabled))
                        }
                    )
                    if (notificationState.khatamReminderEnabled) {
                        NimazSettingsItem(
                            title = stringResource(R.string.notification_settings_reminder_time),
                            subtitle = notificationState.khatamReminderTime,
                            onClick = { showKhatamTimePicker = true },
                            showArrow = true,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showKhatamTimePicker) {
        NimazTimePickerDialog(
            initialTime = NimazTime.parse(notificationState.khatamReminderTime),
            onConfirm = {
                viewModel.onEvent(SettingsEvent.SetKhatamReminderTime(it.toStorageString()))
                showKhatamTimePicker = false
            },
            onDismiss = { showKhatamTimePicker = false },
            title = stringResource(R.string.notification_settings_reminder_time),
        )
    }
}
