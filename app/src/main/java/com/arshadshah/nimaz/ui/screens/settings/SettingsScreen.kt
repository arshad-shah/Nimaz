package com.arshadshah.nimaz.ui.screens.settings

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.CHANNEL_DESC_TEST
import com.arshadshah.nimaz.constants.AppConstants.CHANNEL_TEST
import com.arshadshah.nimaz.constants.AppConstants.DARK_MODE
import com.arshadshah.nimaz.constants.AppConstants.TEST_CHANNEL_ID
import com.arshadshah.nimaz.constants.AppConstants.TEST_NOTIFY_ID
import com.arshadshah.nimaz.constants.AppConstants.TEST_PI_REQUEST_CODE
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_ABOUT
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_PRAYER_TIMES_CUSTOMIZATION_BUTTON
import com.arshadshah.nimaz.constants.AppConstants.THEME
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.bLogic.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.components.ui.intro.BatteryExemptionUI
import com.arshadshah.nimaz.ui.components.ui.intro.NotificationScreenUI
import com.arshadshah.nimaz.ui.components.ui.settings.*
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.Alarms
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun SettingsScreen(
	onNavigateToPrayerTimeCustomizationScreen : () -> Unit ,
	onNavigateToAboutScreen : () -> Unit ,
	paddingValues : PaddingValues ,
	onNavigateToWebViewScreen : (String) -> Unit ,
	onNavigateToLicencesScreen : () -> Unit ,
				  )
{
	val context = LocalContext.current
	val viewModelSettings = viewModel(
			key = AppConstants.SETTINGS_VIEWMODEL_KEY ,
			initializer = { SettingsViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity
									 )
	val themeState = remember {
		viewModelSettings.theme
	}.collectAsState()

	val isDarkMode = remember {
		viewModelSettings.isDarkMode
	}.collectAsState()

	val viewModel = viewModel(
			key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY ,
			initializer = { PrayerTimesViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )

	LaunchedEffect(Unit) {
		viewModelSettings.handleEvent(SettingsViewModel.SettingsEvent.LoadSettings)
		viewModelSettings.handleEvent(SettingsViewModel.SettingsEvent.CheckUpdate(context , false))
	}
	val updateAvailabile = remember {
		viewModelSettings.isUpdateAvailable
	}.collectAsState()

	val updateAvailableText = if (updateAvailabile.value)
	{
		"Update Available"
	} else
	{
		"Nimaz is up to date"
	}

	val fajrTime = remember {
		viewModel.fajrTime
	}.collectAsState()

	val sunriseTime = remember {
		viewModel.sunriseTime
	}.collectAsState()

	val dhuhrTime = remember {
		viewModel.dhuhrTime
	}.collectAsState()

	val asrTime = remember {
		viewModel.asrTime
	}.collectAsState()

	val maghribTime = remember {
		viewModel.maghribTime
	}.collectAsState()

	val ishaTime = remember {
		viewModel.ishaTime
	}.collectAsState()

	val sharedPreferences = PrivateSharedPreferences(context)

	Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState() , true)
				.padding(paddingValues)
				.testTag(AppConstants.TEST_TAG_SETTINGS)
		  ) {
		LocationSettings()

		ElevatedCard(
				shape = MaterialTheme.shapes.extraLarge ,
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
					.testTag(TEST_TAG_PRAYER_TIMES_CUSTOMIZATION_BUTTON)
					) {
			SettingsMenuLink(
					title = { Text(text = "Prayer Times") } ,
					onClick = onNavigateToPrayerTimeCustomizationScreen ,
					icon = {
						Icon(
								modifier = Modifier.size(24.dp) ,
								painter = painterResource(id = R.drawable.settings_sliders_icon) ,
								contentDescription = "Prayer Times settings"
							)
					} ,
							)
		}

		val stateOfTheme =
			rememberPreferenceStringSettingState(key = THEME , defaultValue = themeState.value)

		stateOfTheme.value = themeState.value

		//map of theme to theme name
//		keys are like this LIGHT , DARK , SYSTEM
		val themeMapForDynamic = mapOf(
				"DEFAULT" to "App Default" ,
				"Raisin_Black" to "Raisin Black" ,
				"Dark_Red" to "Burgundy" ,
				"Dark_Liver" to "Dark Liver" ,
				"Rustic_brown" to "Rustic Brown" ,
				"SYSTEM" to "System Default" ,
				"DYNAMIC" to "Dynamic"
									  )
		val themeMapForNonDynamic = mapOf(
				"DEFAULT" to "App Default" ,
				"Raisin_Black" to "Raisin Black" ,
				"Dark_Red" to "Burgundy" ,
				"Dark_Liver" to "Dark Liver" ,
				"Rustic_brown" to "Rustic Brown" ,
				"SYSTEM" to "System Default" ,
										 )

		val stateDarkMode =
			rememberPreferenceBooleanSettingState(
					DARK_MODE ,
					false
												 )
		stateDarkMode.value = isDarkMode.value

		SettingsGroup(
				title = { Text(text = "Theme") } ,
					 ) {
			if (stateOfTheme.value != "SYSTEM")
			{
				ElevatedCard(
						shape = MaterialTheme.shapes.extraLarge ,
						modifier = Modifier
							.padding(8.dp)
							.fillMaxWidth()
							) {
					//switch for theme mode dark/light when its not dynamic
					SettingsSwitch(
							state = stateDarkMode ,
							title = { Text(text = if (stateDarkMode.value) "Dark Mode" else "Light Mode") } ,
							onCheckedChange = {
								viewModelSettings.handleEvent(
										SettingsViewModel.SettingsEvent.DarkMode(
												it
																				)
															 )
							} ,
							icon = {
								if (stateDarkMode.value)
								{
									Icon(
											modifier = Modifier.size(24.dp) ,
											painter = painterResource(id = R.drawable.dark_icon) ,
											contentDescription = "Dark Mode"
										)
								} else
								{
									Icon(
											modifier = Modifier.size(24.dp) ,
											painter = painterResource(id = R.drawable.light_icon) ,
											contentDescription = "Light Mode"
										)
								}
							}
								  )
				}
			}
			//theme
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth()
						) {
				SettingsList(
						onChange = {
							viewModelSettings.handleEvent(SettingsViewModel.SettingsEvent.Theme(it))
						} ,
						height = 250.dp ,
						icon = {
							Icon(
									modifier = Modifier.size(24.dp) ,
									painter = painterResource(id = R.drawable.theme_icon) ,
									contentDescription = "Theme"
								)
						} ,
						iconPainter = painterResource(id = R.drawable.theme_icon) ,
						iconDescription = "Theme" ,
						useSelectedValueAsSubtitle = false ,
						valueState = stateOfTheme ,
						title =
						when (stateOfTheme.value)
						{
							"DEFAULT" -> "App Default"
							"Raisin_Black" -> "Raisin Black"
							"Dark_Red" -> "Burgundy"
							"Dark_Liver" -> "Dark Liver"
							"Rustic_brown" -> "Rustic Brown"
							"SYSTEM" -> "System Default"
							"DYNAMIC" -> "Dynamic"
							else -> "App Default"
						} ,
						items = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) themeMapForDynamic else themeMapForNonDynamic ,
							)
			}
		}

		SettingsGroup(title = { Text(text = "Alarm and Notifications") }) {
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth()
						) {
				SettingsMenuLink(
						title = { Text(text = "Force Reset Alarms") } ,
						onClick = {
							sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK , false)
							val alarmLock =
								sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK , false)
							if (! alarmLock)
							{
								CreateAlarms().exact(
										context ,
										fajrTime.value !! ,
										sunriseTime.value !! ,
										dhuhrTime.value !! ,
										asrTime.value !! ,
										maghribTime.value !! ,
										ishaTime.value !! ,
													)
								sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK , true)
							}
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
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth()
						) {
				SettingsMenuLink(
						title = { Text(text = "Test Alarm") } ,
						//we are goping to set the alarm in next 10 seconds
						subtitle = { Text(text = "Alarm will be set in 10 seconds") } ,
						onClick = {
							CoroutineScope(Dispatchers.IO).launch {
								val zuharAdhan =
									"android.resource://" + context.packageName + "/" + R.raw.zuhar
								//create notification channels
								val notificationHelper = NotificationHelper()
								//test channel
								notificationHelper.createNotificationChannel(
										context ,
										NotificationManager.IMPORTANCE_MAX ,
										true ,
										CHANNEL_TEST ,
										CHANNEL_DESC_TEST ,
										TEST_CHANNEL_ID ,
										zuharAdhan
																			)
								val currentTime = LocalDateTime.now()
								val timeToNotify =
									currentTime.plusSeconds(10).atZone(ZoneId.systemDefault())
										.toInstant().toEpochMilli()
								val testPendingIntent = CreateAlarms().createPendingIntent(
										context ,
										TEST_PI_REQUEST_CODE ,
										TEST_NOTIFY_ID ,
										timeToNotify ,
										"Test Adhan" ,
										TEST_CHANNEL_ID
																						  )
								Alarms().setExactAlarm(context , timeToNotify , testPendingIntent)
							}
							Toasty.success(
									context ,
									"Test Alarm set" ,
									Toast.LENGTH_SHORT ,
									true
										  )
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
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth()
						) {
				NotificationScreenUI()
			}

			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(8.dp)
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
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth()
						) {
				BatteryExemptionUI()
			}
		}

		SettingsGroup(title = { Text(text = "Legal") }) {
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth()
						) {
				SettingsMenuLink(
						title = { Text(text = "Privacy Policy") } ,
						onClick = {
							onNavigateToWebViewScreen("privacy_policy")
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
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth()
						) {
				SettingsMenuLink(
						title = { Text(text = "Terms and Conditions") } ,
						onClick = {
							onNavigateToWebViewScreen("terms_of_service")
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
				shape = MaterialTheme.shapes.extraLarge ,
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
					) {
			SettingsMenuLink(
					title = { Text(text = "Help") } ,
					onClick = {
						onNavigateToWebViewScreen("help")
					} ,
					icon = {
						Icon(
								modifier = Modifier.size(24.dp) ,
								painter = painterResource(id = R.drawable.help_icon) ,
								contentDescription = "Help documentation"
							)
					} ,
							)
		}

		ElevatedCard(
				shape = MaterialTheme.shapes.extraLarge ,
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
					) {
			SettingsMenuLink(
					title = { Text(text = "License & Acknowledgements") } ,
					//version of the app
					subtitle = { Text(text = "Open source libraries") } ,
					onClick = {
						onNavigateToLicencesScreen()
					} ,
					icon = {
						Icon(
								modifier = Modifier.size(24.dp) ,
								painter = painterResource(id = R.drawable.license_icon) ,
								contentDescription = "License & Acknowledgements"
							)
					} ,
							)
		}

		ElevatedCard(
				shape = MaterialTheme.shapes.extraLarge ,
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
					.testTag(TEST_TAG_ABOUT)
					) {
			SettingsMenuLink(
					title = { Text(text = "About") } ,
					//version of the app
					subtitle = { Text(text = updateAvailableText) } ,
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
					action = {
						if (updateAvailabile.value)
						{
							Button(
									onClick = {
										viewModelSettings.handleEvent(
												SettingsViewModel.SettingsEvent.CheckUpdate(
														context ,
														true
																						   )
																	 )
									} ,
								  ) {
								Text(text = "Update")
							}
						}
					}
							)
		}

		//get the current year
		val currentYear = LocalDateTime.now().year
		Text(
				text = "© $currentYear Nimaz " + BuildConfig.VERSION_NAME ,
				modifier = Modifier
					.padding(8.dp)
					.align(Alignment.CenterHorizontally) ,
				style = MaterialTheme.typography.bodyMedium
			)
	}
}