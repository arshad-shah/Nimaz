package com.arshadshah.nimaz.ui.components.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLocationInput(
    locationName: String,
    onLocationInput: (String) -> Unit,
    isLoading: Boolean
) {
    val showDialog = remember { mutableStateOf(false) }
    //show manual location input
    //onclick open dialog
    SettingsMenuLink(
        title = { Text(text = "Edit Location") },
        subtitle = {
            Text(
                text = locationName,
                modifier = Modifier.placeholder(
                    visible = isLoading,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = Color.White,
                    )

                )
            )
        },
        onClick = {
            showDialog.value = true
        },
        icon = {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.location_marker_edit_icon),
                contentDescription = "Location"
            )
        }
    )

    if (!showDialog.value) return

    val input = remember { mutableStateOf("") }
    AlertDialogNimaz(
        cardContent = false,
        bottomDivider = false,
        topDivider = false,
        contentHeight = 100.dp,
        confirmButtonText = "Submit",
        contentDescription = "Edit Location",
        title = "Edit Location",
        contentToShow = {
            OutlinedTextField(
                shape = MaterialTheme.shapes.extraLarge,
                value = input.value,
                onValueChange = { input.value = it },
                label = { Text(text = "Location") },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        },
        onDismissRequest = {
            showDialog.value = false
        },
        onConfirm = {
            onLocationInput(input.value)
            showDialog.value = false

        },
        onDismiss = {
            showDialog.value = false
        })
}