package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberIntSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberStringSettingState
import com.arshadshah.nimaz.ui.theme.NimazTheme
import kotlinx.coroutines.launch


@Composable
fun SettingsList(
    modifier: Modifier = Modifier,
    state: SettingValueState<Int> = rememberIntSettingState(),
    valueState: SettingValueState<String> = rememberStringSettingState(),
    title: @Composable () -> Unit,
    items: List<String>,
    icon: (@Composable () -> Unit)? = null,
    useSelectedValueAsSubtitle: Boolean = true,
    subtitle: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {

    if (state.value >= items.size) {
        throw IndexOutOfBoundsException("Current value for $title list setting cannot be greater than items size")
    }

    var showDialog by remember { mutableStateOf(false) }

    val safeSubtitle = if (state.value >= 0 && useSelectedValueAsSubtitle) {
        { Text(text = items[state.value]) }
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

    val coroutineScope = rememberCoroutineScope()
    val onSelected: (Int) -> Unit = { selectedIndex ->
        coroutineScope.launch {
            state.value = selectedIndex
            valueState.value = items[selectedIndex]
        }
    }

    AlertDialog(
        shape = CardDefaults.elevatedShape,
        title = title,
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected by rememberUpdatedState(newValue = state.value == index)
                    //if valuestate.value has a value, then set the state.value to the index of the valuestate.value
                    if (valueState.value.isNotEmpty() && valueState.value == item) {
                        state.value = index
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                role = Role.RadioButton,
                                selected = isSelected,
                                onClick = { if (!isSelected) onSelected(index) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { if (!isSelected) onSelected(index) }
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        onDismissRequest = { showDialog = false },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
        confirmButton = {
            Button(
                onClick = { showDialog = false },
                content = { Text(text = "Confirm") }
            )
        },
        dismissButton = {
            TextButton(
                onClick = { showDialog = false },
                content = { Text(text = "Cancel") }
            )
        }
    )
}

@Preview
@Composable
internal fun ListLinkPreview() {
    NimazTheme {
        SettingsList(
            items = listOf(
                "Banana",
                "Kiwi",
                "Pineapple",
                "Strawberry",
                "Watermelon",
                "Apple",
                "Orange",
                "Mango"
            ),
            icon = { Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear") },
            title = { Text(text = "Hello") },
            subtitle = { Text(text = "This is a longer text") },
        )
    }
}