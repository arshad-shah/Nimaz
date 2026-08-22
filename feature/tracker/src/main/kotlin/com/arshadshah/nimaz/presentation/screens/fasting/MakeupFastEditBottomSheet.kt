package com.arshadshah.nimaz.presentation.screens.fasting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.core.common.formatMediumDate
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonType
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.NimazFieldLabel
import com.arshadshah.nimaz.presentation.components.molecules.NimazFieldVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazTextField
import java.time.Instant
import java.time.ZoneId

/**
 * Editing a make-up fast: its reason, and whether it was discharged by fidya.
 *
 * All that survives of `FastManagementBottomSheet.kt`. That file's other sheet asked four
 * questions about a *day* — status, fast type, reason, note — and the 2026-08 redesign answered
 * the first two inline on the day card and split the rest into two small sheets. This one is
 * about a make-up fast rather than a day, so none of that applied to it.
 */
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

    NimazBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.fasting_sheet_edit_makeup),
        subtitle = stringResource(R.string.fasting_originally, hijriDate),
        footer = {
            NimazButton(
                text = stringResource(R.string.fasting_sheet_save),
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
                variant = NimazButtonVariant.TONAL,
                type = NimazButtonType.PILL,
                leadingIcon = Icons.Default.Save,
                fullWidth = true
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = originalDate.formatMediumDate(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            NimazTextField(
                value = reason,
                onValueChange = { reason = it },
                label = stringResource(R.string.fasting_sheet_reason),
                modifier = Modifier.fillMaxWidth(),
            )

            NimazTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.fasting_sheet_note),
                variant = NimazFieldVariant.NOTE,
                modifier = Modifier.fillMaxWidth(),
            )

            // Status chips — a control, not a field, but it answers a question in the same
            // sheet, so it wears the family's label.
            NimazFieldLabel(text = stringResource(R.string.fasting_sheet_status))
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
                NimazTextField(
                    value = fidyaAmount,
                    onValueChange = { fidyaAmount = it },
                    label = stringResource(R.string.fasting_sheet_fidya_amount),
                    variant = NimazFieldVariant.NUMERIC,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
