package com.arshadshah.nimaz.ui.screens.settings

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.components.ui.settings.*
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.Alarms
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.utils.location.LocationFinderAuto
import compose.icons.FeatherIcons
import compose.icons.feathericons.*
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
	val sharedPreferences = PrivateSharedPreferences(context)
	//values for coordinates that are mutable
	val longitude =
		remember { mutableStateOf(sharedPreferences.getDataDouble(AppConstants.LONGITUDE , 0.0)) }
	val latitude =
		remember { mutableStateOf(sharedPreferences.getDataDouble(AppConstants.LATITUDE , 0.0)) }
	val locationName = remember {
		mutableStateOf(
				sharedPreferences.getData(
						AppConstants.LOCATION_INPUT ,
						"Abbeyleix"
										 )
					  )
	}

	val cityname =
		rememberPreferenceStringSettingState(AppConstants.LOCATION_INPUT , "Abbeyleix")


	//if any of the settings are changed, set the flag to true so that the prayer times can be updated
	if (cityname.value != sharedPreferences.getData(AppConstants.LOCATION_INPUT , "Abbeyleix"))
	{
		sharedPreferences.saveDataBoolean(AppConstants.RECALCULATE_PRAYER_TIMES , true)
	}

	//a listner callback that is called when the location is found
	val locationFoundCallback = { longitudeValue : Double , latitudeValue : Double ->
		longitude.value = longitudeValue
		latitude.value = latitudeValue
	}

	//a callback that is called when using mauual location
	val locationFoundCallbackManual =
		{ longitudeValue : Double , latitudeValue : Double , name : String ->
			longitude.value = longitudeValue
			latitude.value = latitudeValue
			locationName.value = name
			sharedPreferences.saveData(AppConstants.LOCATION_INPUT , name)
		}

	Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState() , true)
				.padding(paddingValues)
		  ) {

		SettingsGroup(title = { Text(text = "Location") }) {
			val storage =
				rememberPreferenceBooleanSettingState(AppConstants.LOCATION_TYPE , true)
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
									imageVector = FeatherIcons.MapPin ,
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
									Location().getAutomaticLocation(
											LocalContext.current ,
											locationFoundCallback ,
											locationFoundCallbackManual
																   )
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
								Text(text = locationName.value)
							}
						} ,
						onCheckedChange = {
							storage.value = it
							sharedPreferences.saveDataBoolean(
									AppConstants.RECALCULATE_PRAYER_TIMES ,
									true
															 )
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
					ManualLocationInput(locationFoundCallbackManual)
				}
				CoordinatesView(
						longitude = longitude ,
						latitude = latitude
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
								imageVector = FeatherIcons.Clock ,
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
									imageVector = FeatherIcons.Bell ,
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
									imageVector = FeatherIcons.Bell ,
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
									imageVector = FeatherIcons.Settings ,
									contentDescription = "Settings for notification"
								)
						} ,
								)
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
									imageVector = FeatherIcons.Lock ,
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
									imageVector = FeatherIcons.File ,
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
								imageVector = FeatherIcons.Info ,
								contentDescription = "About"
							)
					} ,
							)
		}
	}
}