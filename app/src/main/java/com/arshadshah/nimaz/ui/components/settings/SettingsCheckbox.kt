package com.arshadshah.nimaz.ui.components.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.settings.internal.SettingsTileAction
import com.arshadshah.nimaz.ui.components.settings.internal.SettingsTileIcon
import com.arshadshah.nimaz.ui.components.settings.internal.SettingsTileTexts


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
            if (icon != null) {
                SettingsTileIcon(icon = icon)
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
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
    val storage = rememberBooleanSettingState(defaultValue = true)
    SettingsCheckbox(
        state = storage,
        icon = { Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear") },
        title = { Text(text = "Hello") },
        subtitle = { Text(text = "This is a longer text") },
        onCheckedChange = { }
    )
}