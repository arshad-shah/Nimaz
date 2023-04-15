package com.arshadshah.nimaz.ui.components.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.RadioButtonCustom
import kotlinx.coroutines.launch


@Composable
fun SettingsList(
	modifier : Modifier = Modifier ,
	state : SettingValueState<Int> = rememberIntSettingState() ,
	valueState : SettingValueState<String> = rememberStringSettingState() ,
	title : String ,
	description : String? = null ,
	items : Map<String , String> ,
	icon : @Composable() (() -> Unit)? = null ,
	iconPainter : Painter? = null ,
	iconDescription : String? = null ,
	useSelectedValueAsSubtitle : Boolean = true ,
	subtitle : String? = null ,
	action : @Composable() (() -> Unit)? = null ,
	height : Dp = 56.dp ,
	onChange : (String) -> Unit = { } ,
				)
{

	if (state.value >= items.size)
	{
		throw IndexOutOfBoundsException("Current value for $title list setting cannot be greater than items size")
	}

	var showDialog by remember { mutableStateOf(false) }

	val safeSubtitle = if (state.value >= 0 && useSelectedValueAsSubtitle)
	{
		val key = valueState.value
		//find the value of the key in the map
		val value : String? = items[key]
		value
	} else subtitle

	SettingsMenuLink(
			modifier = if (icon != null) modifier else modifier.padding(8.dp) ,
			icon = icon ,
			title = {
				Text(text = title)
			} ,
			subtitle = {
				if (safeSubtitle != null)
				{
					Text(text = safeSubtitle)
				}
			} ,
			action = action ,
			onClick = { showDialog = true } ,
					)

	if (! showDialog) return

	val coroutineScope = rememberCoroutineScope()
	val onSelected : (Int) -> Unit = { selectedIndex ->
		coroutineScope.launch {
			state.value = selectedIndex
			valueState.value = items.keys.elementAt(selectedIndex)
			onChange(valueState.value)
		}
	}
	AlertDialogNimaz(
			topDivider = false ,
			bottomDivider = false ,
			description = description ,
			contentDescription = iconDescription ?: "" ,
			icon = iconPainter ,
			title = title ,
			contentHeight = height ,
			contentToShow = {
				items.forEach { (s , s2) ->
					val isSelected by rememberUpdatedState(newValue = s == valueState.value)
					//if valuestate.value has a value, then set the state.value to the index of the valuestate.value
					if (isSelected)
					{
						state.value = items.keys.indexOf(s)
					}
					Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(vertical = 4.dp , horizontal = 8.dp)
								.height(52.dp)
								.selectable(
										role = Role.RadioButton ,
										selected = isSelected ,
										onClick = {
											if (! isSelected) onSelected(
													items.keys.indexOf(
															s
																	  )
																		)
										}
										   ) ,
							verticalAlignment = Alignment.CenterVertically
					   ) {
						RadioButtonCustom(
								selected = isSelected ,
								onClick = {
									if (! isSelected) onSelected(items.keys.indexOf(s))
								} ,
								modifier = Modifier.padding(start = 16.dp) ,
										 )
						Text(
								text = s2 ,
								style = MaterialTheme.typography.titleMedium ,
								modifier = Modifier.padding(start = 16.dp) ,
								overflow = TextOverflow.Ellipsis ,
								maxLines = 1
							)
					}
				}

			} ,
			onDismissRequest = {
				showDialog = false
			} ,
			onConfirm = {
				showDialog = false
			} ,
			onDismiss = {
				showDialog = false
			})
}