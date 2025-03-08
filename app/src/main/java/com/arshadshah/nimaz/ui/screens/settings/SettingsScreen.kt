package com.arshadshah.nimaz.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.THEME_DARK_RED
import com.arshadshah.nimaz.constants.AppConstants.THEME_DEFAULT
import com.arshadshah.nimaz.constants.AppConstants.THEME_RAISIN_BLACK
import com.arshadshah.nimaz.constants.AppConstants.THEME_RUSTIC_BROWN
import com.arshadshah.nimaz.constants.AppConstants.THEME_SYSTEM
import com.arshadshah.nimaz.ui.components.settings.LocationSettings
import com.arshadshah.nimaz.ui.components.settings.SettingsAppearanceSection
import com.arshadshah.nimaz.ui.components.settings.SettingsFooterSection
import com.arshadshah.nimaz.ui.components.settings.SettingsPermissionsSection
import com.arshadshah.nimaz.ui.components.settings.SettingsPrayerTimesSection
import com.arshadshah.nimaz.ui.components.settings.ThemeOption
import com.arshadshah.nimaz.ui.components.settings.getAppID
import com.arshadshah.nimaz.ui.theme.Dark_Red_md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.Dark_Red_md_theme_light_primary
import com.arshadshah.nimaz.ui.theme.md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.md_theme_light_primary
import com.arshadshah.nimaz.ui.theme.raison_black_md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.raison_black_md_theme_light_primary
import com.arshadshah.nimaz.ui.theme.rustic_md_theme_dark_primary
import com.arshadshah.nimaz.ui.theme.rustic_md_theme_light_primary
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import com.google.android.play.core.review.ReviewManagerFactory
import es.dmoral.toasty.Toasty


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
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val locationSettingsState by viewModel.locationSettingsState.collectAsState()
    val themeName by viewModel.themeName.collectAsState(initial = THEME_SYSTEM)
    val isDarkMode by viewModel.isDarkMode.collectAsState(initial = false)

    // Check for updates on launch
    LaunchedEffect(Unit) {
        viewModel.handleEvent(SettingsViewModel.SettingsEvent.CheckUpdate(activity))
    }

    val scrollState = rememberScrollState()
    val isDebugMode = remember {
        PrivateSharedPreferences(context).getDataBoolean(AppConstants.DEBUG_MODE, false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    OutlinedIconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
        ) {


            LocationSettings(
                location = uiState.location,
                isLoading = uiState.isLoading,
                locationSettingsState = locationSettingsState,
                onToggleLocation = { enabled ->
                    viewModel.handleEvent(
                        SettingsViewModel.SettingsEvent.LocationToggle(context, enabled)
                    )
                },
                onLocationInput = { locationName ->
                    viewModel.handleEvent(
                        SettingsViewModel.SettingsEvent.LocationInput(context, locationName)
                    )
                }
            )


            // Prayer Times Section
            SettingsPrayerTimesSection(
                onNavigateToPrayerSettings = onNavigateToPrayerTimeCustomizationScreen,
                onResetAlarms = {
                    viewModel.handleEvent(SettingsViewModel.SettingsEvent.ForceResetAlarms)
                    Toast.makeText(context, "Alarms Reset", Toast.LENGTH_SHORT).show()
                },
                onTestAlarm = {
                    viewModel.handleEvent(SettingsViewModel.SettingsEvent.SetTestAlarm)
                    Toast.makeText(context, "Test Alarm Set", Toast.LENGTH_SHORT).show()
                },
                onOpenNotificationSettings = {
                    val intent = Intent().apply {
                        action = "android.settings.APP_NOTIFICATION_SETTINGS"
                        putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                    }
                    context.startActivity(intent)
                }
            )

            // Appearance Section
            SettingsAppearanceSection(
                isDarkMode = isDarkMode,
                onDarkModeChange = { newValue ->
                    viewModel.handleEvent(SettingsViewModel.SettingsEvent.DarkMode(newValue))
                },
                currentTheme = themeName,
                themeOptions = getThemeOptions(context, isDarkMode, themeName),
                onThemeSelect = { theme ->
                    viewModel.handleEvent(SettingsViewModel.SettingsEvent.Theme(theme.themeKey))
                }
            )

            // Permissions Section
            SettingsPermissionsSection(
                areNotificationsAllowed = locationSettingsState.areNotificationsAllowed,
                onNotificationsAllowedChange = { allowed ->
                    viewModel.handleEvent(
                        SettingsViewModel.SettingsEvent.NotificationsAllowed(allowed)
                    )
                },
                isBatteryExempt = locationSettingsState.isBatteryExempt,
                onBatteryExemptChange = { exempt ->
                    viewModel.handleEvent(SettingsViewModel.SettingsEvent.BatteryExempt(exempt))
                }
            )

            // Footer Section with Legal, About, etc.
            SettingsFooterSection(
                onNavigateToPrivacyPolicy = { onNavigateToWebViewScreen("privacy_policy") },
                onNavigateToTerms = { onNavigateToWebViewScreen("terms_of_service") },
                onNavigateToHelp = { onNavigateToWebViewScreen("help") },
                onNavigateToLicenses = onNavigateToLicencesScreen,
                onRateApp = {
                    val manager = ReviewManagerFactory.create(context)
                    manager.requestReviewFlow().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            manager.launchReviewFlow(activity, task.result)
                        } else {
                            Toasty.error(
                                context,
                                task.exception?.message ?: "Error",
                                Toasty.LENGTH_SHORT,
                                true
                            ).show()
                        }
                    }
                },
                onShareApp = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Nimaz")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Check out this app\n\nhttps://play.google.com/store/apps/details?id=${
                                getAppID(
                                    context
                                )
                            }"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Nimaz"))
                },
                onNavigateToAbout = onNavigateToAboutScreen,
                isUpdateAvailable = uiState.updateAvailable,
                onUpdateApp = {
                    viewModel.handleEvent(SettingsViewModel.SettingsEvent.StartUpdate(activity))
                },
                isDebugMode = isDebugMode,
                onNavigateToDebug = onNavigateToDebugScreen
            )
        }
    }
}
