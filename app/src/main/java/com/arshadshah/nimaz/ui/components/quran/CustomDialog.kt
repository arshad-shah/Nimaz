package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.RadioButtonCustom
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.settings.state.StringPreferenceSettingValueState
import kotlinx.coroutines.launch

@Composable
fun CustomDialog(
    title: String,
    items: List<String>,
    setShowDialog: (Boolean) -> Unit,
    state: SettingValueState<Int>,
    valueState: StringPreferenceSettingValueState,
    onStateChange: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val onSelected: (Int) -> Unit = { selectedIndex ->
        coroutineScope.launch {
            state.value = selectedIndex
            valueState.value = items[selectedIndex]
            onStateChange(items[selectedIndex])
        }
    }

    AlertDialogNimaz(
        topDivider = false,
        bottomDivider = false,
        dismissButtonText = "Close",
        contentHeight = 200.dp,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        contentDescription = title,
        title = title,
        contentToShow = {
            items.forEachIndexed { index, item ->
                val isSelected by rememberUpdatedState(newValue = state.value == index)
                //if valuestate.value has a value, then set the state.value to the index of the valuestate.value
                if (valueState.value.isNotEmpty() && valueState.value == item) {
                    state.value = index
                }

                RadioListItem(
                    item = item,
                    isSelected = isSelected,
                    index = index,
                    onSelected = onSelected
                )
            }
        },
        onDismissRequest = {
            setShowDialog(false)
        },
        onConfirm = {
            setShowDialog(false)
        },
        onDismiss = {
            setShowDialog(false)
        })
}


@Composable
private fun RadioListItem(
    item: String,
    isSelected: Boolean,
    index: Int,
    onSelected: (Int) -> Unit
) {
    val transition = updateTransition(isSelected, label = "selection")
    val scale by transition.animateFloat { selected ->
        if (selected) 1.02f else 1f
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .scale(scale),
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
                    onClick = { if (!isSelected) onSelected(index) }
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RadioButtonCustom(
                selected = isSelected,
                onClick = { if (!isSelected) onSelected(index) },
            )

            Text(
                text = item,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}