package com.arshadshah.nimaz.ui.components.settings

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.theme.NimazTheme
import kotlinx.coroutines.launch


@Composable
fun SettingsNumberPickerDialog(
    modifier: Modifier = Modifier,
    state: SettingValueState<Int> = rememberIntSettingState(),
    title: String,
    description: String? = null,
    iconDescription: String? = null,
    iconPainter: Painter? = null,
    items: List<Int>,
    icon: @Composable (() -> Unit)? = null,
    useSelectedValueAsSubtitle: Boolean = true,
    subtitle: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    valueState: SettingValueState<String> = rememberStringSettingState(),
    onChange: (Int) -> Unit = { },
    height: Dp,
) {

    if (state.value >= items.size) {
        throw IndexOutOfBoundsException("Current value for $title list setting cannot be greater than items size")
    }

    var showDialog by remember { mutableStateOf(false) }

    val safeSubtitle = if (state.value >= 0 && useSelectedValueAsSubtitle) {
        {
            Text(
                text = if (items.size == 51)
                    if (valueState.value.toInt() < 2 && valueState.value.toInt() > -1) "${valueState.value} Degree " else if (valueState.value.toInt() != -1) "${valueState.value} Degrees" else "${valueState.value} Degree"
                else
                    if (valueState.value.toInt() < 2 && valueState.value.toInt() > -1) "${valueState.value} Minute " else if (valueState.value.toInt() != -1) "${valueState.value} Minutes" else "${valueState.value} Minute"
            )
        }
    } else subtitle

    SettingsMenuLink(
        icon = icon,
        title = {
            Text(
                text = title
            )
        },
        subtitle = safeSubtitle,
        action = action,
        onClick = { showDialog = true },
    )

    if (!showDialog) return

    val coroutineScope = rememberCoroutineScope()
    val onSelected: (Int) -> Unit = { selectedIndex ->
        coroutineScope.launch {
            Log.d("Nimaz: SettingsNumberPickerDialog", "onSelected: $selectedIndex")
            val index = items.indexOf(selectedIndex)
            state.value = index
            valueState.value = selectedIndex.toString()
            onChange(selectedIndex)
        }
    }
    AlertDialogNimaz(
        topDivider = false,
        bottomDivider = false,
        contentDescription = title,
        description = description,
        contentHeight = height,
        title = title,
        contentToShow = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Picker(
                    modifier = modifier,
                    value = if (items.size == 51) valueState.value.toDouble()
                        .toInt() else valueState.value.toInt(),
                    onValueChange = onSelected,
                    list = items,
                    dividersColor = MaterialTheme.colorScheme.outline,
                    textStyle = MaterialTheme.typography.titleLarge,
                    label = {
                        if (items.size == 51)
                            if (it < 2 && it > -1) "$it Degree " else if (it != -1) "$it Degrees" else "$it Degree"
                        else
                            if (it < 2 && it > -1) "$it Minute " else if (it != -1) "$it Minutes" else "$it Minute"
                    },
                )
            }
        },
        onDismissRequest = {
            showDialog = false
        },
        onConfirm = {
            showDialog = false
        },
        onDismiss = {
            showDialog = false
        }
    )
}


//preview
@Preview
@Composable
fun SettingsNumberPickerDialogPreview() {
    val storage = rememberPreferenceStringSettingState(
        key = "test",
        defaultValue = "0"
    )
    NimazTheme {
        SettingsNumberPickerDialog(
            title = "Title",
            description = "Description",
            items = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
            icon = { Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear") },
            subtitle = { Text(text = "Subtitle") },
            valueState = storage,
            height = 100.dp,
        )
    }
}

@Preview
@Composable
fun SettingsNumberPickerDialogNoIconPreview() {
    val storage = rememberPreferenceStringSettingState(
        key = "test",
        defaultValue = "0"
    )
    NimazTheme {
        SettingsNumberPickerDialog(
            title = "Title",
            description = "Description",
            items = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
            subtitle = { Text(text = "Subtitle") },
            valueState = storage,
            height = 100.dp,
        )
    }
}