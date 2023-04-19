package com.arshadshah.nimaz.ui.screens.settings

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import com.arshadshah.nimaz.ui.components.common.BatteryExemptionUI
import com.arshadshah.nimaz.ui.components.common.NotificationScreenUI
import com.arshadshah.nimaz.ui.components.settings.LocationSettings
import com.arshadshah.nimaz.ui.components.settings.SettingsGroup
import com.arshadshah.nimaz.ui.components.settings.SettingsMenuLink
import com.arshadshah.nimaz.ui.components.settings.SettingsSwitch
import com.arshadshah.nimaz.ui.components.settings.internal.SettingsTileIcon
import com.arshadshah.nimaz.ui.components.settings.internal.SettingsTileTexts
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

	val isSelectedTheme = remember {
		mutableStateOf(
				ThemeOption(
						themeName = "App Default" ,
						themeKey = "DEFAULT" ,
						themeColor = if (isDarkMode.value) md_theme_dark_primary else md_theme_light_primary ,
						isSelected = themeState.value == "DEFAULT"
						   )
					  )
	}


	val themeOptionsList =
		listOf(
				ThemeOption(
						themeName = "Forest Green" ,
						themeKey = "DEFAULT" ,
						themeColor = if (isDarkMode.value) md_theme_dark_primary else md_theme_light_primary ,
						isSelected = themeState.value == "DEFAULT"
						   ) ,
				ThemeOption(
						themeName = "Raisin Black" ,
						themeKey = "Raisin_Black" ,
						themeColor = if (isDarkMode.value) raison_black_md_theme_light_primary else raison_black_md_theme_dark_primary ,
						isSelected = themeState.value == "Raisin_Black"
						   ) ,
				ThemeOption(
						themeName = "Burgundy" ,
						themeKey = "Dark_Red" ,
						themeColor = if (isDarkMode.value) Dark_Red_md_theme_dark_primary else Dark_Red_md_theme_light_primary ,
						isSelected = themeState.value == "Dark_Red"
						   ) ,
				ThemeOption(
						themeName = "Rustic Brown" ,
						themeKey = "Rustic_brown" ,
						themeColor = if (isDarkMode.value) rustic_md_theme_dark_primary else rustic_md_theme_light_primary ,
						isSelected = themeState.value == "Rustic_brown"
						   ) ,
				ThemeOption(
						themeName = "System" ,
						themeKey = "SYSTEM" ,
						themeColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
						{
							if (isDarkMode.value) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(
									context
																													  ).primary
						} else
						{
							if (isDarkMode.value) md_theme_dark_primary else md_theme_light_primary
						} ,
						isSelected = themeState.value == "SYSTEM"
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
								Crossfade(targetState = stateDarkMode.value) { darkMode ->
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
				shape = MaterialTheme.shapes.extraLarge ,
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
					) {
			SettingsMenuLink(
					title = {
						Text(
								text = "License & Acknowledgements" ,
								maxLines = 1 ,
								overflow = TextOverflow.Ellipsis
							)
					} ,
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

class ThemeOption(
	var themeName : String ,
	var themeKey : String ,
	var themeColor : Color ,
	var isSelected : Boolean ,
				 )

@Composable
fun ThemeGrid(
	themeOptions : List<ThemeOption> ,
	onThemeOptionSelected : (ThemeOption) -> Unit ,
			 )
{
	Column(
			modifier = Modifier
				.fillMaxWidth()
		  ) {
		//name of current theme
		Row(
				modifier = Modifier
					.fillMaxWidth() ,
				horizontalArrangement = Arrangement.SpaceBetween
		   ) {
			Row(
					modifier = Modifier ,
					verticalAlignment = Alignment.CenterVertically
			   ) {

				SettingsTileIcon(icon = {
					Icon(
							modifier = Modifier
								.size(24.dp) ,
							painter = painterResource(id = R.drawable.theme_icon) ,
							contentDescription = "Color"
						)
				})
				SettingsTileTexts(title = {
					Text(
							text = "Color" ,
						)
				} ,
								  subtitle = {
									  Text(
											  text = themeOptions.find { it.isSelected }?.themeName
												  ?: "" ,
										  )
								  })
			}
		}
		//the row
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp) ,
				horizontalArrangement = Arrangement.SpaceBetween
		   ) {
			themeOptions.forEachIndexed { index , themeOption ->
				//the theme item
				ToggleableCustomThemeItem(
						checked = themeOption.isSelected ,
						onCheckedChange = {
							onThemeOptionSelected(themeOption)
						} ,
						color = themeOption.themeColor ,
						//if the thtme key is SYSTEM then show the system icon
						icon = if (themeOption.themeKey == "SYSTEM")
						{
							@Composable {
								Icon(
										modifier = Modifier.size(24.dp) ,
										painter = painterResource(id = R.drawable.system_icon) ,
										contentDescription = "System" ,
										tint = MaterialTheme.colorScheme.onPrimary
									)
							}
						} else null
										 )
			}
		}
	}
}

//a theme toggle item with a circle that gets highlighted witha  thick border when selected
@Composable
fun ToggleableCustomThemeItem(
	modifier : Modifier = Modifier ,
	checked : Boolean ,
	onCheckedChange : (Boolean) -> Unit ,
	color : Color = MaterialTheme.colorScheme.primary ,
	icon : @Composable (() -> Unit)? = null ,
							 )
{
	Box(
			modifier = modifier
				.border(
						width = 2.dp ,
						color = if (checked) Color.Gray else Color.Transparent ,
						shape = CircleShape
					   ) ,
			contentAlignment = Alignment.Center ,
	   ) {
		//the circle
		Box(

				modifier = modifier
					.padding(6.dp)
					.size(48.dp)
					.clickable {
						onCheckedChange(! checked)
					}
					.background(
							color = color ,
							shape = CircleShape
							   ) ,
				contentAlignment = Alignment.Center
		   ) {
			//the icon
			icon?.invoke()
		}
	}
}


@Preview(showBackground = true)
@Composable
//preview of the theme grid
fun ThemeGridPreview()
{
	ThemeGrid(
			themeOptions = listOf(
					ThemeOption("Light" , "light" , Color.White , true) ,
					ThemeOption("Dark" , "dark" , Color.Black , false) ,
					ThemeOption("Red" , "red" , Color.Red , false) ,
					ThemeOption("Green" , "green" , Color.Green , false) ,
					ThemeOption("Blue" , "blue" , Color.Blue , false) ,
								 ) ,
			onThemeOptionSelected = {}
			 )
}

//preview of ToggleableItemRow
@Preview(showBackground = true , name = "Theme Toggle Selected" , group = "Theme Toggle")
@Composable
fun ToggleableItemRowPreview()
{
	ToggleableCustomThemeItem(
			checked = true ,
			onCheckedChange = {} ,
			color = Color.Red ,
			icon = {
				Icon(
						modifier = Modifier
							.size(24.dp) ,
						painter = painterResource(id = R.drawable.settings_icon) ,
						contentDescription = "Selected"
					)
			}
							 )
}

@Preview(showBackground = true , name = "Theme Toggle Unselected" , group = "Theme Toggle")
@Composable
fun ToggleableItemRowPreview2()
{
	ToggleableCustomThemeItem(
			checked = false ,
			onCheckedChange = {} ,
			color = Color.Red
							 )
}