package com.arshadshah.nimaz.ui.components.ui.settings.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

@Composable
internal fun SettingsTileAction(content : @Composable () -> Unit)
{
	Box(
			modifier = Modifier
				.height(64.dp)
				.padding(horizontal = 8.dp) ,
			contentAlignment = Alignment.Center ,
	   ) {
		content()
	}
}

@Preview
@Composable
internal fun SettingsTileActionPreview()
{
	SettingsTileAction {
		Button(
				onClick = {
				} ,
			  ) {
			Text(text = "Update")
		}
	}
}

//switch insettings
@Preview
@Composable
internal fun SettingsTileActionPreview2()
{
	val update : (Boolean) -> Unit = { boolean ->
	}

	val iconForSwitch : (@Composable () -> Unit)? = if (true)
	{
		{
			Icon(
					painter = painterResource(id = R.drawable.check_icon) ,
					contentDescription = null ,
					modifier = Modifier
						.size(SwitchDefaults.IconSize)
				)
		}
	} else
	{
		null
	}
	SettingsTileAction {
		Switch(
				checked = true ,
				onCheckedChange = update ,
				thumbContent = iconForSwitch ,
			  )
	}
}