package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun SettingsSelectionDialog(
    title: String,
    options: Map<String, String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    showDialog: Boolean
) {
    // Temporary state to hold the selected option until confirmed
    var tempSelectedOption by remember(selectedOption) { mutableStateOf(selectedOption) }

    if (showDialog) {
        AlertDialogNimaz(
            title = title,
            onDismiss = onDismiss,
            onConfirm = {
                // Only apply the selection when confirmed
                onOptionSelected(tempSelectedOption)
                onDismiss()
            },
            contentDescription = "Select your preferred option",
            onDismissRequest = onDismiss,
            confirmButtonText = "Apply",
            contentToShow = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options.toList()) { (key, description) ->
                        val isSelected = key == tempSelectedOption
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = if (isSelected) 4.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .selectable(
                                        role = Role.RadioButton,
                                        selected = isSelected,
                                        onClick = { tempSelectedOption = key }
                                    )
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RadioButtonCustom(
                                    selected = isSelected,
                                    onClick = { tempSelectedOption = key }
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.8f
                                        )
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}