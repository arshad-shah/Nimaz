package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.ui.components.ui.settings.internal.SettingsTileAction
import com.arshadshah.nimaz.ui.components.ui.settings.internal.SettingsTileIcon
import com.arshadshah.nimaz.ui.components.ui.settings.internal.SettingsTileTexts
import com.arshadshah.nimaz.ui.theme.NimazTheme


@Composable
fun SettingsMenuLink(
	modifier : Modifier = Modifier ,
	icon : (@Composable () -> Unit)? = null ,
	title : @Composable () -> Unit ,
	subtitle : (@Composable () -> Unit)? = null ,
	action : (@Composable () -> Unit)? = null ,
	onClick : () -> Unit ,
					)
{
	Surface {
		Row(
				modifier = modifier.fillMaxWidth() ,
		   ) {
			Row(
					modifier = Modifier
						.weight(1f)
						.clickable(onClick = onClick) ,
					verticalAlignment = Alignment.CenterVertically ,
			   ) {
				SettingsTileIcon(icon = icon)
				SettingsTileTexts(title = title , subtitle = subtitle)
			}
			if (action != null)
			{
				SettingsTileAction {
					action.invoke()
				}
			}
		}
	}
}

@Preview
@Composable
internal fun SettingsMenuLinkPreview()
{
	NimazTheme {
		SettingsMenuLink(
				icon = { Icon(imageVector = Icons.Default.Clear , contentDescription = "Clear") } ,
				title = { Text(text = "Hello") } ,
				subtitle = { Text(text = "This is a longer text") } ,
						) {

		}
	}
}

@Preview
@Composable
internal fun SettingsMenuLinkNoIconPreview()
{
	NimazTheme {
		SettingsMenuLink(
				title = { Text(text = "Hello") } ,
				subtitle = { Text(text = "This is a longer text") } ,
						) {

		}
	}
}

@Preview
@Composable
internal fun SettingsMenuLinkActionPreview()
{
	var rememberCheckBoxState by remember { mutableStateOf(true) }
	NimazTheme {
		SettingsMenuLink(
				icon = { Icon(imageVector = Icons.Default.Clear , contentDescription = "Clear") } ,
				title = { Text(text = "Hello") } ,
				subtitle = { Text(text = "This is a longer text") } ,
				action = {
					Button(
							onClick = { rememberCheckBoxState = ! rememberCheckBoxState } ,
						  ) {
						Text(text = "Action")
					}
				} ,
						) {

		}
	}
}