package com.arshadshah.nimaz.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.SettingsOption
import com.arshadshah.nimaz.ui.components.settings.LocationSettings
import com.arshadshah.nimaz.ui.components.settings.SettingsFooterSection
import com.arshadshah.nimaz.ui.components.settings.SettingsPermissionsSection
import com.arshadshah.nimaz.ui.components.settings.SettingsPrayerTimesSection
import com.arshadshah.nimaz.ui.components.settings.getAppID
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import com.google.android.play.core.review.ReviewManagerFactory
import es.dmoral.toasty.Toasty


// Theme options with descriptions and dynamic colors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToPrayerTimeCustomizationScreen: () -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onNavigateToWebViewScreen: (String) -> Unit,
    onNavigateToLicencesScreen: () -> Unit,
    onNavigateToDebugScreen: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    activity: MainActivity,
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val locationSettingsState by viewModel.locationSettingsState.collectAsState()

    // Check for updates on launch
    LaunchedEffect(Unit) {
        viewModel.handleEvent(SettingsViewModel.SettingsEvent.CheckUpdate(activity))
    }

    val scrollState = rememberScrollState()
    val isDebugMode = remember {
        PrivateSharedPreferences(context).getDataBoolean(AppConstants.DEBUG_MODE, false)
    }

    // Scroll behavior for collapsing toolbar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
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
                scrollBehavior = scrollBehavior,
                modifier = Modifier.shadow(4.dp)
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

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                SettingsOption(
                    icon = Icons.Default.ColorLens,
                    title = "Appearance",
                    description = "Customize the look and feel of the app",
                    onClick = onNavigateToAppearance
                )
            }


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
