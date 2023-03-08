package com.arshadshah.nimaz.ui.components.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.BooleanPreferenceSettingValueState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.location.FeatureThatRequiresLocationPermission
import com.arshadshah.nimaz.utils.network.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.sunMoonUtils.AutoAnglesCalc
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import es.dmoral.toasty.Toasty


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationSettings(isIntro : Boolean = false)
{

	val context = LocalContext.current
	val viewModel = viewModel(
			key = "SettingsViewModel" ,
			initializer = { SettingsViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity
							 )
	val viewModelPrayerTimes = viewModel(
			key = "PrayerTimesViewModel" ,
			initializer = { PrayerTimesViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
										)
	val isLocationAuto = remember {
		viewModel.isLocationAuto
	}.collectAsState()

	val locationNameState = remember {
		viewModel.locationName
	}.collectAsState()

	val latitudeState = remember {
		viewModel.latitude
	}.collectAsState()

	val longitudeState = remember {
		viewModel.longitude
	}.collectAsState()
	val isLoading = remember {
		viewModel.isLoading
	}.collectAsState()

	val isError = remember {
		viewModel.isError
	}.collectAsState()


	if (isError.value.isNotBlank())
	{
		Toasty.error(context , isError.value , Toasty.LENGTH_SHORT).show()
	} else
	{

		//location permission state
		val locationPermissionState = rememberMultiplePermissionsState(
				permissions = listOf(
						Manifest.permission.ACCESS_COARSE_LOCATION ,
						Manifest.permission.ACCESS_FINE_LOCATION
									)
																	  )
		//the state of the switch
		val state =
			rememberPreferenceBooleanSettingState(
					AppConstants.LOCATION_TYPE ,
					false
												 )




		if (state.value)
		{
			FeatureThatRequiresLocationPermission(locationPermissionState , state)
		}

		val latitude = remember {
			viewModel.latitude
		}.collectAsState()
		val longitude = remember {
			viewModel.longitude
		}.collectAsState()

		val autoParams = remember {
			viewModel.autoParams
		}.collectAsState()

		LaunchedEffect(
				key1 = locationNameState.value ,
				key2 = latitudeState.value ,
				key3 = longitudeState.value
					  ) {

			if(autoParams.value){
				//set method to other
				viewModel.handleEvent(
						SettingsViewModel.SettingsEvent.CalculationMethod(
								"OTHER"
																		 )
											)
				//set fajr angle
				viewModel.handleEvent(
						SettingsViewModel.SettingsEvent.FajrAngle(
								AutoAnglesCalc().calculateFajrAngle(context , latitude.value , longitude.value).toString()
																 )
											)
				//set ishaa angle
				viewModel.handleEvent(
						SettingsViewModel.SettingsEvent.IshaAngle(
								AutoAnglesCalc().calculateIshaaAngle(context , latitude.value , longitude.value).toString()
																 )
											)
				//set high latitude method
				viewModel.handleEvent(
						SettingsViewModel.SettingsEvent.HighLatitude(
								"TWILIGHT_ANGLE"
																	)
											)
				viewModel.handleEvent(
						SettingsViewModel.SettingsEvent.LoadSettings
											)
			}


			viewModelPrayerTimes.handleEvent(
					context , PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
					PrayerTimesParamMapper.getParams(context)
																					  )
											)

			viewModelPrayerTimes.handleEvent(
					context ,
					PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
							context
																	   )
											)

		}

		if (isIntro)
		{
			LocationToggleSwitch(
					state = state ,
					locationPermissionState = locationPermissionState ,
					isIntro = true ,
								)
			if (! state.value)
			{
				ElevatedCard(
						modifier = Modifier
							.padding(8.dp)
							.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
							.fillMaxWidth()
							.placeholder(
									visible = isLoading.value ,
									color = MaterialTheme.colorScheme.outline ,
									shape = RoundedCornerShape(4.dp) ,
									highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																			)
										)
							) {
					ManualLocationInput(
							handleSettingEvents = viewModel::handleEvent ,
							locationNameState = locationNameState ,
									   )
				}
			}
		} else
		{
			SettingsGroup(title = { Text(text = "Location") }) {
				LocationToggleSwitch(
						state = state ,
						locationPermissionState = locationPermissionState ,
						isIntro = false ,
									)
				if (! state.value)
				{
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								.placeholder(
										visible = isLoading.value ,
										color = MaterialTheme.colorScheme.outline ,
										shape = RoundedCornerShape(4.dp) ,
										highlight = PlaceholderHighlight.shimmer(
												highlightColor = Color.White ,
																				)
											)
								) {
						ManualLocationInput(
								handleSettingEvents = viewModel::handleEvent ,
								locationNameState = locationNameState ,
										   )
					}
					CoordinatesView(
							longitudeState = longitudeState ,
							latitudeState = latitudeState ,
								   )
				}
			}
		}


	}
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationToggleSwitch(
	state : BooleanPreferenceSettingValueState ,
	locationPermissionState : MultiplePermissionsState ,
	isIntro : Boolean ,
						)
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = "SettingsViewModel" ,
			initializer = { SettingsViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity
							 )
	val viewModelPrayerTimes = viewModel(
			key = "PrayerTimesViewModel" ,
			initializer = { PrayerTimesViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
										)
	val isLocationAuto = remember {
		viewModel.isLocationAuto
	}.collectAsState()
	val locationNameState = remember {
		viewModel.locationName
	}.collectAsState()
	val isLoading = remember {
		viewModel.isLoading
	}.collectAsState()

	val isChecked = remember {
		mutableStateOf(isLocationAuto.value)
	}

	val lifecycle = LocalLifecycleOwner.current.lifecycle
	DisposableEffect(lifecycle) {
		val observer = LifecycleEventObserver { _ , event ->
			when (event)
			{
				Lifecycle.Event.ON_RESUME ->
				{
					if (locationPermissionState.permissions[0].status.isGranted || locationPermissionState.permissions[1].status.isGranted)
					{
						//check if the value saved in the shared preferences is true
						val isLocationAutoInPref = PrivateSharedPreferences(context).getDataBoolean(
								AppConstants.LOCATION_TYPE ,
								false
																							)
						if(isLocationAutoInPref)
						{
							viewModel.handleEvent(
									SettingsViewModel.SettingsEvent.LocationToggle(
											context ,
											true
																				  )
													 )
							viewModelPrayerTimes.handleEvent(
									context , PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
									PrayerTimesParamMapper.getParams(context)
																									  )
															)
							viewModelPrayerTimes.handleEvent(
									context ,
									PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
											context
																					   )
															)
							isChecked.value = true
						}else{
							viewModel.handleEvent(
									SettingsViewModel.SettingsEvent.LocationToggle(
											context ,
											false
																				  )
													 )
							isChecked.value = false
						}
					}
				}

				else ->
				{

				}
			}
		}

		lifecycle.addObserver(observer)
		onDispose {
			lifecycle.removeObserver(observer)
		}
	}

	ElevatedCard(
			modifier = Modifier
				.padding(8.dp)
				.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
				.fillMaxWidth()
				.testTag("LocationSwitch")
				) {
		SettingsSwitch(
				modifier = Modifier.placeholder(
						visible = isLoading.value ,
						color = MaterialTheme.colorScheme.outline ,
						shape = RoundedCornerShape(4.dp) ,
						highlight = PlaceholderHighlight.shimmer(
								highlightColor = Color.White ,
																)
											   ) ,
				state = state ,
				icon = {
					Icon(
							modifier = Modifier.size(24.dp) ,
							painter = painterResource(id = R.drawable.marker_icon) ,
							contentDescription = "Location"
						)
				} ,
				title = {
					if (isIntro)
					{
						Text(text = "Enable Auto Location")
					} else
					{
						if (state.value)
						{
							Text(text = "Automatic")
						} else
						{
							Text(text = "Manual")
						}
					}
				} ,
				subtitle = {
					//if the permission is granted, show a checkmark and text saying "Allowed"
					if (isIntro)
					{
						if (isChecked.value)
						{
							Row(
									verticalAlignment = Alignment.CenterVertically
							   ) {
								Icon(
										imageVector = Icons.Filled.CheckCircle ,
										contentDescription = "Location Allowed"
									)
								Text(text = "Enabled")
							}
						} else
						{
							//if the permission is not granted, show a notification icon and text saying "Not Allowed"
							Row(
									verticalAlignment = Alignment.CenterVertically
							   ) {
								Icon(
										imageVector = Icons.Filled.Close ,
										contentDescription = "Location Not Allowed"
									)
								Text(text = "Disabled")
							}
						}
					} else
					{
						if (isLocationAuto.value)
						{
							Text(text = locationNameState.value)
						}
					}
				} ,
				onCheckedChange = {
					if (it)
					{
						if (locationPermissionState.allPermissionsGranted)
						{
							viewModel.handleEvent(
									SettingsViewModel.SettingsEvent.LocationToggle(
											context ,
											true
																				  )
												 )
							viewModelPrayerTimes.handleEvent(
									context ,
									PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
											PrayerTimesParamMapper.getParams(context)
																							)
															)
							viewModelPrayerTimes.handleEvent(
									context ,
									PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
											context
																					   )
															)
						} else
						{
							locationPermissionState.launchMultiplePermissionRequest()
						}
					} else
					{
						viewModel.handleEvent(
								SettingsViewModel.SettingsEvent.LocationToggle(
										context ,
										it
																			  )
											 )
						viewModelPrayerTimes.handleEvent(
								context , PrayerTimesViewModel.PrayerTimesEvent.UPDATE_PRAYERTIMES(
								PrayerTimesParamMapper.getParams(context)
																								  )
														)
						viewModelPrayerTimes.handleEvent(
								context ,
								PrayerTimesViewModel.PrayerTimesEvent.UPDATE_WIDGET(
										context
																				   )
														)
						isChecked.value = false
						if (isIntro)
						{
							Toasty.info(
									context ,
									"Please disable location permission for Nimaz in \n Permissions -> Location -> Don't Allow" ,
									Toasty.LENGTH_LONG
									   ).show()
							//send the user to the location settings of the app
							val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
							with(intent) {
								data = Uri.fromParts("package" , context.packageName , null)
								addCategory(Intent.CATEGORY_DEFAULT)
								addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
							}

							context.startActivity(intent)
						}
					}
				}
					  )
	}

	if (state.value && isIntro)
	{
		if (locationNameState.value.isBlank())
		{
			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				Text(
						textAlign = TextAlign.Center ,
						text = "Current Location: Loading..." ,
						modifier = Modifier.padding(16.dp) ,
						style = MaterialTheme.typography.bodyMedium
					)
			}
		} else
		{
			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				Text(
						textAlign = TextAlign.Center ,
						text = "Current Location: " + locationNameState.value ,
						modifier = Modifier.padding(16.dp) ,
						style = MaterialTheme.typography.bodyMedium
					)
			}
		}
	}
}
