package com.arshadshah.nimaz.presentation.screens.khatam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.KhatamProgressRing
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonType
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.atoms.NimazTime
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazDatePickerDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownField
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownMenu
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownRow
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazTimePickerDialog
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.KhatamEvent
import com.arshadshah.nimaz.presentation.viewmodel.KhatamFormMode
import com.arshadshah.nimaz.presentation.viewmodel.KhatamFormUiState
import com.arshadshah.nimaz.presentation.viewmodel.KhatamPacePreset
import com.arshadshah.nimaz.presentation.viewmodel.KhatamViewModel

/**
 * One form, two modes.
 *
 * Create and Edit differ only in what they preload, what the primary action says, and
 * whether the overflow menu is shown — so they share a single composable rather than
 * existing as two near-identical screens that drift apart.
 *
 * @param khatamId null to create, non-null to edit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatamFormScreen(
    khatamId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: KhatamViewModel = hiltViewModel(),
) {
    val state by viewModel.formState.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(khatamId) {
        if (khatamId == null) {
            viewModel.onEvent(KhatamEvent.StartCreate())
        } else {
            viewModel.onEvent(KhatamEvent.StartEdit(khatamId))
        }
    }

    // Navigate only once the write has actually committed.
    LaunchedEffect(state.saveComplete) {
        if (state.saveComplete) {
            viewModel.onEvent(KhatamEvent.ConsumeSaveComplete)
            onNavigateBack()
        }
    }

    val editId = (state.mode as? KhatamFormMode.Edit)?.khatamId

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(
                    if (state.isEdit) R.string.khatam_edit_title else R.string.khatam_new
                ),
                onBackClick = onNavigateBack,
                actions = {
                    if (state.isEdit && editId != null) {
                        Box {
                            NimazIconButton(
                                icon = Icons.Default.MoreVert,
                                onClick = { menuExpanded = true },
                                contentDescription =
                                    stringResource(R.string.khatam_more_actions),
                            )
                            // Destructive actions live in the overflow menu rather than an
                            // inline "danger zone", which put delete a full scroll away
                            // from the save button.
                            NimazDropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                if (!state.isActiveKhatam) {
                                    NimazDropdownRow(
                                        text = stringResource(R.string.khatam_set_active),
                                        leadingIcon = Icons.Default.Star,
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.onEvent(
                                                KhatamEvent.SetActiveKhatam(editId)
                                            )
                                        },
                                    )
                                }
                                NimazDropdownRow(
                                    text = stringResource(R.string.khatam_action_archive),
                                    leadingIcon = Icons.Default.Archive,
                                    onClick = {
                                        menuExpanded = false
                                        showArchiveConfirm = true
                                    },
                                )
                                NimazDropdownRow(
                                    text = stringResource(R.string.khatam_action_delete),
                                    leadingIcon = Icons.Default.Delete,
                                    destructive = true,
                                    onClick = {
                                        menuExpanded = false
                                        showDeleteConfirm = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            NimazLoadingState(modifier = Modifier.padding(padding))
        } else {
            KhatamFormContent(
                state = state,
                contentPadding = padding,
                onEvent = viewModel::onEvent,
            )
        }
    }

    if (showArchiveConfirm && editId != null) {
        NimazConfirmDialog(
            title = stringResource(R.string.khatam_archive_title),
            message = stringResource(R.string.khatam_archive_message, state.name),
            confirmText = stringResource(R.string.khatam_action_archive),
            cancelText = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.onEvent(KhatamEvent.AbandonKhatam(editId))
                showArchiveConfirm = false
                onNavigateBack()
            },
            onDismiss = { showArchiveConfirm = false },
        )
    }

    if (showDeleteConfirm && editId != null) {
        NimazConfirmDialog(
            title = stringResource(R.string.khatam_delete_title),
            message = stringResource(R.string.khatam_delete_message, state.name),
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            isDestructive = true,
            onConfirm = {
                viewModel.onEvent(KhatamEvent.DeleteKhatam(editId))
                showDeleteConfirm = false
                onNavigateBack()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun KhatamFormContent(
    state: KhatamFormUiState,
    contentPadding: PaddingValues,
    onEvent: (KhatamEvent) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val formatter = rememberKhatamDateFormatter()

    val presetItems = KhatamPacePreset.entries.map { preset ->
        NimazDropdownItem(
            value = preset,
            label = stringResource(presetLabelRes(preset)),
            description = preset.targetAyahs()?.let {
                pluralStringResource(R.plurals.khatam_ayahs_per_day, it, it)
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = NimazSpacing.Large,
            end = NimazSpacing.Large,
            top = contentPadding.calculateTopPadding() + NimazSpacing.Small,
            bottom = contentPadding.calculateBottomPadding() + NimazSpacing.ExtraLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
    ) {
        // On edit, lead with progress and state plainly that editing won't touch it.
        if (state.isEdit) {
            item(key = "progress-note") {
                NimazCard(
                    modifier = Modifier.fillMaxWidth(),
                    tone = NimazTone.NEUTRAL,
                    style = NimazCardStyle.ELEVATED,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(NimazSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KhatamProgressRing(
                            progress = state.totalAyahsRead.toFloat() /
                                Khatam.TOTAL_QURAN_AYAHS,
                            size = 44.dp,
                            strokeWidth = 5.dp,
                        )
                        Spacer(Modifier.width(NimazSpacing.Medium))
                        Column {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.khatam_ayahs_read_plural,
                                    state.totalAyahsRead,
                                    state.totalAyahsRead,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.khatam_edit_progress_note),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item(key = "name") {
            FieldLabel(stringResource(R.string.khatam_name_label))
            OutlinedTextField(
                value = state.name,
                onValueChange = { onEvent(KhatamEvent.UpdateName(it)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(NimazCornerRadius.Medium),
                placeholder = { Text(stringResource(R.string.khatam_name_placeholder)) },
                singleLine = true,
                isError = state.errorRes != null,
                supportingText = state.errorRes?.let { { Text(stringResource(it)) } },
            )
        }

        item(key = "pace") {
            // A dropdown rather than a chip row: four options, each wanting a subtitle,
            // do not fit one line of chips at larger font scales.
            NimazDropdownField(
                items = presetItems,
                selected = state.preset,
                onSelected = { onEvent(KhatamEvent.SelectPreset(it)) },
                label = stringResource(R.string.khatam_field_pace),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(NimazSpacing.Small))
            // No formatValue on purpose: the stepper's editable field parses its own
            // display text back to an Int, so a formatted "208 ayahs / day" made direct
            // entry impossible. The unit lives in the label and the field stays numeric,
            // which is also what gets the number keyboard on tap.
            NimazNumberStepper(
                value = state.dailyTarget,
                onValueChange = { onEvent(KhatamEvent.UpdateDailyTarget(it)) },
                modifier = Modifier.fillMaxWidth(),
                variant = NimazNumberStepperVariant.SPREAD,
                label = stringResource(R.string.khatam_ayahs_per_day_label),
                minValue = 1,
                maxValue = 1000,
            )
        }

        item(key = "projection") {
            val days = state.projectedDays
            if (days != null) {
                NimazCard(
                    modifier = Modifier.fillMaxWidth(),
                    tone = NimazTone.NEUTRAL,
                    style = NimazCardStyle.ELEVATED,
                ) {
                    Column(Modifier.padding(NimazSpacing.Medium)) {
                        Text(
                            text = stringResource(
                                R.string.khatam_projected_finish,
                                formatter.format(System.currentTimeMillis() + days * DAY_MILLIS),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.khatam_ayahs_remaining,
                                state.remainingAyahs,
                                state.remainingAyahs,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item(key = "deadline") {
            FieldLabel(
                stringResource(R.string.khatam_field_deadline) +
                    " · " + stringResource(R.string.khatam_optional)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                NimazButton(
                    text = state.deadline?.let { formatter.format(it) }
                        ?: stringResource(R.string.khatam_deadline_not_set),
                    onClick = { showDatePicker = true },
                    variant = NimazButtonVariant.OUTLINED,
                    leadingIcon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f),
                )
                if (state.deadline != null) {
                    NimazIconButton(
                        icon = Icons.Default.Close,
                        onClick = { onEvent(KhatamEvent.UpdateDeadline(null)) },
                        contentDescription = stringResource(R.string.khatam_deadline_clear),
                    )
                }
            }
        }

        item(key = "reminder") {
            FieldLabel(stringResource(R.string.khatam_field_reminder))
            NimazCard(
                modifier = Modifier.fillMaxWidth(),
                tone = NimazTone.NEUTRAL,
                style = NimazCardStyle.OUTLINED,
                elevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(NimazSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    NimazButton(
                        text = state.reminderTime ?: DEFAULT_REMINDER_TIME,
                        onClick = { showTimePicker = true },
                        variant = NimazButtonVariant.TEXT,
                        enabled = state.reminderEnabled,
                    )
                    NimazSwitch(
                        checked = state.reminderEnabled,
                        onCheckedChange = { enabled ->
                            onEvent(KhatamEvent.UpdateReminderEnabled(enabled))
                            if (enabled && state.reminderTime == null) {
                                onEvent(KhatamEvent.UpdateReminderTime(DEFAULT_REMINDER_TIME))
                            }
                        },
                    )
                }
            }
        }

        item(key = "notes") {
            FieldLabel(stringResource(R.string.khatam_notes_label))
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onEvent(KhatamEvent.UpdateNotes(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                // Multi-line fields inherit the single-line shape, which reads as
                // under-rounded at this height.
                shape = RoundedCornerShape(NimazCornerRadius.Medium),
                placeholder = { Text(stringResource(R.string.khatam_notes_placeholder)) },
            )
        }

        item(key = "save") {
            Spacer(Modifier.height(NimazSpacing.Small))
            NimazButton(
                text = stringResource(
                    if (state.isEdit) R.string.khatam_action_save
                    else R.string.khatam_action_begin
                ),
                onClick = { onEvent(KhatamEvent.SaveKhatam) },
                modifier = Modifier.fillMaxWidth(),
                type = NimazButtonType.PILL,
                loading = state.isSaving,
                enabled = !state.isSaving,
                fullWidth = true,
            )
        }
    }

    if (showDatePicker) {
        NimazDatePickerDialog(
            selectedDateMillis = state.deadline,
            onConfirm = {
                onEvent(KhatamEvent.UpdateDeadline(it))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            title = stringResource(R.string.khatam_field_deadline),
        )
    }

    if (showTimePicker) {
        NimazTimePickerDialog(
            initialTime = NimazTime.parse(state.reminderTime),
            onConfirm = {
                onEvent(KhatamEvent.UpdateReminderTime(it.toStorageString()))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
            title = stringResource(R.string.notification_settings_reminder_time),
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = NimazSpacing.Small, bottom = NimazSpacing.ExtraSmall),
    )
}

private fun presetLabelRes(preset: KhatamPacePreset): Int = when (preset) {
    KhatamPacePreset.JUZ_DAILY -> R.string.khatam_preset_juz_daily
    KhatamPacePreset.HALF_JUZ_DAILY -> R.string.khatam_preset_half_juz_daily
    KhatamPacePreset.QUARTER_JUZ_DAILY -> R.string.khatam_preset_quarter_juz_daily
    KhatamPacePreset.CUSTOM -> R.string.khatam_preset_custom
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000
private const val DEFAULT_REMINDER_TIME = "06:00"
