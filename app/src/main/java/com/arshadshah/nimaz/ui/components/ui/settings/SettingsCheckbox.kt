package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.getValue
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberBooleanSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.setValue
import com.arshadshah.nimaz.ui.components.ui.settings.internal.SettingsTileAction
import com.arshadshah.nimaz.ui.components.ui.settings.internal.SettingsTileIcon
import com.arshadshah.nimaz.ui.components.ui.settings.internal.SettingsTileTexts
import com.arshadshah.nimaz.ui.theme.NimazTheme


@Composable
fun SettingsCheckbox(
    modifier: Modifier = Modifier,
    state: SettingValueState<Boolean> = rememberBooleanSettingState(),
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    var storageValue by state
    val update: (Boolean) -> Unit = { boolean ->
        storageValue = boolean
        onCheckedChange(storageValue)
    }
    Surface {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .toggleable(
                    value = storageValue,
                    role = Role.Checkbox,
                    onValueChange = { update(!storageValue) }
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsTileIcon(icon = icon)
            SettingsTileTexts(title = title, subtitle = subtitle)
            SettingsTileAction {
                Checkbox(
                    checked = storageValue,
                    onCheckedChange = update
                )
            }
        }
    }
}

@Preview
@Composable
internal fun SettingsCheckboxPreview() {
    NimazTheme {
        val storage = rememberBooleanSettingState(defaultValue = true)
        SettingsCheckbox(
            state = storage,
            icon = { Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear") },
            title = { Text(text = "Hello") },
            subtitle = { Text(text = "This is a longer text") },
            onCheckedChange = { }
        )
    }
}