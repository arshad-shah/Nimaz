package com.arshadshah.nimaz.ui.components.ui.settings.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
internal fun SettingsTileIcon(
	modifier : Modifier = Modifier ,
	icon : @Composable (() -> Unit) ,
							 )
{
	Box(
			modifier = modifier.size(64.dp) ,
			contentAlignment = Alignment.Center ,
	   ) {
		icon()
	}
}

@Preview
@Composable
fun SettingsIconPreview()
{
	NimazTheme {
		SettingsTileIcon {
			Icon(imageVector = Icons.Default.Clear , contentDescription = "")
		}
	}
}

@Preview
@Composable
fun SettingsIconPreviewEmpty()
{
	NimazTheme {
		SettingsTileIcon(
				icon = { } ,
						)
	}
}