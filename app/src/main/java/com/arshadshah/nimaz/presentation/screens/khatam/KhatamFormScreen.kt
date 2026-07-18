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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonType
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.atoms.KhatamProgressRing
import com.arshadshah.nimaz.presentation.components.molecules.NimazConfirmDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperVariant
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.KhatamEvent
import com.arshadshah.nimaz.presentation.viewmodel.KhatamFormUiState
import com.arshadshah.nimaz.presentation.viewmodel.KhatamPacePreset
import com.arshadshah.nimaz.presentation.viewmodel.KhatamViewModel
import com.arshadshah.nimaz.domain.model.Khatam

/**
 * One form, two modes.
 *
 * Create and Edit differ only in what they preload, what the primary action says, and
 * whether the danger zone is shown — so they share a single composable rather than
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(
                    if (state.isEdit) R.string.khatam_edit_title else R.string.khatam_new
                ),
                onBackClick = onNavigateBack,
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            KhatamFormContent(
                state = state,
                contentPadding = padding,
                onEvent = viewModel::onEvent,
                onArchived = onNavigateBack,
                onDeleted = onNavigateBack,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KhatamFormContent(
    state: KhatamFormUiState,
    contentPadding: PaddingValues,
    onEvent: (KhatamEvent) -> Unit,
    onArchived: () -> Unit,
    onDeleted: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    val formatter = rememberKhatamDateFormatter()

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
                    colors = NimazCardDefaults.colors(
                        container = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
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
                placeholder = { Text(stringResource(R.string.khatam_name_placeholder)) },
                singleLine = true,
                isError = state.errorRes != null,
                supportingText = state.errorRes?.let { { Text(stringResource(it)) } },
            )
        }

        item(key = "pace") {
            FieldLabel(stringResource(R.string.khatam_field_pace))
            Row(horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small)) {
                KhatamPacePreset.entries.forEach { preset ->
                    NimazChip(
                        text = stringResource(presetLabelRes(preset)),
                        selected = state.preset == preset,
                        onClick = { onEvent(KhatamEvent.SelectPreset(preset)) },
                    )
                }
            }
            Spacer(Modifier.height(NimazSpacing.Small))
            val context = LocalContext.current
            NimazNumberStepper(
                value = state.dailyTarget,
                onValueChange = { onEvent(KhatamEvent.UpdateDailyTarget(it)) },
                modifier = Modifier.fillMaxWidth(),
                variant = NimazNumberStepperVariant.SPREAD,
                // Resolved through the context rather than inlined: this used to be
                // a hardcoded "$it ayahs".
                formatValue = { context.getString(R.string.khatam_ayahs_per_day, it) },
                minValue = 1,
                maxValue = 1000,
            )
        }

        item(key = "projection") {
            val days = state.projectedDays
            if (days != null) {
                NimazCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = NimazCardDefaults.colors(
                        container = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                ) {
                    Column(Modifier.padding(NimazSpacing.Medium)) {
                        Text(
                            text = stringResource(
                                R.string.khatam_projected_finish,
                                formatter.format(
                                    System.currentTimeMillis() + days * DAY_MILLIS
                                ),
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
                    IconButton(onClick = { onEvent(KhatamEvent.UpdateDeadline(null)) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.khatam_deadline_clear),
                        )
                    }
                }
            }
        }

        item(key = "reminder") {
            FieldLabel(stringResource(R.string.khatam_field_reminder))
            NimazCard(
                modifier = Modifier.fillMaxWidth(),
                colors = NimazCardDefaults.colors(
                    container = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(NimazSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = state.reminderTime ?: DEFAULT_REMINDER_TIME,
                        style = MaterialTheme.typography.bodyMedium,
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
                    .height(100.dp),
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

        // Archive and delete live here rather than behind a long-press on the list,
        // where they were undiscoverable.
        if (state.isEdit) {
            item(key = "danger") {
                Spacer(Modifier.height(NimazSpacing.Medium))
                NimazSectionHeader(title = stringResource(R.string.khatam_section_danger))
                NimazButton(
                    text = stringResource(R.string.khatam_action_archive),
                    onClick = { showArchiveConfirm = true },
                    variant = NimazButtonVariant.OUTLINED,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(NimazSpacing.Small))
                NimazButton(
                    text = stringResource(R.string.khatam_action_delete),
                    onClick = { showDeleteConfirm = true },
                    variant = NimazButtonVariant.DESTRUCTIVE,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    val editId = (state.mode as? com.arshadshah.nimaz.presentation.viewmodel.KhatamFormMode.Edit)
        ?.khatamId

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.deadline)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(KhatamEvent.UpdateDeadline(pickerState.selectedDateMillis))
                    showDatePicker = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        ) { DatePicker(state = pickerState) }
    }

    if (showArchiveConfirm && editId != null) {
        NimazConfirmDialog(
            title = stringResource(R.string.khatam_archive_title),
            message = stringResource(R.string.khatam_archive_message, state.name),
            onConfirm = {
                onEvent(KhatamEvent.AbandonKhatam(editId))
                showArchiveConfirm = false
                onArchived()
            },
            onDismiss = { showArchiveConfirm = false },
        )
    }

    if (showDeleteConfirm && editId != null) {
        NimazConfirmDialog(
            title = stringResource(R.string.khatam_delete_title),
            message = stringResource(R.string.khatam_delete_message, state.name),
            isDestructive = true,
            onConfirm = {
                onEvent(KhatamEvent.DeleteKhatam(editId))
                showDeleteConfirm = false
                onDeleted()
            },
            onDismiss = { showDeleteConfirm = false },
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
