package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberIntSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.internal.SettingsTileIcon
import com.arshadshah.nimaz.ui.components.ui.settings.internal.SettingsTileTexts
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun SettingsListDropdown(
	modifier : Modifier = Modifier ,
	state : SettingValueState<Int> = rememberIntSettingState() ,
	title : @Composable () -> Unit ,
	items : List<String> ,
	icon : (@Composable () -> Unit)? = null ,
	subtitle : (@Composable () -> Unit)? = null ,
	menuItem : (@Composable (index : Int , text : String) -> Unit)? = null ,
						)
{
	if (state.value >= items.size)
	{
		throw IndexOutOfBoundsException("Current value of state for list setting cannot be greater than items size")
	}

	Surface {
		Row(
				modifier = modifier.fillMaxWidth() ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			if (icon != null)
			{
				SettingsTileIcon(icon = icon)
			}else{
				Spacer(modifier = Modifier.size(24.dp))
			}
			SettingsTileTexts(
					title = title ,
					subtitle = subtitle
							 )

			var isDropdownExpanded by remember {
				mutableStateOf(false)
			}

			Column(
					modifier = Modifier.padding(end = 8.dp)
				  ) {
				Row(
						modifier = Modifier
							.clickable { isDropdownExpanded = true }
							.padding(vertical = 5.dp) ,
						verticalAlignment = Alignment.CenterVertically
				   ) {
					Text(text = items[state.value])
					Icon(
							modifier = Modifier.padding(start = 8.dp) ,
							imageVector = Icons.Outlined.ArrowDropDown ,
							contentDescription = null
						)
				}

				DropdownMenu(
						expanded = isDropdownExpanded ,
						onDismissRequest = { isDropdownExpanded = false }
							) {
					items.forEachIndexed { index , text ->
						DropdownMenuItem(
								text = {
									if (menuItem != null)
									{
										menuItem(index , text)
									} else
									{
										Text(text = text)
									}
								} ,
								onClick = {
									state.value = index
									isDropdownExpanded = false
								}
										)
					}
				}
			}
		}
	}
}

@Preview
@Composable
internal fun SettingsListDropdownPreview()
{
	NimazTheme {
		SettingsListDropdown(
				title = { Text(text = "Title") } ,
				items = listOf("Item 1" , "Item 2" , "Item 3") ,
				icon = {
					Icon(
							imageVector = Icons.Outlined.ArrowDropDown ,
							contentDescription = null
						)
				} ,
				subtitle = { Text(text = "Subtitle") }
							)
	}
}