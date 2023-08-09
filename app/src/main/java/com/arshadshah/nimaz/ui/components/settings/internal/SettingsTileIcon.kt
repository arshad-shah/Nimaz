package com.arshadshah.nimaz.ui.components.settings.internal

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

@Composable
internal fun SettingsTileIcon(
	modifier : Modifier = Modifier ,
	icon : @Composable (() -> Unit) ,
							 )
{
	//the circle
	Box(

			modifier = modifier
				.size(60.dp) ,
			contentAlignment = Alignment.Center
	   ) {
		icon()
	}
}

@Preview
@Composable
fun SettingsIconPreview()
{

		SettingsTileIcon {
			Icon(imageVector = Icons.Default.Clear , contentDescription = "")
		}
}

@Preview
@Composable
fun SettingsIconPreviewEmpty()
{

		SettingsTileIcon(
				icon = { } ,
						)
}