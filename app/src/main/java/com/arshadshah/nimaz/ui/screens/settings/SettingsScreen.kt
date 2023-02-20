package com.arshadshah.nimaz.ui.screens.settings

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.ui.intro.BatteryExemptionUI
import com.arshadshah.nimaz.ui.components.ui.settings.*
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.Alarms
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import es.dmoral.toasty.Toasty
import java.time.LocalDateTime
import java.time.ZoneOffset

@Composable
fun SettingsScreen(
	onNavigateToPrayerTimeCustomizationScreen : () -> Unit ,
	onNavigateToAboutScreen : () -> Unit ,
	paddingValues : PaddingValues ,
				  )
{
	val context = LocalContext.current
	val viewModel = viewModel(key = "SettingsViewModel", initializer = { SettingsViewModel(context) }, viewModelStoreOwner = context as ComponentActivity)

	LaunchedEffect(key1 = true) {
		viewModel.handleEvent(SettingsViewModel.SettingsEvent.LoadLocation)
	}


	val isLocationManual = remember {
		viewModel.isLocationManual
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
	
	val isLocationNameLoading = remember {
		viewModel.isLocationNameLoading
	}.collectAsState()

	val sharedPreferences = PrivateSharedPreferences(context)

	Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState() , true)
				.padding(paddingValues)
		  ) {

		SettingsGroup(title = { Text(text = "Location") }) {
			val storage =
				rememberPreferenceBooleanSettingState(AppConstants.LOCATION_TYPE , true)
			storage.value = isLocationManual.value
			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				SettingsSwitch(
						state = storage ,
						icon = {
							Icon(
									modifier = Modifier.size(24.dp) ,
									painter = painterResource(id = R.drawable.marker_icon) ,
									contentDescription = "Location"
								)
						} ,
						title = {
							if (storage.value)
							{
								Text(text = "Automatic")
							} else
							{
								Text(text = "Manual")
							}
						} ,
						subtitle = {
							if (storage.value)
							{
								if(isLocationNameLoading.value)
								{
									Text(text = "Loading...")
								}else{
									Text(text = locationNameState.value)
								}
							} else
							{
								Text(text = "Enter your location manually")
							}
						},
						onCheckedChange = {
							storage.value = it
							viewModel.handleEvent(
									SettingsViewModel.SettingsEvent.LocationToggle(it)
													)
							//if its true then we need to get the location from the gps
							if (it)
							{
								viewModel.handleEvent(
										SettingsViewModel.SettingsEvent.LocationAutomatic(context)
													)
							}else{
								//if its false then we need to get the location from the shared preferences
								viewModel.handleEvent(
										SettingsViewModel.SettingsEvent.LocationManual(context, locationNameState.value)
													)
							}
						}
							  )
			}
			if (! storage.value)
			{
				ElevatedCard(
						modifier = Modifier
							.padding(8.dp)
							.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
							.fillMaxWidth()
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

		ElevatedCard(
				modifier = Modifier
					.padding(8.dp)
					.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
					.fillMaxWidth()
					) {
			SettingsMenuLink(
					title = { Text(text = "Prayer Times Adjustments") } ,
					onClick = onNavigateToPrayerTimeCustomizationScreen ,
					icon = {
						Icon(
								modifier = Modifier.size(24.dp) ,
								painter = painterResource(id = R.drawable.settings_sliders_icon) ,
								contentDescription = "Clock"
							)
					} ,
							)
		}


		SettingsGroup(title = { Text(text = "Alarm and Notifications") }) {
			//get all the prayer times from the shared preferences
			val fajr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.FAJR , "00:00"))
			val sunrise =
				LocalDateTime.parse(sharedPreferences.getData(AppConstants.SUNRISE , "00:00"))
			val dhuhr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.DHUHR , "00:00"))
			val asr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.ASR , "00:00"))
			val maghrib =
				LocalDateTime.parse(sharedPreferences.getData(AppConstants.MAGHRIB , "00:00"))
			val isha = LocalDateTime.parse(sharedPreferences.getData(AppConstants.ISHA , "00:00"))

			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				SettingsMenuLink(
						title = { Text(text = "Force Reset Alarms") } ,
						onClick = {
							CreateAlarms().exact(
									context ,
									fajr ,
									sunrise ,
									dhuhr ,
									asr ,
									maghrib ,
									isha
												)
							Toasty.success(context , "Alarms Reset" , Toast.LENGTH_SHORT , true)
								.show()
						} ,
						icon = {
							Icon(
									modifier = Modifier.size(24.dp) ,
									painter = painterResource(id = R.drawable.alarm_clock_icon) ,
									contentDescription = "Notifications"
								)
						} ,
								)
			}

			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				SettingsMenuLink(
						title = { Text(text = "Test Alarm") } ,
						//we are goping to set the alarm in next 10 seconds
						subtitle = { Text(text = "Alarm will be set in 10 seconds") } ,
						onClick = {
							val zuharAdhan =
								"android.resource://" + context.packageName + "/" + R.raw.zuhar
							//create notification channels
							val notificationHelper = NotificationHelper()
							//fajr
							notificationHelper.createNotificationChannel(
									context ,
									NotificationManager.IMPORTANCE_MAX ,
									true ,
									"Test_channel" ,
									"A test channel for adhan" ,
									"Test_Channel" ,
									zuharAdhan
																		)
							val timeToNotify = LocalDateTime.now().plusSeconds(10).toInstant(
									ZoneOffset.UTC
																							)
								.toEpochMilli()
							val testPendingIntent = CreateAlarms().createPendingIntent(
									context ,
									1006 ,
									2006 ,
									timeToNotify ,
									"Test Adhan" ,
									"Test_Channel"
																					  )
							Alarms().setExactAlarm(context , timeToNotify , testPendingIntent)
							Toasty.success(context , "Test Alarm set" , Toast.LENGTH_SHORT , true)
								.show()
						} ,
						icon = {
							Icon(
									modifier = Modifier.size(24.dp) ,
									painter = painterResource(id = R.drawable.alarm_set_icon) ,
									contentDescription = "Back"
								)
						} ,
								)
			}

			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				SettingsMenuLink(
						title = { Text(text = "Notification Settings") } ,
						subtitle = { Text(text = "Settings for all the Adhan") } ,
						onClick = {
							//open the notification settings
							val intent = Intent()
							intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
							intent.putExtra(
									"android.provider.extra.APP_PACKAGE" ,
									context.packageName
										   )
							context.startActivity(intent)
						} ,
						icon = {
							Icon(
									modifier = Modifier.size(24.dp) ,
									painter = painterResource(id = R.drawable.settings_icon) ,
									contentDescription = "Settings for notification"
								)
						} ,
								)
			}

			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				BatteryExemptionUI()
			}
		}

		SettingsGroup(title = { Text(text = "Legal") }) {
			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				SettingsMenuLink(
						title = { Text(text = "Privacy Policy") } ,
						onClick = {
							//open the privacy policy in the browser
							val url =
								"https://nimaz.arshadshah.com/static/media/Privacy%20Policy.06ada0df63d36ef44b56.pdf"
							val i = Intent(Intent.ACTION_VIEW)
							i.data = Uri.parse(url)
							context.startActivity(i)
						} ,
						icon = {
							Icon(
									modifier = Modifier.size(24.dp) ,
									painter = painterResource(id = R.drawable.privacy_policy_icon) ,
									contentDescription = "Privacy Policy"
								)
						} ,
								)
			}

			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
						.fillMaxWidth()
						) {
				SettingsMenuLink(
						title = { Text(text = "Terms and Conditions") } ,
						onClick = {
							//open the terms and conditions in the browser
							val url =
								"https://nimaz.arshadshah.com/static/media/Terms%20and%20Condition.c2cb253a0ddd3b258abf.pdf"
							val i = Intent(Intent.ACTION_VIEW)
							i.data = Uri.parse(url)
							context.startActivity(i)
						} ,
						icon = {
							Icon(
									modifier = Modifier.size(24.dp) ,
									painter = painterResource(id = R.drawable.document_icon) ,
									contentDescription = "Privacy Policy"
								)
						} ,
								)
			}
		}


		ElevatedCard(
				modifier = Modifier
					.padding(8.dp)
					.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
					.fillMaxWidth()
					) {
			SettingsMenuLink(
					title = { Text(text = "About") } ,
					//version of the app
					subtitle = { Text(text = "Version: " + BuildConfig.VERSION_NAME) } ,
					onClick = {
						onNavigateToAboutScreen()
					} ,
					icon = {
						Icon(
								modifier = Modifier.size(24.dp) ,
								painter = painterResource(id = R.drawable.info_icon) ,
								contentDescription = "About"
							)
					} ,
							)
		}
	}
}