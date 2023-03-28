package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import kotlinx.coroutines.launch

@Composable
fun CustomDialog(
	title : String ,
	items : List<String> ,
	setShowDialog : (Boolean) -> Unit ,
	state : SettingValueState<Int> ,
	valueState : SettingValueState<String> ,
	onStateChange : (String) -> Unit ,
				)
{
	val coroutineScope = rememberCoroutineScope()
	val onSelected : (Int) -> Unit = { selectedIndex ->
		coroutineScope.launch {
			state.value = selectedIndex
			valueState.value = items[selectedIndex]
			onStateChange(items[selectedIndex])
		}
	}

	AlertDialog(
			title = { Text(text = title) } ,
			text = {
				Column(
						modifier = Modifier
							.verticalScroll(rememberScrollState())
							.fillMaxWidth()
							.fillMaxHeight()
					  ) {
					items.forEachIndexed { index , item ->
						val isSelected by rememberUpdatedState(newValue = state.value == index)
						//if valuestate.value has a value, then set the state.value to the index of the valuestate.value
						if (valueState.value.isNotEmpty() && valueState.value == item)
						{
							state.value = index
						}
						Row(
								modifier = Modifier
									.fillMaxWidth()
									.height(48.dp)
									.selectable(
											role = Role.RadioButton ,
											selected = isSelected ,
											onClick = { if (! isSelected) onSelected(index) }
											   ) ,
								verticalAlignment = Alignment.CenterVertically
						   ) {
							RadioButton(
									selected = isSelected ,
									onClick = { if (! isSelected) onSelected(index) }
									   )
							Text(
									text = item ,
									style = MaterialTheme.typography.titleMedium ,
									modifier = Modifier.padding(start = 8.dp)
								)
						}
					}
				}
			} ,
			onDismissRequest = { setShowDialog(false) } ,
			properties = DialogProperties(
					dismissOnBackPress = true ,
					dismissOnClickOutside = true ,
										 ) ,
			confirmButton = {
				Button(
						onClick = {
							setShowDialog(false)
						} ,
						content = {
							Text(
									text = "Confirm" , style = MaterialTheme.typography.titleMedium
								)
						}
					  )
			} ,
			dismissButton = {
				TextButton(
						onClick = { setShowDialog(false) } ,
						content = {
							Text(
									text = "Cancel" ,
									style = MaterialTheme.typography.titleMedium
								)
						}
						  )
			}
			   )
}
