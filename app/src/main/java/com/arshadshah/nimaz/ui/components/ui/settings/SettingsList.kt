package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberIntSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberStringSettingState
import com.arshadshah.nimaz.ui.theme.NimazTheme
import kotlinx.coroutines.launch


@Composable
fun SettingsList(
	modifier : Modifier = Modifier ,
	state : SettingValueState<Int> = rememberIntSettingState() ,
	valueState : SettingValueState<String> = rememberStringSettingState() ,
	title : @Composable () -> Unit ,
	description : (@Composable () -> Unit)? = null ,
	items : Map<String , String> ,
	icon : (@Composable () -> Unit)? = null ,
	useSelectedValueAsSubtitle : Boolean = true ,
	subtitle : (@Composable () -> Unit)? = null ,
	action : (@Composable () -> Unit)? = null ,
	height : Dp = 56.dp ,
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
		{ Text(text = value !!) }
	} else subtitle

	SettingsMenuLink(
			modifier = modifier ,
			icon = icon ,
			title = title ,
			subtitle = safeSubtitle ,
			action = action ,
			onClick = { showDialog = true } ,
					)

	if (! showDialog) return

	val coroutineScope = rememberCoroutineScope()
	val onSelected : (Int) -> Unit = { selectedIndex ->
		coroutineScope.launch {
			state.value = selectedIndex
			valueState.value = items.keys.elementAt(selectedIndex)
		}
	}
	AlertDialog(
			shape = CardDefaults.elevatedShape ,
			title = title ,
			text = {
				Column {
					Row(modifier.padding(bottom = 8.dp)) {
						description?.invoke()
					}
					Divider(color = MaterialTheme.colorScheme.outline)
					Row(modifier.height(300.dp)) {
						Column(
								modifier = Modifier
									.verticalScroll(rememberScrollState())
									.fillMaxWidth()
									.fillMaxHeight()
							  ) {
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
											.height(48.dp)
											.selectable(
													role = Role.RadioButton ,
													selected = isSelected ,
													onClick = {
														if (! isSelected) onSelected(items.keys.indexOf(
																s))
													}
													   ) ,
										verticalAlignment = Alignment.CenterVertically
								   ) {
									RadioButton(
											selected = isSelected ,
											onClick = {
												if (! isSelected) onSelected(items.keys.indexOf(s))
											} ,
											   )
									Text(
											text = s2 ,
											style = MaterialTheme.typography.titleMedium ,
											modifier = Modifier.padding(start = 8.dp) ,
										)
								}
							}
						}
					}
					Divider(color = MaterialTheme.colorScheme.outline)
				}
			} ,
			onDismissRequest = { showDialog = false } ,
			properties = DialogProperties(
					dismissOnBackPress = true ,
					dismissOnClickOutside = true ,
										 ) ,
			confirmButton = {
				Button(
						onClick = { showDialog = false } ,
						content = { Text(text = "Confirm") }
					  )
			} ,
			dismissButton = {
				TextButton(
						onClick = { showDialog = false } ,
						content = { Text(text = "Cancel") }
						  )
			}
			   )
}

@Preview
@Composable
internal fun ListLinkPreview()
{
	//50 items
	NimazTheme {
		SettingsList(
				items = mapOf(
						"1" to "Item 1" ,
						"2" to "Item 2" ,
						"3" to "Item 3" ,
						"4" to "Item 4" ,
						"5" to "Item 5" ,
						"6" to "Item 6" ,
						"7" to "Item 7" ,
						"8" to "Item 8" ,
						"9" to "Item 9" ,
						"10" to "Item 10" ,
						"11" to "Item 11" ,
						"12" to "Item 12" ,
						"13" to "Item 13" ,
						"14" to "Item 14" ,
						"15" to "Item 15" ,
						"16" to "Item 16" ,
						"17" to "Item 17" ,
						"18" to "Item 18" ,
						"19" to "Item 19" ,
						"20" to "Item 20" ,
						"21" to "Item 21" ,
						"22" to "Item 22" ,
						"23" to "Item 23" ,
						"24" to "Item 24" ,
						"25" to "Item 25" ,
						"26" to "Item 26" ,
						"27" to "Item 27" ,
						"28" to "Item 28" ,
						"29" to "Item 29" ,
						"30" to "Item 30" ,
						"31" to "Item 31" ,
						"32" to "Item 32" ,
						"33" to "Item 33" ,
						"34" to "Item 34" ,
						"35" to "Item 35" ,
						"36" to "Item 36" ,
						"37" to "Item 37" ,
						"38" to "Item 38" ,
						"39" to "Item 39" ,
						"40" to "Item 40" ,
						"41" to "Item 41" ,
							 ) ,
				icon = { Icon(imageVector = Icons.Default.Clear , contentDescription = "Clear") } ,
				title = { Text(text = "Hello") } ,
				subtitle = { Text(text = "This is a longer text") } ,
				description = { Text(text = "This is a description") } ,
					)
	}
}