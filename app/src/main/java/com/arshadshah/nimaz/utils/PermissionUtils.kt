package com.arshadshah.nimaz.utils

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

//feature that requires notification permission
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FeatureThatRequiresNotificationPermission(
    notificationPermissionState: PermissionState,
    isChecked: MutableState<Boolean>,
) {

    val descToShow = remember { mutableStateOf("") }
    val showRationale = remember { mutableStateOf(false) }
    //check if notification permission is granted
    if (notificationPermissionState.status.isGranted) {
        isChecked.value = true
    } else {
        //rationale
        if (notificationPermissionState.status.shouldShowRationale) {
            showRationale.value = true
            descToShow.value =
                "Notification permission is required to deliver adhan notifications. Please allow notification permission."
        } else {
            showRationale.value = true
            descToShow.value =
                "Notification permission is required to deliver adhan notifications, Nimaz will not be able to show adhan notifications if it is denied."
        }
    }

    if (showRationale.value) {
        //permission not granted
        //show dialog
        AlertDialog(
            onDismissRequest = {
                showRationale.value = false
            },
            title = { Text(text = "Notification Permission Required") },
            text = { Text(text = descToShow.value) },
            confirmButton = {
                Button(onClick = { notificationPermissionState.launchPermissionRequest() }) {
                    Text(text = "Allow", style = MaterialTheme.typography.titleMedium)
                }
            },
            dismissButton = {
                Button(onClick = { showRationale.value = false }) {
                    Text(text = "Cancel", style = MaterialTheme.typography.titleMedium)
                }
            }
        )
    }

}