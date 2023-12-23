package com.arshadshah.nimaz.ui.components.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_VIEWMODEL_KEY
import com.arshadshah.nimaz.data.remote.models.Tasbih
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.viewModel.TasbihViewModel

@Composable
fun DeleteDialog(
    tasbih: Tasbih,
    showDialog: MutableState<Boolean>,
) {
    val context = LocalContext.current
    val viewModel = viewModel(
        key = TASBIH_VIEWMODEL_KEY,
        initializer = { TasbihViewModel(context) },
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
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
                viewModel.handleEvent(
                    TasbihViewModel.TasbihEvent.DeleteTasbih(
                        tasbih
                    )
                )
                showDialog.value = false
            },
            onDismiss = {
                showDialog.value = false
            })
    }
}