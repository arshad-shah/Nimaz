package com.arshadshah.nimaz.ui.screens.settings

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.CHANNEL_DESC_TEST
import com.arshadshah.nimaz.constants.AppConstants.CHANNEL_TEST
import com.arshadshah.nimaz.constants.AppConstants.TEST_CHANNEL_ID
import com.arshadshah.nimaz.constants.AppConstants.TEST_NOTIFY_ID
import com.arshadshah.nimaz.constants.AppConstants.TEST_PI_REQUEST_CODE
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_ABOUT
import com.arshadshah.nimaz.constants.AppConstants.THEME_DARK_RED
import com.arshadshah.nimaz.constants.AppConstants.THEME_DEFAULT
import com.arshadshah.nimaz.constants.AppConstants.THEME_RAISIN_BLACK
import com.arshadshah.nimaz.constants.AppConstants.THEME_RUSTIC_BROWN
import com.arshadshah.nimaz.constants.AppConstants.THEME_SYSTEM
import com.arshadshah.nimaz.ui.components.common.BannerDuration
import com.arshadshah.nimaz.ui.components.common.BannerLarge
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.ui.components.common.BatteryExemptionUI
import com.arshadshah.nimaz.ui.components.common.NotificationScreenUI
import com.arshadshah.nimaz.ui.components.settings.LocationSettings
import com.arshadshah.nimaz.ui.components.settings.Option
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.settings.SettingsGroup
import com.arshadshah.nimaz.ui.components.settings.SettingsMenuLink
import com.arshadshah.nimaz.ui.components.settings.SettingsSwitch
import com.arshadshah.nimaz.ui.components.settings.ThemeOption
import com.arshadshah.nimaz.ui.components.settings.ThemeSelector
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
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import com.google.android.play.core.review.ReviewManagerFactory
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToPrayerTimeCustomizationScreen: () -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onNavigateToWebViewScreen: (String) -> Unit,
    onNavigateToLicencesScreen: () -> Unit,
    onNavigateToDebugScreen: () -> Unit,
    activity: MainActivity,
    navController: NavHostController,
    viewModelSettings: SettingsViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModelSettings.handleEvent(SettingsViewModel.SettingsEvent.LoadSettings)
        viewModelSettings.handleEvent(SettingsViewModel.SettingsEvent.CheckUpdate(activity))
    }

    val uiState by viewModelSettings.uiState.collectAsState()
    val locationState by viewModelSettings.locationState.collectAsState()

    val updateAvailableText = if (uiState.updateAvailable) {
        "Update Available"
    } else {
        "Nimaz is up to date"
    }

    val prayerTimesState = viewModelSettings.prayerTimesState.collectAsState(initial = null)

    val fajrTime = prayerTimesState.value?.fajrTime ?: LocalDateTime.now()

    val sunriseTime = prayerTimesState.value?.sunriseTime ?: LocalDateTime.now()

    val dhuhrTime = prayerTimesState.value?.dhuhrTime ?: LocalDateTime.now()

    val asrTime = prayerTimesState.value?.asrTime ?: LocalDateTime.now()

    val maghribTime = prayerTimesState.value?.maghribTime ?: LocalDateTime.now()

    val ishaTime = prayerTimesState.value?.ishaTime ?: LocalDateTime.now()

    val sharedPreferences = PrivateSharedPreferences(context)

    val isDebugMode = remember {
        sharedPreferences.getDataBoolean(AppConstants.DEBUG_MODE, false)
    }

    val isSelectedTheme = remember {
        mutableStateOf(
            ThemeOption(
                themeName = "App Default",
                themeKey = THEME_SYSTEM,
                themeColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (uiState.isDarkMode) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(
                        context
                    ).primary
                } else {
                    if (uiState.isDarkMode) md_theme_dark_primary else md_theme_light_primary
                },
                isSelected = uiState.theme == THEME_SYSTEM
            )
        )
    }


    val themeOptionsList = getThemeOptions(context, uiState.isDarkMode, uiState.theme)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Settings")
                },
                navigationIcon = {
                    OutlinedIconButton(
                        modifier = Modifier
                            .testTag("backButton")
                            .padding(start = 8.dp),
                        onClick = {
                            navController.popBackStack()
                        }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState(), true)
                .padding(it)
                .testTag(AppConstants.TEST_TAG_SETTINGS)
        ) {
            if (uiState.error != "") {
                val errorBannerOpen = remember { mutableStateOf(true) }
                Log.d("SettingsScreen", "SettingsScreen: ${uiState.error}")
                BannerLarge(
                    title = "Error",
                    variant = BannerVariant.Error,
                    showFor = BannerDuration.FOREVER.value,
                    isOpen = errorBannerOpen,
                    message = uiState.error,
                    onDismiss = {},
                )
            }
            LocationSettings(
                isLoading = uiState.isLoading,
                locationState = locationState,
                onToggleLocation = { enabled ->
                    viewModelSettings.handleEvent(
                        SettingsViewModel.SettingsEvent.LocationToggle(
                            context,
                            enabled
                        )
                    )
                },
                onLocationInput = { locationName: String ->
                    viewModelSettings.handleEvent(
                        SettingsViewModel.SettingsEvent.LocationInput(
                            context,
                            locationName
                        )
                    )
                }
            )

            SettingsGroup(title = { Text(text = "Prayer Times") }) {
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsMenuLink(
                        title = { Text(text = "Prayer Times") },
                        onClick = onNavigateToPrayerTimeCustomizationScreen,
                        icon = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.settings_sliders_icon),
                                contentDescription = "Prayer Times settings"
                            )
                        },
                        action = {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(2.dp),
                                painter = painterResource(id = R.drawable.angle_right_icon),
                                contentDescription = "Update Available"
                            )
                        }
                    )
                }

                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsMenuLink(
                        title = { Text(text = "Force Reset Alarms") },
                        onClick = {
                            sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, false)
                            val alarmLock =
                                sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK, false)
                            if (!alarmLock) {
                                CreateAlarms().exact(
                                    context,
                                    fajrTime!!,
                                    sunriseTime!!,
                                    dhuhrTime!!,
                                    asrTime!!,
                                    maghribTime!!,
                                    ishaTime!!,
                                )
                                sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, true)
                            }
                            Toasty.success(context, "Alarms Reset", Toast.LENGTH_SHORT, true)
                                .show()
                        },
                        icon = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.alarm_clock_icon),
                                contentDescription = "Notifications"
                            )
                        },
                    )
                }

                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsMenuLink(
                        title = { Text(text = "Test Alarm") },
                        //we are goping to set the alarm in next 10 seconds
                        subtitle = { Text(text = "Alarm will be set in 10 seconds") },
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                val zuharAdhan =
                                    "android.resource://" + context.packageName + "/" + R.raw.zuhar
                                //create notification channels
                                val notificationHelper = NotificationHelper()
                                //test channel
                                notificationHelper.createNotificationChannel(
                                    context,
                                    NotificationManager.IMPORTANCE_MAX,
                                    true,
                                    CHANNEL_TEST,
                                    CHANNEL_DESC_TEST,
                                    TEST_CHANNEL_ID,
                                    zuharAdhan
                                )
                                val currentTime = LocalDateTime.now()
                                val timeToNotify =
                                    currentTime.plusSeconds(10).atZone(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli()
                                val testPendingIntent = CreateAlarms().createPendingIntent(
                                    context,
                                    TEST_PI_REQUEST_CODE,
                                    TEST_NOTIFY_ID,
                                    timeToNotify,
                                    "Test Adhan",
                                    TEST_CHANNEL_ID
                                )
                                Alarms().setExactAlarm(context, timeToNotify, testPendingIntent)
                            }
                            Toasty.success(
                                context,
                                "Test Alarm set",
                                Toast.LENGTH_SHORT,
                                true
                            )
                                .show()
                        },
                        icon = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.alarm_set_icon),
                                contentDescription = "Back"
                            )
                        },
                    )
                }

                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsMenuLink(
                        title = { Text(text = "Notification Settings") },
                        subtitle = { Text(text = "Settings for all the Adhan") },
                        onClick = {
                            //open the notification settings
                            val intent = Intent()
                            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                            intent.putExtra(
                                "android.provider.extra.APP_PACKAGE",
                                context.packageName
                            )
                            context.startActivity(intent)
                        },
                        icon = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.settings_icon),
                                contentDescription = "Settings for notification"
                            )
                        },
                        action = {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(2.dp),
                                painter = painterResource(id = R.drawable.angle_right_icon),
                                contentDescription = "Update Available"
                            )
                        }
                    )
                }

            }

            SettingsGroup(
                title = { Text(text = "Appearance") },
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    //switch for theme mode dark/light when its not dynamic
                    SettingsSwitch(
                        state = createBooleanState(uiState.isDarkMode),
                        title = { Text(text = if (uiState.isDarkMode) "Dark Mode" else "Light Mode") },
                        onCheckedChange = { isDarkMode ->
                            viewModelSettings.handleEvent(
                                SettingsViewModel.SettingsEvent.DarkMode(
                                    isDarkMode
                                )
                            )
                        },
                        icon = {
                            Crossfade(
                                targetState = uiState.isDarkMode,
                                label = "themeModeChange"
                            ) { darkMode ->
                                if (darkMode) {
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        painter = painterResource(id = R.drawable.dark_icon),
                                        contentDescription = "Dark Mode"
                                    )
                                } else {
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        painter = painterResource(id = R.drawable.light_icon),
                                        contentDescription = "Light Mode"
                                    )
                                }
                            }
                        }
                    )
                }
                //theme
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {

                    ThemeSelector(
                        themeOptions = themeOptionsList,
                        onThemeOptionSelected = {
                            //set current selected theme to false
                            isSelectedTheme.value.isSelected = !isSelectedTheme.value.isSelected
                            isSelectedTheme.value = themeOptionsList[themeOptionsList.indexOf(it)]
                            isSelectedTheme.value.isSelected = !isSelectedTheme.value.isSelected
                            viewModelSettings.handleEvent(
                                SettingsViewModel.SettingsEvent.Theme(
                                    isSelectedTheme.value.themeKey
                                )
                            )
                        }
                    )
                }
            }

            SettingsGroup(title = { Text(text = "Permissions") }) {

                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    NotificationScreenUI(
                        areNotificationsAllowed = locationState.areNotificationsAllowed,
                        onNotificationsAllowedChange = { notificationsAllowed: Boolean ->
                            viewModelSettings.handleEvent(
                                SettingsViewModel.SettingsEvent.NotificationsAllowed(
                                    notificationsAllowed
                                )
                            )
                        }
                    )
                }
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    BatteryExemptionUI(
                        isBatteryExempt = locationState.isBatteryExempt,
                        onBatteryExemptChange = { it: Boolean ->
                            viewModelSettings.handleEvent(
                                SettingsViewModel.SettingsEvent.BatteryExempt(
                                    it
                                )
                            )
                        }
                    )
                }
            }

            SettingsGroup(title = { Text(text = "Legal") }) {
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsMenuLink(
                        title = { Text(text = "Privacy Policy") },
                        onClick = {
                            onNavigateToWebViewScreen("privacy_policy")
                        },
                        icon = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.privacy_policy_icon),
                                contentDescription = "Privacy Policy"
                            )
                        },
                        action = {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(2.dp),
                                painter = painterResource(id = R.drawable.angle_right_icon),
                                contentDescription = "Update Available"
                            )
                        }
                    )
                }

                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    SettingsMenuLink(
                        title = { Text(text = "Terms and Conditions") },
                        onClick = {
                            onNavigateToWebViewScreen("terms_of_service")
                        },
                        icon = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.document_icon),
                                contentDescription = "Privacy Policy"
                            )
                        },
                        action = {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(2.dp),
                                painter = painterResource(id = R.drawable.angle_right_icon),
                                contentDescription = "Update Available"
                            )
                        }
                    )
                }
            }
            SettingsGroup(title = { Text(text = "Other") }) {
                Option(
                    title = { Text(text = "Help") },
                    onClick = {
                        onNavigateToWebViewScreen("help")
                    },
                    icon = painterResource(id = R.drawable.help_icon),
                    iconDescription = "Help documentation",
                    testTag = TEST_TAG_ABOUT
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(2.dp),
                        painter = painterResource(id = R.drawable.angle_right_icon),
                        contentDescription = "Update Available"
                    )
                }

                Option(
                    title = {
                        Text(
                            text = "License & Acknowledgements",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    subtitle = { Text(text = "Open source libraries") },
                    onClick = { onNavigateToLicencesScreen() },
                    icon = painterResource(id = R.drawable.license_icon),
                    iconDescription = "License & Acknowledgements",
                    testTag = TEST_TAG_ABOUT
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(2.dp),
                        painter = painterResource(id = R.drawable.angle_right_icon),
                        contentDescription = "Update Available"
                    )
                }

                Option(
                    title = { Text(text = "Rate Nimaz") },
                    onClick = {
                        val manager = ReviewManagerFactory.create(context)
                        val request = manager.requestReviewFlow()
                        request.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                manager.launchReviewFlow(context as Activity, task.result)
                                    .addOnCompleteListener { _ ->
                                    }
                            } else {
                                // There was some problem, log or handle the error code.
                                Toasty.error(
                                    context,
                                    task.exception?.message ?: "Error",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    },
                    icon = painterResource(id = R.drawable.rating_icon),
                    iconDescription = "Rate Nimaz",
                    testTag = TEST_TAG_ABOUT
                )

                Option(
                    title = { Text(text = "Share Nimaz") },
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Nimaz")
                        var shareMessage = "\nCheck out this app\n\n"
                        shareMessage = """
								${shareMessage}https://play.google.com/store/apps/details?id=${
                            getAppID(
                                context
                            )
                        }
								
								""".trimIndent()
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                        context.startActivity(Intent.createChooser(shareIntent, "choose one"))
                    },
                    icon = painterResource(id = R.drawable.share_icon),
                    iconDescription = "Share Nimaz",
                    testTag = TEST_TAG_ABOUT
                )

                Option(
                    title = { Text(text = "About") },
                    subtitle = { Text(text = updateAvailableText) },
                    onClick = { onNavigateToAboutScreen() },
                    icon = painterResource(id = R.drawable.info_icon),
                    iconDescription = "About",
                    testTag = TEST_TAG_ABOUT
                ) {
                    if (uiState.updateAvailable) {
                        Button(
                            onClick = {
                                viewModelSettings.handleEvent(
                                    SettingsViewModel.SettingsEvent.CheckUpdate(
                                        activity,
                                    )
                                )
                            },
                        ) {
                            Text(text = "Update")
                        }
                    } else {
                        Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(2.dp),
                            painter = painterResource(id = R.drawable.angle_right_icon),
                            contentDescription = "Update Available"
                        )
                    }
                }

                if (isDebugMode) {
                    Option(
                        title = { Text(text = "Debug Tools") },
                        subtitle = { Text(text = "For testing purposes only") },
                        onClick = { onNavigateToDebugScreen() },
                        icon = painterResource(id = R.drawable.debug_icon),
                        iconDescription = "Debug Tools",
                        testTag = TEST_TAG_ABOUT,
                        action = {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(2.dp),
                                painter = painterResource(id = R.drawable.angle_right_icon),
                                contentDescription = "Go to Debug Tools"
                            )
                        }
                    )
                }
            }

            //get the current year
            val currentYear = LocalDateTime.now().year
            Text(
                text = "© $currentYear Nimaz " + getAppVersion(context),
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

fun getAppVersion(context: Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName.toString() // Or use versionCode based on your need
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}


fun getAppID(context: Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.packageName // Or use versionCode based on your need
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

// Theme options with descriptions and dynamic colors
@Composable
fun getThemeOptions(
    context: Context,
    isDarkMode: Boolean,
    themeState: String
): List<ThemeOption> {
    return listOf(
        ThemeOption(
            themeName = "Forest Green",
            themeKey = THEME_DEFAULT,
            themeColor = if (isDarkMode) md_theme_dark_primary else md_theme_light_primary,
            isSelected = themeState == THEME_DEFAULT,
            description = "A calming forest green theme inspired by nature's tranquility"
        ),
        ThemeOption(
            themeName = "Raisin Black",
            themeKey = THEME_RAISIN_BLACK,
            themeColor = if (isDarkMode)
                raison_black_md_theme_light_primary
            else
                raison_black_md_theme_dark_primary,
            isSelected = themeState == THEME_RAISIN_BLACK,
            description = "An elegant darker theme with sophisticated raisin black tones"
        ),
        ThemeOption(
            themeName = "Burgundy",
            themeKey = THEME_DARK_RED,
            themeColor = if (isDarkMode)
                Dark_Red_md_theme_dark_primary
            else
                Dark_Red_md_theme_light_primary,
            isSelected = themeState == THEME_DARK_RED,
            description = "Rich burgundy tones for a classic and timeless appearance"
        ),
        ThemeOption(
            themeName = "Rustic Brown",
            themeKey = THEME_RUSTIC_BROWN,
            themeColor = if (isDarkMode)
                rustic_md_theme_dark_primary
            else
                rustic_md_theme_light_primary,
            isSelected = themeState == THEME_RUSTIC_BROWN,
            description = "Warm rustic brown hues reminiscent of natural earth tones"
        ),
        ThemeOption(
            themeName = "System",
            themeKey = THEME_SYSTEM,
            themeColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDarkMode)
                    dynamicDarkColorScheme(context).primary
                else
                    dynamicLightColorScheme(context).primary
            } else {
                if (isDarkMode)
                    md_theme_dark_primary
                else
                    md_theme_light_primary
                md_theme_light_primary
            },
            isSelected = themeState == THEME_SYSTEM,
            description = "Automatically matches your system's theme preferences"
        )
    )
}

// Add these at the bottom of your file
private fun createValueState(value: String) = object : SettingValueState<String> {
    override var value: String = value
    override fun reset() {}
}

private fun createBooleanState(value: Boolean) = object : SettingValueState<Boolean> {
    override var value: Boolean = value
    override fun reset() {}
}

private fun createDoubleState(value: Double) = object : SettingValueState<Double> {
    override var value: Double = value
    override fun reset() {}
}