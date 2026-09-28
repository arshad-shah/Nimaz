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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R

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
    fullScreen: Boolean = false,
    saving: Boolean = false,
    error: String? = null,
    allowEmpty: Boolean = true,
) {
    // Keyed on the subject: reopening the sheet for a different verse must not carry the
    // previous one's draft across.
    var text by rememberSaveable(subject) { mutableStateOf(initialNote.orEmpty()) }
    var discard by remember { mutableStateOf(false) }
    val dismiss = {
        if (!saving) {
            if (fullScreen && text != initialNote.orEmpty()) discard = true else onDismiss()
        }
    }
    val canSave = !saving && (text.isNotBlank() || (allowEmpty && !initialNote.isNullOrBlank()))
    val editor: @Composable () -> Unit = {
        NimazTextField(
            value = text,
            onValueChange = { if (!saving) text = it },
            label = stringResource(R.string.edit_note),
            variant = NimazFieldVariant.NOTE,
            placeholder = stringResource(R.string.note_hint),
            modifier = Modifier.fillMaxWidth(),
            minLines = if (fullScreen) 8 else 3,
            maxLines = if (fullScreen) Int.MAX_VALUE else 8,
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
    }
    if (fullScreen) {
        Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BackHandler(onBack = dismiss)
            NimazScreenScaffold(
                modifier = Modifier.fillMaxSize().imePadding(),
                topBar = {
                    NimazBackTopAppBar(
                        title = stringResource(R.string.edit_note),
                        subtitle = subject,
                        onBackClick = dismiss,
                        actions = {
                            NimazButton(
                                text = stringResource(R.string.save),
                                onClick = { onSave(text.trim().takeIf(String::isNotEmpty)) },
                                enabled = canSave,
                                variant = NimazButtonVariant.TEXT,
                            )
                        },
                    )
                },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) { editor() }
            }
        }
    } else {
        NimazBottomSheet(
            onDismissRequest = dismiss,
            modifier = modifier,
            title = stringResource(R.string.edit_note),
            subtitle = subject,
            icon = Icons.Default.Edit,
            onClose = dismiss,
            footer = {
                NimazSheetFooterButtons(
                    primaryText = stringResource(R.string.save),
                    onPrimary = { onSave(text.trim().takeIf(String::isNotEmpty)) },
                    primaryEnabled = canSave,
                    secondaryText = stringResource(R.string.cancel),
                    onSecondary = dismiss,
                )
            }
        ) {
            editor()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    if (discard) NimazConfirmDialog(
        title = stringResource(R.string.note_discard_title),
        message = stringResource(R.string.note_discard_message),
        confirmText = stringResource(R.string.yes),
        cancelText = stringResource(R.string.cancel),
        onConfirm = { discard = false; onDismiss() },
        onDismiss = { discard = false },
    )
}
