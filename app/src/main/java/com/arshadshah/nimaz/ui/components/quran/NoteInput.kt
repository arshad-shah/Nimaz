package com.arshadshah.nimaz.ui.components.quran

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import es.dmoral.toasty.Toasty

@Composable
fun NoteInput(
    showNoteDialog: MutableState<Boolean>,
    onClick: () -> Unit,
    noteContent: MutableState<String>,
    titleOfDialog: String,
) {
    val context = LocalContext.current
    val maxLength = 150

    AlertDialogNimaz(
        contentDescription = "Note",
        title = titleOfDialog,
        topDivider = false,
        bottomDivider = false,
        contentHeight = 200.dp,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        action = {
            DeleteNoteButton(
                onClick = {
                    noteContent.value = ""
                    onClick()
                }
            )
        },
        contentToShow = {
            NoteTextField(
                value = noteContent.value,
                maxLength = maxLength,
                onValueChange = { newValue ->
                    if (newValue.length <= maxLength) {
                        noteContent.value = newValue
                    }
                },
                onClear = { noteContent.value = "" }
            )
        },
        onDismissRequest = {
            showNoteDialog.value = false
            onClick()
        },
        confirmButtonText = "Save",
        onConfirm = {
            if (noteContent.value.isEmpty()) {
                showEmptyNoteWarning(context)
            } else {
                onClick()
            }
        },
        dismissButtonText = "Cancel",
        onDismiss = {
            showNoteDialog.value = false
            onClick()
        }
    )
}

@Composable
private fun DeleteNoteButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(8.dp)
            .size(24.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.delete_icon),
            contentDescription = "Delete note",
            tint = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun NoteTextField(
    value: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = false,
        label = { Text("Add a Note") },
        isError = value.length > maxLength,
        supportingText = {
            CharacterCounter(
                current = value.length,
                max = maxLength
            )
        },
        trailingIcon = {
            ClearButton(
                visible = value.isNotEmpty(),
                onClick = onClear
            )
        },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            errorBorderColor = MaterialTheme.colorScheme.error,
        )
    )
}

@Composable
private fun CharacterCounter(
    current: Int,
    max: Int
) {
    Text(
        text = "$current/$max",
        style = MaterialTheme.typography.bodySmall,
        color = if (current > max) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

@Composable
private fun ClearButton(
    visible: Boolean,
    onClick: () -> Unit
) {
    if (visible) {
        Icon(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            painter = painterResource(id = R.drawable.cross_icon),
            contentDescription = "Clear note",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

private fun showEmptyNoteWarning(context: Context) {
    Toasty.warning(
        context,
        "Note is empty. Closing without save.",
        Toasty.LENGTH_SHORT
    ).show()
}