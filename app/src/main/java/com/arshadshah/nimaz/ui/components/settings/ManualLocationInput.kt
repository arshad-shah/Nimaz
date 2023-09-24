package com.arshadshah.nimaz.ui.components.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.viewModel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLocationInput()
{
	val context = LocalContext.current
	val viewModelPrayerTimes = viewModel(
			 key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY ,
			 initializer = { PrayerTimesViewModel() } ,
			 viewModelStoreOwner = LocalContext.current as ComponentActivity
										)
	val viewModel = viewModel(
			 key = AppConstants.SETTINGS_VIEWMODEL_KEY ,
			 initializer = { SettingsViewModel(context) } ,
			 viewModelStoreOwner = context as ComponentActivity
							 )

	val locationNameState = remember {
		viewModel.locationName
	}.collectAsState()
	val isLoading = remember {
		viewModel.isLoading
	}.collectAsState()

	val showDialog = remember { mutableStateOf(false) }
	val name = remember {
		mutableStateOf(locationNameState.value)
	}
	//show manual location input
	//onclick open dialog
	SettingsMenuLink(
			 title = { Text(text = "Edit Location") } ,
			 subtitle = {
				 Text(
						  text = locationNameState.value ,
						  modifier = Modifier.placeholder(
								   visible = isLoading.value ,
								   color = MaterialTheme.colorScheme.outline ,
								   shape = RoundedCornerShape(4.dp) ,
								   highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																		   )

														 )
					 )
			 } ,
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
			 cardContent = false ,
			 bottomDivider = false ,
			 topDivider = false ,
			 contentHeight = 100.dp ,
			 confirmButtonText = "Submit" ,
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
						  modifier = Modifier
							  .fillMaxWidth()
							  .padding(horizontal = 16.dp)
								  )
			 } ,
			 onDismissRequest = {
				 showDialog.value = false
			 } ,
			 onConfirm = {
				 viewModel.handleEvent(
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