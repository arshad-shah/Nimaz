package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberIntSetSettingState

@Composable
fun SettingsListMultiSelect(
    modifier: Modifier = Modifier,
    state: SettingValueState<Set<Int>> = rememberIntSetSettingState(),
    title: @Composable () -> Unit,
    items: List<String>,
    icon: @Composable (() -> Unit)? = null,
    confirmButton: String,
    useSelectedValuesAsSubtitle: Boolean = true,
    subtitle: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
) {

    if (state.value.any { index -> index >= items.size }) {
        throw IndexOutOfBoundsException("Current indexes for $title list setting cannot be grater than items size")
    }

    var showDialog by remember { mutableStateOf(false) }

    val safeSubtitle = if (state.value.size >= 0 && useSelectedValuesAsSubtitle) {
        {
            Text(text = state.value.map { index -> items[index] }
                .joinToString(separator = ", ") { it })
        }
    } else subtitle

    SettingsMenuLink(
        modifier = modifier,
        icon = icon,
        title = title,
        subtitle = safeSubtitle,
        action = action,
        onClick = { showDialog = true },
    )

    if (!showDialog) return

    val onAdd: (Int) -> Unit = { selectedIndex ->
        val mutable = state.value.toMutableSet()
        mutable.add(selectedIndex)
        state.value = mutable
    }
    val onRemove: (Int) -> Unit = { selectedIndex ->
        val mutable = state.value.toMutableSet()
        mutable.remove(selectedIndex)
        state.value = mutable
    }

    AlertDialog(
        title = title,
        text = {
            Column {
                if (subtitle != null) {
                    subtitle()
                    Spacer(modifier = Modifier.size(8.dp))
                }

                items.forEachIndexed { index, item ->
                    val isSelected by rememberUpdatedState(newValue = state.value.contains(index))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .toggleable(
                                role = Role.Checkbox,
                                value = isSelected,
                                onValueChange = {
                                    if (isSelected) {
                                        onRemove(index)
                                    } else {
                                        onAdd(index)
                                    }
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null
                        )
                    }
                }
            }
        },
        onDismissRequest = { showDialog = false },
        confirmButton = {
            TextButton(
                onClick = { showDialog = false },
            ) {
                Text(text = confirmButton)
            }
        }
    )
}