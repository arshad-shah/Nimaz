package com.arshadshah.nimaz.presentation.screens.fasting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownField
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FastManagementBottomSheet(
    isVisible: Boolean,
    date: LocalDate,
    existingRecord: FastRecord?,
    initialStatus: FastStatus,
    initialFastType: FastType,
    initialExemptionReason: ExemptionReason?,
    initialNote: String,
    onSave: (FastStatus, FastType, ExemptionReason?, String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    // Local state managed within the sheet
    var selectedStatus by remember(date, existingRecord?.id) { mutableStateOf(initialStatus) }
    var selectedFastType by remember(date, existingRecord?.id) { mutableStateOf(initialFastType) }
    var selectedExemptionReason by remember(date, existingRecord?.id) {
        mutableStateOf(
            initialExemptionReason
        )
    }
    var note by remember(date, existingRecord?.id) { mutableStateOf(initialNote) }

    val hijriDate = remember(date) { HijriDateCalculator.toHijri(date) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy") }

    NimazBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Gregorian + Hijri date
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = hijriDate.formatted(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status selector
            Text(
                text = stringResource(R.string.fasting_sheet_status),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    FastStatus.FASTED,
                    FastStatus.NOT_FASTED,
                    FastStatus.EXEMPTED
                ).forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = {
                            Text(
                                when (status) {
                                    FastStatus.FASTED -> stringResource(R.string.fasting_status_fasting)
                                    FastStatus.NOT_FASTED -> stringResource(R.string.fasting_status_not_fasting)
                                    FastStatus.EXEMPTED -> stringResource(R.string.fasting_status_exempted)
                                    else -> ""
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (status) {
                                FastStatus.FASTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                FastStatus.NOT_FASTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                FastStatus.EXEMPTED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    )
                }
            }

            // Fast type selector
            Text(
                text = stringResource(R.string.fasting_sheet_type),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FastType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedFastType == type,
                        onClick = { selectedFastType = type },
                        label = { Text(type.displayName()) }
                    )
                }
            }

            // Exemption reason (only when EXEMPTED)
            AnimatedVisibility(visible = selectedStatus == FastStatus.EXEMPTED) {
                ExemptionReasonSelector(
                    selectedReason = selectedExemptionReason,
                    onReasonSelected = { selectedExemptionReason = it }
                )
            }

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.fasting_sheet_note)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existingRecord != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.fasting_sheet_delete))
                    }
                }
                Button(
                    onClick = {
                        onSave(selectedStatus, selectedFastType, selectedExemptionReason, note)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.fasting_sheet_save))
                }
            }
        }
    }
}

@Composable
private fun ExemptionReasonSelector(
    selectedReason: ExemptionReason?,
    onReasonSelected: (ExemptionReason?) -> Unit
) {
    NimazDropdownField(
        label = stringResource(R.string.fasting_sheet_exemption_reason),
        items = ExemptionReason.entries.map { reason ->
            NimazDropdownItem(value = reason, label = reason.displayName())
        },
        selected = selectedReason,
        placeholder = stringResource(R.string.fasting_sheet_select_reason),
        onSelected = { onReasonSelected(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MakeupFastEditBottomSheet(
    makeupFast: MakeupFast?,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSave: (MakeupFast) -> Unit,
    onPayFidya: (Long, Double) -> Unit
) {
    if (!isVisible || makeupFast == null) return

    var reason by remember(makeupFast.id) { mutableStateOf(makeupFast.reason) }
    var note by remember(makeupFast.id) { mutableStateOf(makeupFast.note ?: "") }
    var selectedStatus by remember(makeupFast.id) { mutableStateOf(makeupFast.status) }
    var fidyaAmount by remember(makeupFast.id) {
        mutableStateOf(
            makeupFast.fidyaAmount?.toString() ?: ""
        )
    }

    val originalDate = remember(makeupFast.originalDate) {
        Instant.ofEpochMilli(makeupFast.originalDate)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()
    }
    val hijriDate = remember(originalDate) {
        makeupFast.originalHijriDate ?: HijriDateCalculator.toHijri(originalDate).formatted()
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    NimazBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.fasting_sheet_edit_makeup),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.fasting_originally, hijriDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = originalDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reason
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text(stringResource(R.string.fasting_sheet_reason)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.fasting_sheet_note)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Status chips
            Text(
                text = stringResource(R.string.fasting_sheet_status),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MakeupFastStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = {
                            Text(
                                when (status) {
                                    MakeupFastStatus.PENDING -> stringResource(R.string.fasting_pending)
                                    MakeupFastStatus.COMPLETED -> stringResource(R.string.fasting_completed_label)
                                    MakeupFastStatus.FIDYA_PAID -> stringResource(R.string.fasting_fidya_paid)
                                }
                            )
                        }
                    )
                }
            }

            // Fidya amount (only when FIDYA_PAID)
            AnimatedVisibility(visible = selectedStatus == MakeupFastStatus.FIDYA_PAID) {
                OutlinedTextField(
                    value = fidyaAmount,
                    onValueChange = { fidyaAmount = it },
                    label = { Text(stringResource(R.string.fasting_sheet_fidya_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Save button
            Button(
                onClick = {
                    if (selectedStatus == MakeupFastStatus.FIDYA_PAID) {
                        val amount = fidyaAmount.toDoubleOrNull() ?: 0.0
                        onPayFidya(makeupFast.id, amount)
                    } else {
                        val updated = makeupFast.copy(
                            reason = reason,
                            note = note.ifBlank { null },
                            status = selectedStatus,
                            completedDate = if (selectedStatus == MakeupFastStatus.COMPLETED)
                                System.currentTimeMillis() else makeupFast.completedDate,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(updated)
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.fasting_sheet_save))
            }
        }
    }
}
