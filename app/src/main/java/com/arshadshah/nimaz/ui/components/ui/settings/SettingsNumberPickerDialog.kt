package com.arshadshah.nimaz.ui.components.ui.settings

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.bLogic.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberIntSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.rememberStringSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.theme.NimazTheme
import kotlinx.coroutines.launch


@Composable
fun SettingsNumberPickerDialog(
	modifier : Modifier = Modifier ,
	state : SettingValueState<Int> = rememberIntSettingState() ,
	title : String ,
	description : String? = null ,
	iconDescription : String? = null ,
	iconPainter : Painter? = null ,
	items : List<Int> ,
	icon : @Composable() (() -> Unit)? = null ,
	useSelectedValueAsSubtitle : Boolean = true ,
	subtitle : @Composable() (() -> Unit)? = null ,
	action : @Composable() (() -> Unit)? = null ,
	valueState : SettingValueState<String> = rememberStringSettingState() ,
	onChange : (Int) -> Unit = { } ,
	height : Dp ,
							  )
{

	if (state.value >= items.size)
	{
		throw IndexOutOfBoundsException("Current value for $title list setting cannot be greater than items size")
	}

	var showDialog by remember { mutableStateOf(false) }

	val safeSubtitle = if (state.value >= 0 && useSelectedValueAsSubtitle)
	{
		{
			Text(
					text = if (items.size == 51)
						if (valueState.value.toInt() < 2 && valueState.value.toInt() > - 1) "${valueState.value} Degree " else if (valueState.value.toInt() != - 1) "${valueState.value} Degrees" else "${valueState.value} Degree"
					else
						if (valueState.value.toInt() < 2 && valueState.value.toInt() > - 1) "${valueState.value} Minute " else if (valueState.value.toInt() != - 1) "${valueState.value} Minutes" else "${valueState.value} Minute"
				)
		}
	} else subtitle

	SettingsMenuLink(
			modifier = if (icon != null) modifier else  modifier.padding(8.dp)  ,
			icon = icon ,
			title = {
				Text(
						text = title ,
						style = MaterialTheme.typography.titleLarge
					)
			} ,
			subtitle = safeSubtitle ,
			action = action ,
			onClick = { showDialog = true } ,
					)

	if (! showDialog) return

	val coroutineScope = rememberCoroutineScope()
	val onSelected : (Int) -> Unit = { selectedIndex ->
		coroutineScope.launch {
			Log.d("Nimaz: SettingsNumberPickerDialog" , "onSelected: $selectedIndex")
			val index = items.indexOf(selectedIndex)
			state.value = index
			valueState.value = selectedIndex.toString()
			onChange(selectedIndex)
		}
	}
	AlertDialogNimaz(
			contentDescription = title ,
			description = description ,
			contentHeight = height ,
			title = title ,
			contentToShow = {
					NumberPicker(
							modifier = modifier ,
							value = if (items.size == 51) valueState.value.toDouble()
								.toInt() else valueState.value.toInt() ,
							onValueChange = onSelected ,
							range = items ,
							dividersColor = MaterialTheme.colorScheme.outline ,
							textStyle = MaterialTheme.typography.titleLarge ,
							label = {
								if (items.size == 51)
									if (it < 2 && it > - 1) "$it Degree " else if (it != - 1) "$it Degrees" else "$it Degree"
								else
									if (it < 2 && it > - 1) "$it Minute " else if (it != - 1) "$it Minutes" else "$it Minute"
							} ,
								)
			} ,
			onDismissRequest = {
				showDialog = false
			} ,
			onConfirm = {
				showDialog = false
			} ,
			onDismiss = {
					showDialog = false
			}
					)
}


//preview
@Preview
@Composable
fun SettingsNumberPickerDialogPreview()
{
	val storage = rememberPreferenceStringSettingState(
			key = "test" ,
			defaultValue = "0"
													  )
	NimazTheme {
		SettingsNumberPickerDialog(
				title = "Title" ,
				description = "Description" ,
				items = listOf(0 , 1 , 2 , 3 , 4 , 5 , 6 , 7 , 8 , 9 , 10) ,
				icon = { Icon(imageVector = Icons.Filled.Clear , contentDescription = "Clear") } ,
				subtitle = { Text(text = "Subtitle") } ,
				valueState = storage ,
				height = 100.dp ,
								  )
	}
}

@Preview
@Composable
fun SettingsNumberPickerDialogNoIconPreview()
{
	val storage = rememberPreferenceStringSettingState(
			key = "test" ,
			defaultValue = "0"
													  )
	NimazTheme {
		SettingsNumberPickerDialog(
				title = "Title" ,
				description = "Description" ,
				items = listOf(0 , 1 , 2 , 3 , 4 , 5 , 6 , 7 , 8 , 9 , 10) ,
				subtitle = { Text(text = "Subtitle") } ,
				valueState = storage ,
				height = 100.dp ,
								  )
	}
}