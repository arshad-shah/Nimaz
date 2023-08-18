package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.RadioButtonCustom
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.settings.state.StringPreferenceSettingValueState
import kotlinx.coroutines.launch

@Composable
fun CustomDialog(
	title : String ,
	items : List<String> ,
	setShowDialog : (Boolean) -> Unit ,
	state : SettingValueState<Int> ,
	valueState : StringPreferenceSettingValueState ,
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

	AlertDialogNimaz(
			 topDivider = false ,
			 bottomDivider = false ,
			 dismissButtonText = "Close" ,
			 contentHeight = 120.dp ,
			 properties = DialogProperties(
					  dismissOnBackPress = true ,
					  dismissOnClickOutside = true
										  ) ,
			 contentDescription = title ,
			 title = title ,
			 contentToShow = {
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
								  .padding(vertical = 4.dp , horizontal = 8.dp)
								  .height(52.dp)
								  .selectable(
										   role = Role.RadioButton ,
										   selected = isSelected ,
										   onClick = { if (! isSelected) onSelected(index) }
											 ) ,
							  verticalAlignment = Alignment.CenterVertically
						) {
						 RadioButtonCustom(
								  selected = isSelected ,
								  onClick = { if (! isSelected) onSelected(index) } ,
								  modifier = Modifier.padding(start = 16.dp)
										  )

						 Text(
								  text = item ,
								  style = MaterialTheme.typography.titleMedium ,
								  modifier = Modifier.padding(start = 16.dp)
							 )
					 }
				 }
			 } ,
			 onDismissRequest = {
				 setShowDialog(false)
			 } ,
			 onConfirm = {
				 setShowDialog(false)
			 } ,
			 onDismiss = {
				 setShowDialog(false)
			 })
}
