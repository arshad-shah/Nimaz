package com.arshadshah.nimaz.ui.screens.settings

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.EmailPasswordActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.settings.*
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.Alarms
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.utils.location.LocationFinderAuto
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.LogIn
import es.dmoral.toasty.Toasty
import java.time.LocalDateTime
import java.time.ZoneOffset


@Composable
fun SettingsScreen(
	onNavigateToPrayerTimeCustomizationScreen : () -> Unit ,
	onNavigateToAboutScreen : () -> Unit ,
	paddingValues : PaddingValues ,
	viewModel : SettingsViewModel = SettingsViewModel() ,
				  )
{
	val context = LocalContext.current

	val isLoggedIn = viewModel.loginUiState.collectAsState().value

	val user = viewModel.userStateFlow.collectAsState()

	val cityname =
		rememberPreferenceStringSettingState(AppConstants.LOCATION_INPUT, "Abbeyleix")

	val sharedPreferences = PrivateSharedPreferences(context)

	//if any of the settings are changed, set the flag to true so that the prayer times can be updated
	if (cityname.value != sharedPreferences.getData(AppConstants.LOCATION_INPUT, "Abbeyleix"))
	{
		sharedPreferences.saveDataBoolean(AppConstants.RECALCULATE_PRAYER_TIMES, true)
	}

	val activityResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			viewModel.checkLogin()
		}
	}

	Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState() , true)
				.padding(paddingValues)
		  ) {

		when(isLoggedIn){
			is SettingsViewModel.LoginState.Success -> {
				Profile(user, viewModel)
			}
			is SettingsViewModel.LoginState.Error -> {
				ElevatedCard {
					//login card
					SettingsMenuLink(title = {
						Text(text = "Login or Signup")
					},
									 icon = {
										 Icon(
												 imageVector = FeatherIcons.LogIn ,
												 contentDescription = "Login or signup"
											 )
									 },
									 onClick = {
										 activityResult.launch(Intent(context, EmailPasswordActivity::class.java))
									 })
				}
			}
			is SettingsViewModel.LoginState.Loading -> {
				CircularLoaderCard()
			}
		}

		SettingsGroup(title = { Text(text = "Location") }) {
			val storage =
				rememberPreferenceBooleanSettingState(AppConstants.LOCATION_TYPE, true)
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
									imageVector = Icons.Outlined.LocationOn ,
									contentDescription = "Location"
								)
						} ,
						title = {
							if (storage.value)
							{
								Text(text = "Automatic")
								//if the location city name is not null, then run the code
								if (cityname.value != "")
								{
									Location().getAutomaticLocation(LocalContext.current)
								}
							} else
							{
								Text(text = "Manual")
								val locationFinderAuto = LocationFinderAuto()
								locationFinderAuto.stopLocationUpdates()
							}
						} ,
						subtitle = {
							if (storage.value)
							{
								Text(text = cityname.value)
							}
						} ,
						onCheckedChange = {
							storage.value = it
							sharedPreferences.saveDataBoolean(AppConstants.RECALCULATE_PRAYER_TIMES, true)
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
					ManualLocationInput()
				}
				CoordinatesView()
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
								imageVector = FeatherIcons.Clock ,
								contentDescription = "Clock"
							)
					} ,
							)
		}


		SettingsGroup(title = { Text(text = "Alarm and Notifications") }) {
			//get all the prayer times from the shared preferences
			val fajr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.FAJR, "00:00"))
			val sunrise = LocalDateTime.parse(sharedPreferences.getData(AppConstants.SUNRISE, "00:00"))
			val dhuhr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.DHUHR, "00:00"))
			val asr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.ASR, "00:00"))
			val maghrib = LocalDateTime.parse(sharedPreferences.getData(AppConstants.MAGHRIB, "00:00"))
			val isha = LocalDateTime.parse(sharedPreferences.getData(AppConstants.ISHA, "00:00"))

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
									imageVector = Icons.Filled.Notifications ,
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
									imageVector = Icons.Filled.Notifications ,
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
									imageVector = Icons.Filled.Settings ,
									contentDescription = "Settings for notification"
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
								imageVector = Icons.Filled.Info ,
								contentDescription = "About"
							)
					} ,
							)
		}
	}
}