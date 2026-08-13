package com.arshadshah.nimaz.presentation.screens.fasting

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.formatWeekdayDayMonth
import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazFilterChip
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import java.time.LocalDate

/**
 * Why a day was exempt.
 *
 * One of the two small sheets that replaced `FastManagementBottomSheet`. That sheet asked four
 * questions at once — status, fast type, reason, note — of which the status is now answered by
 * the day card's control and the type is inferred. What is left is the one question a tap on
 * "Exempt" cannot answer by itself.
 *
 * @param initialReason the reason already on the day, so reopening the sheet shows the current
 *   answer rather than a blank form.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FastExemptionSheet(
    isVisible: Boolean,
    date: LocalDate,
    initialReason: ExemptionReason?,
    onSave: (ExemptionReason) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    var selected by remember(date, initialReason) { mutableStateOf(initialReason) }

    NimazBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.fasting_why_exempt),
        subtitle = date.formatWeekdayDayMonth(),
        footer = {
            SheetActions(
                onCancel = onDismiss,
                onSave = {
                    // A reason is required to record an exemption, but making the user pick one
                    // to dismiss a sheet they opened by accident is worse than defaulting: OTHER
                    // is honest about what an unanswered "why" amounts to.
                    onSave(selected ?: ExemptionReason.OTHER)
                },
            )
        },
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ExemptionReason.entries.forEach { reason ->
                NimazFilterChip(
                    selected = selected == reason,
                    onClick = { selected = reason },
                    label = reason.displayName(),
                    // Pills, not the default 8dp rounded rectangles. Seven reasons wrapping over
                    // three rows read as a paragraph of boxes at that radius; fully-round chips
                    // read as a set of choices, which is what they are.
                    shape = RoundedCornerShape(percent = 50),
                    // The tick is redundant next to a filled pill and costs the label its width,
                    // which is what pushed "Menstruation" and "Breastfeeding" onto lines of
                    // their own.
                    showSelectedIcon = false,
                )
            }
        }
    }
}

/**
 * A free-text note against a day.
 *
 * Separate from the exemption sheet rather than a field inside it: a note belongs to any day, not
 * only an exempt one, and the day card offers it independently of the status control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastNoteSheet(
    isVisible: Boolean,
    date: LocalDate,
    initialNote: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    var note by remember(date, initialNote) { mutableStateOf(initialNote) }

    NimazBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.fasting_note_title),
        subtitle = date.formatWeekdayDayMonth(),
        footer = {
            SheetActions(onCancel = onDismiss, onSave = { onSave(note) })
        },
    ) {
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            placeholder = { Text(stringResource(R.string.fasting_note_hint)) },
            minLines = 3,
        )
    }
}

/** Cancel / Save, shared so the two sheets cannot drift apart. */
@Composable
private fun SheetActions(
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Equal weight, and Cancel is outlined rather than bare text: two buttons of visibly
        // different weight read as one button beside a link.
        //
        // OUTLINED and not TONAL — this theme's `primaryContainer` is a soft amber, so a tonal
        // Cancel came out looking like a warning sitting next to Save. Neutral is the whole job
        // of this button.
        NimazButton(
            text = stringResource(R.string.cancel),
            onClick = onCancel,
            variant = NimazButtonVariant.OUTLINED,
            modifier = Modifier.weight(1f),
        )
        NimazButton(
            text = stringResource(R.string.save),
            onClick = onSave,
            modifier = Modifier.weight(1f),
        )
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun SheetActionsShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.fasting_why_exempt))
        FlowRowShowcase()
        Spacer(modifier = Modifier.height(8.dp))
        SheetActions(onCancel = {}, onSave = {})
    }
}

/**
 * The reason chips on their own.
 *
 * A `ModalBottomSheet` does not render in the preview pane, so the sheet's *contents* are
 * previewed here instead — otherwise the only way to see this design is to run the app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowShowcase() {
    var selected by remember { mutableStateOf(ExemptionReason.TRAVEL) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ExemptionReason.entries.forEach { reason ->
            NimazFilterChip(
                selected = selected == reason,
                onClick = { selected = reason },
                label = reason.displayName(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Fast day sheets — Light")
@Composable
private fun FastDaySheetsLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { SheetActionsShowcase() }
}

@Preview(
    showBackground = true, widthDp = 400, name = "Fast day sheets — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun FastDaySheetsDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { SheetActionsShowcase() }
}
