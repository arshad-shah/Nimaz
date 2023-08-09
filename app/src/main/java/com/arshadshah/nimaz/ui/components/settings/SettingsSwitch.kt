package com.arshadshah.nimaz.ui.components.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.settings.internal.SettingsTileAction
import com.arshadshah.nimaz.ui.components.settings.internal.SettingsTileIcon
import com.arshadshah.nimaz.ui.components.settings.internal.SettingsTileTexts

@Composable
fun SettingsSwitch(
	modifier : Modifier = Modifier ,
	state : SettingValueState<Boolean> = rememberBooleanSettingState() ,
	icon : @Composable (() -> Unit)? = null ,
	title : @Composable () -> Unit ,
	subtitle : @Composable (() -> Unit)? = null ,
	onCheckedChange : (Boolean) -> Unit = {} ,
				  )
{
	var storageValue by state
	val update : (Boolean) -> Unit = { boolean ->
		storageValue = boolean
		onCheckedChange(storageValue)
	}

	val iconForSwitch : (@Composable () -> Unit) = if (state.value)
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
		{
			Icon(
					painter = painterResource(id = R.drawable.cross_icon) ,
					contentDescription = null ,
					modifier = Modifier
						.size(SwitchDefaults.IconSize).padding(2.dp)
				)
		}
	}



	Surface {
		Row(
				modifier = modifier
					.fillMaxWidth()
					.height(60.dp)
					.toggleable(
							value = storageValue ,
							role = Role.Switch ,
							onValueChange = { update(! storageValue) }
							   ) ,
				verticalAlignment = Alignment.CenterVertically ,
		   ) {
			if (icon != null)
			{
				SettingsTileIcon(icon = icon)
			} else
			{
				Spacer(modifier = Modifier.width(24.dp).height(60.dp))
			}
			SettingsTileTexts(title = title , subtitle = subtitle)
			SettingsTileAction {
				Switch(
						checked = storageValue ,
						onCheckedChange = update ,
						thumbContent = iconForSwitch ,
					  )
			}
		}
	}
}

@Preview
@Composable
internal fun SettingsSwitchPreview()
{
		val storage = rememberBooleanSettingState(defaultValue = true)
		SettingsSwitch(
				state = storage ,
				icon = { Icon(imageVector = Icons.Default.Clear , contentDescription = "Clear") } ,
				title = { Text(text = "Hello") } ,
				subtitle = { Text(text = "This is a longer text") } ,
				onCheckedChange = { }
					  )
}

//preview with checked state as false
@Preview
@Composable
internal fun SettingsSwitchPreview2()
{
		val storage = rememberBooleanSettingState(defaultValue = false)
		SettingsSwitch(
				state = storage ,
				icon = { Icon(imageVector = Icons.Default.Clear , contentDescription = "Clear") } ,
				title = { Text(text = "Hello") } ,
				subtitle = { Text(text = "This is a longer text") } ,
				onCheckedChange = { }
					  )
}

//preview of checked state as true in dark theme
@Preview
@Composable
internal fun SettingsSwitchPreview3()
{
		val storage = rememberBooleanSettingState(defaultValue = true)
		SettingsSwitch(
				state = storage ,
				title = { Text(text = "Hello") } ,
				subtitle = { Text(text = "This is a longer text") } ,
				onCheckedChange = { }
					  )
}