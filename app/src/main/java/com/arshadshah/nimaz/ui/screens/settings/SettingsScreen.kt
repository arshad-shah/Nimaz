package com.arshadshah.nimaz.ui.screens.settings

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.arshadshah.nimaz.constants.AppConstants.THEME_DARK_RED
import com.arshadshah.nimaz.constants.AppConstants.THEME_DEFAULT
import com.arshadshah.nimaz.constants.AppConstants.THEME_RAISIN_BLACK
import com.arshadshah.nimaz.constants.AppConstants.THEME_RUSTIC_BROWN
import com.arshadshah.nimaz.constants.AppConstants.THEME_SYSTEM
import com.arshadshah.nimaz.ui.components.common.BatteryExemptionUI
import com.arshadshah.nimaz.ui.components.common.NotificationScreenUI
import com.arshadshah.nimaz.ui.components.settings.LocationSettings
import com.arshadshah.nimaz.ui.components.settings.Option
import com.arshadshah.nimaz.ui.components.settings.SettingsGroup
import com.arshadshah.nimaz.ui.components.settings.SettingsMenuLink
import com.arshadshah.nimaz.ui.components.settings.SettingsSwitch
import com.arshadshah.nimaz.ui.components.settings.ThemeGrid
import com.arshadshah.nimaz.ui.components.settings.ThemeOption
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceStringSettingState
import com.arshadshah.nimaz.ui.theme.Dark_Red_md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.Dark_Red_md_theme_light_primary
import com.arshadshah.nimaz.ui.theme.md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.md_theme_light_primary
import com.arshadshah.nimaz.ui.theme.raison_black_md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.raison_black_md_theme_light_primary
import com.arshadshah.nimaz.ui.theme.rustic_md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.rustic_md_theme_light_primary
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.Alarms
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import com.google.android.play.core.review.ReviewManagerFactory
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
	onNavigateToDebugScreen : () -> Unit ,
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

	val isDebugMode = remember {
		sharedPreferences.getDataBoolean(AppConstants.DEBUG_MODE , false)
	}

	val isSelectedTheme = remember {
		mutableStateOf(
				 ThemeOption(
						  themeName = "App Default" ,
						  themeKey = THEME_DEFAULT ,
						  themeColor = if (isDarkMode.value) md_theme_dark_primary else md_theme_light_primary ,
						  isSelected = themeState.value == THEME_DEFAULT
							)
					  )
	}


	val themeOptionsList =
		listOf(
				 ThemeOption(
						  themeName = "Forest Green" ,
						  themeKey = THEME_DEFAULT ,
						  themeColor = if (isDarkMode.value) md_theme_dark_primary else md_theme_light_primary ,
						  isSelected = themeState.value == THEME_DEFAULT
							) ,
				 ThemeOption(
						  themeName = "Raisin Black" ,
						  themeKey = THEME_RAISIN_BLACK ,
						  themeColor = if (isDarkMode.value) raison_black_md_theme_light_primary else raison_black_md_theme_dark_primary ,
						  isSelected = themeState.value == THEME_RAISIN_BLACK
							) ,
				 ThemeOption(
						  themeName = "Burgundy" ,
						  themeKey = THEME_DARK_RED ,
						  themeColor = if (isDarkMode.value) Dark_Red_md_theme_dark_primary else Dark_Red_md_theme_light_primary ,
						  isSelected = themeState.value == THEME_DARK_RED
							) ,
				 ThemeOption(
						  themeName = "Rustic Brown" ,
						  themeKey = THEME_RUSTIC_BROWN ,
						  themeColor = if (isDarkMode.value) rustic_md_theme_dark_primary else rustic_md_theme_light_primary ,
						  isSelected = themeState.value == THEME_RUSTIC_BROWN
							) ,
				 ThemeOption(
						  themeName = "System" ,
						  themeKey = THEME_SYSTEM ,
						  themeColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
						  {
							  if (isDarkMode.value) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(
									   context
																														).primary
						  } else
						  {
							  if (isDarkMode.value) md_theme_dark_primary else md_theme_light_primary
						  } ,
						  isSelected = themeState.value == THEME_SYSTEM
							) ,
			  )

	Column(
			 modifier = Modifier
				 .verticalScroll(rememberScrollState() , true)
				 .padding(paddingValues)
				 .testTag(AppConstants.TEST_TAG_SETTINGS)
		  ) {
		LocationSettings()

		ElevatedCard(
				 colors = CardDefaults.elevatedCardColors(
						  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
								   elevation = 32.dp
																							) ,
						  contentColor = MaterialTheme.colorScheme.onSurface ,
						  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
						  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
														 ) ,
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
					 action = {
						 Icon(
								  modifier = Modifier
									  .size(24.dp)
									  .padding(2.dp) ,
								  painter = painterResource(id = R.drawable.angle_right_icon) ,
								  contentDescription = "Update Available"
							 )
					 }
							)
		}

		val stateOfTheme =
			rememberPreferenceStringSettingState(key = THEME , defaultValue = themeState.value)

		stateOfTheme.value = themeState.value

		val stateDarkMode =
			rememberPreferenceBooleanSettingState(
					 DARK_MODE ,
					 false
												 )
		stateDarkMode.value = isDarkMode.value

		SettingsGroup(
				 title = { Text(text = "Appearance") } ,
					 ) {
			AnimatedVisibility(visible = stateOfTheme.value != "SYSTEM") {
				ElevatedCard(
						 colors = CardDefaults.elevatedCardColors(
								  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
										   elevation = 32.dp
																									) ,
								  contentColor = MaterialTheme.colorScheme.onSurface ,
								  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(
										   alpha = 0.38f
																								 ) ,
								  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(
										   alpha = 0.38f
																								 ) ,
																 ) ,
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
								 Crossfade(
										  targetState = stateDarkMode.value ,
										  label = "themeModeChange"
										  ) { darkMode ->
									 if (darkMode)
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
							 }
								  )
				}
			}
			//theme
			ElevatedCard(
					 colors = CardDefaults.elevatedCardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
									   elevation = 32.dp
																								) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
															 ) ,
					 shape = MaterialTheme.shapes.extraLarge ,
					 modifier = Modifier
						 .padding(8.dp)
						 .fillMaxWidth()
						) {

				ThemeGrid(
						 themeOptions = themeOptionsList ,
						 onThemeOptionSelected = {
							 //set current selected theme to false
							 isSelectedTheme.value.isSelected = ! isSelectedTheme.value.isSelected
							 isSelectedTheme.value = themeOptionsList[themeOptionsList.indexOf(it)]
							 isSelectedTheme.value.isSelected = ! isSelectedTheme.value.isSelected
							 viewModelSettings.handleEvent(
									  SettingsViewModel.SettingsEvent.Theme(
											   isSelectedTheme.value.themeKey
																		   )
														  )
						 }
						 )
			}
		}

		SettingsGroup(title = { Text(text = "Alarm and Notifications") }) {
			ElevatedCard(
					 colors = CardDefaults.elevatedCardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
									   elevation = 32.dp
																								) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
															 ) ,
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
					 colors = CardDefaults.elevatedCardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
									   elevation = 32.dp
																								) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
															 ) ,
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
					 colors = CardDefaults.elevatedCardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
									   elevation = 32.dp
																								) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
															 ) ,
					 shape = MaterialTheme.shapes.extraLarge ,
					 modifier = Modifier
						 .padding(8.dp)
						 .fillMaxWidth()
						) {
				NotificationScreenUI()
			}

			ElevatedCard(
					 colors = CardDefaults.elevatedCardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
									   elevation = 32.dp
																								) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
															 ) ,
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
						 action = {
							 Icon(
									  modifier = Modifier
										  .size(24.dp)
										  .padding(2.dp) ,
									  painter = painterResource(id = R.drawable.angle_right_icon) ,
									  contentDescription = "Update Available"
								 )
						 }
								)
			}

			ElevatedCard(
					 colors = CardDefaults.elevatedCardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
									   elevation = 32.dp
																								) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
															 ) ,
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
					 colors = CardDefaults.elevatedCardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
									   elevation = 32.dp
																								) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
															 ) ,
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
						 action = {
							 Icon(
									  modifier = Modifier
										  .size(24.dp)
										  .padding(2.dp) ,
									  painter = painterResource(id = R.drawable.angle_right_icon) ,
									  contentDescription = "Update Available"
								 )
						 }
								)
			}

			ElevatedCard(
					 colors = CardDefaults.elevatedCardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
									   elevation = 32.dp
																								) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
															 ) ,
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
						 action = {
							 Icon(
									  modifier = Modifier
										  .size(24.dp)
										  .padding(2.dp) ,
									  painter = painterResource(id = R.drawable.angle_right_icon) ,
									  contentDescription = "Update Available"
								 )
						 }
								)
			}
		}
		SettingsGroup(title = { Text(text = "Other") }) {
			Option(title = { Text(text = "Help") } ,
				   onClick = {
					   onNavigateToWebViewScreen("help")
				   } ,
				   icon = painterResource(id = R.drawable.help_icon) ,
				   iconDescription = "Help documentation" ,
				   testTag = TEST_TAG_ABOUT) {
				Icon(
						 modifier = Modifier
							 .size(24.dp)
							 .padding(2.dp) ,
						 painter = painterResource(id = R.drawable.angle_right_icon) ,
						 contentDescription = "Update Available"
					)
			}

			Option(
					 title = {
						 Text(
								  text = "License & Acknowledgements" ,
								  maxLines = 1 ,
								  overflow = TextOverflow.Ellipsis
							 )
					 } ,
					 subtitle = { Text(text = "Open source libraries") } ,
					 onClick = { onNavigateToLicencesScreen() } ,
					 icon = painterResource(id = R.drawable.license_icon) ,
					 iconDescription = "License & Acknowledgements" ,
					 testTag = TEST_TAG_ABOUT) {
				Icon(
						 modifier = Modifier
							 .size(24.dp)
							 .padding(2.dp) ,
						 painter = painterResource(id = R.drawable.angle_right_icon) ,
						 contentDescription = "Update Available"
					)
			}

			Option(title = { Text(text = "Rate Nimaz") } ,
				   onClick = {
					   val manager = ReviewManagerFactory.create(context)
					   val request = manager.requestReviewFlow()
					   request.addOnCompleteListener { task ->
						   if (task.isSuccessful)
						   {
							   manager.launchReviewFlow(context as Activity , task.result)
								   .addOnCompleteListener { _ ->

								   }
						   } else
						   {
							   // There was some problem, log or handle the error code.
							   Toasty.error(
										context ,
										task.exception?.message ?: "Error" ,
										Toast.LENGTH_SHORT
										   )
								   .show()
						   }
					   }
				   } ,
				   icon = painterResource(id = R.drawable.rating_icon) ,
				   iconDescription = "Rate Nimaz" ,
				   testTag = TEST_TAG_ABOUT)

			Option(
					 title = { Text(text = "Share Nimaz") } ,
					 onClick = {
						 val shareIntent = Intent(Intent.ACTION_SEND)
						 shareIntent.type = "text/plain"
						 shareIntent.putExtra(Intent.EXTRA_SUBJECT , "Nimaz")
						 var shareMessage = "\nCheck out this app\n\n"
						 shareMessage = """
								${shareMessage}https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}
								
								""".trimIndent()
						 shareIntent.putExtra(Intent.EXTRA_TEXT , shareMessage)
						 context.startActivity(Intent.createChooser(shareIntent , "choose one"))
					 } ,
					 icon = painterResource(id = R.drawable.share_icon) ,
					 iconDescription = "Share Nimaz" ,
					 testTag = TEST_TAG_ABOUT)

			Option(
					 title = { Text(text = "About") } ,
					 subtitle = { Text(text = updateAvailableText) } ,
					 onClick = { onNavigateToAboutScreen() } ,
					 icon = painterResource(id = R.drawable.info_icon) ,
					 iconDescription = "About" ,
					 testTag = TEST_TAG_ABOUT
				  ) {
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
				} else
				{
					Icon(
							 modifier = Modifier
								 .size(24.dp)
								 .padding(2.dp) ,
							 painter = painterResource(id = R.drawable.angle_right_icon) ,
							 contentDescription = "Update Available"
						)
				}
			}

			if (isDebugMode)
			{
				Option(
						 title = { Text(text = "Debug Tools") } ,
						 subtitle = { Text(text = "For testing purposes only") } ,
						 onClick = { onNavigateToDebugScreen() } ,
						 icon = painterResource(id = R.drawable.debug_icon) ,
						 iconDescription = "Debug Tools" ,
						 testTag = TEST_TAG_ABOUT ,
						 action = {
							 Icon(
									  modifier = Modifier
										  .size(24.dp)
										  .padding(2.dp) ,
									  painter = painterResource(id = R.drawable.angle_right_icon) ,
									  contentDescription = "Go to Debug Tools"
								 )
						 }
					  )
			}
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