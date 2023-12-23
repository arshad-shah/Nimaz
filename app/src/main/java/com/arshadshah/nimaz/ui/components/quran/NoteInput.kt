package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteInput(
    showNoteDialog: MutableState<Boolean>,
    onClick: () -> Unit,
    noteContent: MutableState<String>,
) {
    val context = LocalContext.current
    AlertDialogNimaz(
        cardContent = false,
        contentDescription = "Note",
        title = "Add Note",
        topDivider = false,
        bottomDivider = false,
        contentHeight = 350.dp,
        action = {
            IconButton(
                onClick = {
                    //remove note
                    noteContent.value = ""
                    onClick()
                },
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.delete_icon),
                    contentDescription = "delete note",
                )
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        contentToShow = {
            OutlinedTextField(
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = false,
                shape = MaterialTheme.shapes.extraLarge,
                value = noteContent.value,
                onValueChange = { noteContent.value = it },
                label = { Text(text = "Note") },
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .height(300.dp),
            )
        },
        onDismissRequest = {
            showNoteDialog.value = false
            onClick()
        },
        confirmButtonText = "Save",
        onConfirm = {
            if (noteContent.value.isEmpty()) {
                //show toast message saying note is empty
                Toasty.warning(
                    context,
                    "Note is empty. closing without save.",
                    Toasty.LENGTH_SHORT
                ).show()
                showNoteDialog.value = false
            } else {
                onClick()
            }
        },
        dismissButtonText = "Cancel",
        onDismiss = {
            showNoteDialog.value = false
            onClick()
        })


}