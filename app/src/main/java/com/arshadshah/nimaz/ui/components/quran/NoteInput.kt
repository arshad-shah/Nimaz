package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    titleOfDialog: MutableState<String>,
) {
    val context = LocalContext.current
    AlertDialogNimaz(
        contentDescription = "Note",
        title = titleOfDialog.value,
        topDivider = false,
        bottomDivider = false,
        contentHeight = 200.dp,
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
                value = noteContent.value,
                onValueChange = {
                    if (it.length <= 150) {
                        noteContent.value = it
                    }
                },
                label = { Text(text = "Add a Note") },
                isError = noteContent.value.length > 150,
                supportingText = {
                    Text(
                        text = "${noteContent.value.length}/150",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                trailingIcon = {
                    if (noteContent.value.isNotEmpty()) {
                        Icon(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable {
                                    noteContent.value = ""
                                },
                            painter = painterResource(id = R.drawable.cross_icon),
                            contentDescription = "Clear note",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                },
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