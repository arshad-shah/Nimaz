package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

/**
 * Write a note against something you saved.
 *
 * One sheet for both places a note is written — the Saved screen's bookmark menu and the
 * reader's ayah sheet — because they are the same editor and had no business being two.
 *
 * @param subject what the note is about, shown as the sheet's subtitle so a reader who opened
 *   it from a list of near-identical rows can check they tapped the right one.
 * @param onSave receives the trimmed text, or `null` when the field was cleared — an empty note
 *   is not a note, and storing `""` leaves a bookmark advertising an annotation it does not have.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(
    subject: String,
    initialNote: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keyed on the subject: reopening the sheet for a different verse must not carry the
    // previous one's draft across.
    var text by remember(subject) { mutableStateOf(initialNote.orEmpty()) }
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = stringResource(R.string.edit_note),
        subtitle = subject,
        icon = Icons.Default.Edit,
        onClose = onDismiss,
        footer = {
            NimazSheetFooterButtons(
                primaryText = stringResource(R.string.save),
                onPrimary = { onSave(text.trim().takeIf { it.isNotEmpty() }) },
                // The one place a field's state reaches outside itself: an empty note is not a
                // note, so Save has nothing to do until there is something in the field.
                primaryEnabled = text.isNotBlank() || !initialNote.isNullOrBlank(),
                secondaryText = stringResource(R.string.cancel),
                onSecondary = onDismiss,
            )
        }
    ) {
        NimazTextField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(R.string.edit_note),
            variant = NimazFieldVariant.NOTE,
            placeholder = stringResource(R.string.note_hint),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
