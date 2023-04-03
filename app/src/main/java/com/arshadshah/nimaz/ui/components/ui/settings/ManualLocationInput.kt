package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.AlertDialogNimaz
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLocationInput(
	handleSettingEvents : KFunction1<SettingsViewModel.SettingsEvent , Unit> ,
	locationNameState : State<String> ,
					   )
{

	val context = LocalContext.current
	val viewModelPrayerTimes = viewModel(
			key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY ,
			initializer = { PrayerTimesViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
										)
	val showDialog = remember { mutableStateOf(false) }
	val name = remember {
		mutableStateOf(locationNameState.value)
	}
	//show manual location input
	//onclick open dialog
	SettingsMenuLink(
			title = { Text(text = "Edit Location") } ,
			subtitle = { Text(text = locationNameState.value) } ,
			onClick = {
				showDialog.value = true
			} ,
			icon = {
				Icon(
						modifier = Modifier.size(24.dp) ,
						painter = painterResource(id = R.drawable.location_marker_edit_icon) ,
						contentDescription = "Location"
					)
			}
					)

	if (! showDialog.value) return

	AlertDialogNimaz(
			bottomDivider = false ,
			topDivider = false ,
			contentHeight = 100.dp,
			confirmButtonText = "Submit",
			contentDescription = "Edit Location" ,
			title = "Edit Location" ,
			contentToShow = {
				OutlinedTextField(
						shape = MaterialTheme.shapes.extraLarge ,
						value = name.value ,
						onValueChange = { name.value = it } ,
						label = { Text(text = "Location") } ,
						singleLine = true ,
						maxLines = 1 ,
						modifier = Modifier.fillMaxWidth()
								 )
			} ,
			onDismissRequest = {
				showDialog.value = false
			} ,
			onConfirm = {
				handleSettingEvents(
						SettingsViewModel.SettingsEvent.LocationInput(
								context ,
								name.value
																 )
								   )
				viewModelPrayerTimes.handleEvent(
						context ,
						PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
								context
																		   )
												)

				showDialog.value = false

			} ,
			onDismiss = {
				showDialog.value = false
			})
}