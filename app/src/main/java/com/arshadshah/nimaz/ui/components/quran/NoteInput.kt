package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.NimazTextField
import com.arshadshah.nimaz.ui.components.common.NimazTextFieldType
import com.arshadshah.nimaz.viewModel.AyatViewModel

@Composable
fun NoteDialog(
    aya: LocalAya,
    initialNote: String,
    onDismiss: () -> Unit,
    onEvent: (AyatViewModel.AyatEvent) -> Unit,
) {
    val text = remember { mutableStateOf(initialNote) }
    AlertDialogNimaz(
        title = if (initialNote.isEmpty()) "Add Note" else "Edit Note",
        contentHeight = 250.dp,
        action = {
            DeleteNoteButton(
                visible = initialNote.isNotEmpty(),
                onClick = {
                    onEvent(AyatViewModel.AyatEvent.UpdateNote(aya, ""))
                    onDismiss()
                }
            )
        },
        contentDescription = if (initialNote.isEmpty()) "Add Note" else "Edit Note",
        cardContent = false,
        contentToShow = {
            NimazTextField(
                value = text.value,
                onValueChange = { text.value = it },
                type = NimazTextFieldType.MULTILINE,
                placeholder = "Enter your note...",
                maxLength = 250,
                minLines = 3,
                maxLines = 8,
                requestFocus = true
            )
        },
        onDismissRequest = onDismiss,
        dismissButtonText = if (initialNote.isEmpty()) "Cancel" else "Close",
        showConfirmButton = if (initialNote.isEmpty()) text.value.isNotEmpty() else text.value != initialNote,
        confirmButtonText = if (initialNote.isEmpty()) "Save" else "Update",
        onConfirm = {
            onEvent(
                AyatViewModel.AyatEvent.UpdateNote(
                    aya = aya,
                    note = text.value
                )
            )
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun DeleteNoteButton(
    visible: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.delete_icon),
                contentDescription = "Delete note",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
