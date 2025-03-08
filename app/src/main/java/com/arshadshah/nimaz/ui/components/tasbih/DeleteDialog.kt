package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz

@Composable
fun DeleteDialog(
    tasbih: LocalTasbih,
    showDialog: MutableState<Boolean>,
    onDeleteTasbih: (LocalTasbih) -> Unit,
) {
    if (showDialog.value) {
        AlertDialogNimaz(
            bottomDivider = false,
            topDivider = false,
            contentHeight = 100.dp,
            confirmButtonText = "Yes",
            dismissButtonText = "No, Cancel",
            contentDescription = "Delete Tasbih",
            title = "Delete Tasbih",
            contentToShow = {
                Text(
                    text = "Are you sure you want to delete this tasbih?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            },
            onDismissRequest = {
                showDialog.value = false
            },
            onConfirm = {
                onDeleteTasbih(tasbih)
                showDialog.value = false
            },
            onDismiss = {
                showDialog.value = false
            })
    }
}