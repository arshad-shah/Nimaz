package com.arshadshah.nimaz.ui.screens.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.THEME_DARK_RED
import com.arshadshah.nimaz.constants.AppConstants.THEME_DEFAULT
import com.arshadshah.nimaz.constants.AppConstants.THEME_RAISIN_BLACK
import com.arshadshah.nimaz.constants.AppConstants.THEME_RUSTIC_BROWN
import com.arshadshah.nimaz.constants.AppConstants.THEME_SYSTEM
import com.arshadshah.nimaz.ui.components.common.PermissionItem
import com.arshadshah.nimaz.ui.components.settings.CoordinatesView
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
import com.arshadshah.nimaz.utils.FeatureThatRequiresNotificationPermission
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.play.core.review.ReviewManagerFactory
import es.dmoral.toasty.Toasty
import java.time.LocalDateTime

@Composable
fun AppCopyright(
    modifier: Modifier = Modifier,
    appVersion: String = getAppVersion(LocalContext.current)
) {
    val currentYear = remember { LocalDateTime.now().year }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .semantics { contentDescription = "Copyright information" },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    append("© $currentYear ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Nimaz")
                    }
                    append(" • Version: $appVersion")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

//
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

//
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

@Preview
@Composable
fun LocationSettingPreview() {
    LocationSettings(
        location = "Current Location",
        latitude = 0.0,
        longitude = 0.0,
        isLoading = false,
        locationSettingsState = SettingsViewModel.LocationSettingsState(
            isAuto = true,
            areNotificationsAllowed = true,
            isBatteryExempt = true
        ),
        onToggleLocation = {},
        onLocationInput = {}
    )
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationSettings(
    location: String,
    latitude: Double,
    longitude: Double,
    isLoading: Boolean,
    locationSettingsState: SettingsViewModel.LocationSettingsState,
    onToggleLocation: (Boolean) -> Unit,
    onLocationInput: (String) -> Unit,
) {

    val context = LocalContext.current
    val locationInput = remember {
        mutableStateOf(location)
    }

    // Location permission state
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    //custom focus for the text field
    val focusRequester = remember { FocusRequester() }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { onToggleLocation(!locationSettingsState.isAuto) },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.marker_icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Location Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(text = location)
                            }
                        }
                        Switch(
                            checked = locationSettingsState.isAuto,
                            onCheckedChange = {
                                if (it) {
                                    if (locationPermissionState.allPermissionsGranted) {
                                        onToggleLocation(true)
                                    } else {
                                        locationPermissionState.launchMultiplePermissionRequest()
                                    }
                                } else {
                                    onToggleLocation(false)
                                    Toasty.info(
                                        context,
                                        "Please disable location permission for Nimaz in \n Permissions -> Location -> Don't Allow",
                                        Toasty.LENGTH_LONG
                                    ).show()
                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data =
                                                Uri.fromParts("package", context.packageName, null)
                                            addCategory(Intent.CATEGORY_DEFAULT)
                                            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                        }
                                    context.startActivity(intent)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
                AnimatedContent(
                    targetState = locationSettingsState.isAuto,
                    label = "location_mode",
                    transitionSpec = {
                        fadeIn() + slideInVertically() togetherWith
                                fadeOut() + slideOutVertically()
                    }
                ) { isAuto: Boolean ->
                    if (!isAuto) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = locationInput.value,
                                    onValueChange = { locationInput.value = it },
                                    label = { Text("Enter location") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester)
                                        .focusable(),
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Search
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            onLocationInput(locationInput.value)
                                            //remove focus from the text field
                                            focusRequester.freeFocus()
                                        }
                                    )
                                )

                                FilledIconButton(
                                    onClick = {
                                        onLocationInput(locationInput.value)
                                        //remove focus from the text field
                                        focusRequester.freeFocus()
                                    },
                                    modifier = Modifier.size(48.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search location",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }

                CoordinatesView(
                    latitudeState = latitude,
                    longitudeState = longitude,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopSection(
    onNavigateBack: () -> Unit,
) {
    Column {
        LargeTopAppBar(
            title = {
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize your prayer time experience",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("backButton")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_icon),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.shadow(elevation = 0.dp)
        )
    }
}


@Composable
fun SettingsPrayerTimesSection(
    onNavigateToPrayerSettings: () -> Unit,
    onResetAlarms: () -> Unit,
    onTestAlarm: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prayer Times",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilledIconButton(
                    onClick = onNavigateToPrayerSettings,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings_sliders_icon),
                        contentDescription = "Configure prayer times",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Prayer Times Quick Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = R.drawable.alarm_clock_icon,
                    title = "Reset Prayer Alarms",
                    subtitle = "Re-configure all prayer time notifications",
                    onClick = onResetAlarms
                )

                QuickActionButton(
                    icon = R.drawable.alarm_set_icon,
                    title = "Test Notification",
                    subtitle = "Send a test notification in 10 seconds",
                    onClick = onTestAlarm
                )

                QuickActionButton(
                    icon = R.drawable.settings_icon,
                    title = "Notification Settings",
                    subtitle = "Configure system notification settings",
                    onClick = onOpenNotificationSettings,
                    showArrow = true
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showArrow: Boolean = false
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showArrow) {
                Icon(
                    painter = painterResource(id = R.drawable.angle_right_icon),
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@Composable
fun SettingsAppearanceSection(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    currentTheme: String,
    themeOptions: List<ThemeOption>,
    onThemeSelect: (ThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Dark Mode Switch
            Surface(
                onClick = { onDarkModeChange(!isDarkMode) },
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = isDarkMode,
                            label = "theme_icon",
                            transitionSpec = {
                                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                            }
                        ) { dark ->
                            Icon(
                                painter = painterResource(
                                    id = if (dark) R.drawable.dark_icon else R.drawable.light_icon
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = if (isDarkMode) "Dark Mode" else "Light Mode",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // Theme Selection
            Text(
                text = "Color Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )

            ThemeSelector(
                themeOptions = themeOptions,
                onThemeOptionSelected = onThemeSelect,
            )
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsPermissionsSection(
    areNotificationsAllowed: Boolean,
    onNotificationsAllowedChange: (Boolean) -> Unit,
    isBatteryExempt: Boolean,
    onBatteryExemptChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val sharedPref = remember { PrivateSharedPreferences(context) }

    // Notification Permission State
    val notificationManager = remember {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    val notificationPermissionState =
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val isNotificationChecked = remember {
        mutableStateOf(notificationPermissionState.status.isGranted)
    }

    // Battery Optimization State
    val powerManager = remember {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    val isBatteryChecked = remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }

    // Lifecycle effects
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Update Notification State
                onNotificationsAllowedChange(notificationManager.areNotificationsEnabled())
                isNotificationChecked.value = notificationManager.areNotificationsEnabled()

                // Update Battery State
                val batteryState = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                onBatteryExemptChange(batteryState)
                isBatteryChecked.value = batteryState
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Permission Request Effect
    LaunchedEffect(notificationPermissionState.status.isGranted) {
        if (notificationPermissionState.status.isGranted) {
            isNotificationChecked.value = true
            sharedPref.saveDataBoolean(AppConstants.NOTIFICATION_ALLOWED, true)
        }
    }

    if (isNotificationChecked.value) {
        FeatureThatRequiresNotificationPermission(
            notificationPermissionState,
            isNotificationChecked
        )
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Notifications Permission
            PermissionItem(
                title = "Notifications",
                description = "Allow prayer time notifications",
                icon = Icons.Default.Notifications,
                isGranted = areNotificationsAllowed,
                onPermissionChange = { granted ->
                    if (granted) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (notificationPermissionState.status.isGranted) {
                                sharedPref.saveDataBoolean(AppConstants.NOTIFICATION_ALLOWED, true)
                            } else {
                                notificationPermissionState.launchPermissionRequest()
                            }
                        } else {
                            val intent = Intent().apply {
                                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                                putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    } else {
                        sharedPref.removeData(AppConstants.NOTIFICATION_ALLOWED)
                        val intent = Intent().apply {
                            action = "android.settings.APP_NOTIFICATION_SETTINGS"
                            putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                        }
                        context.startActivity(intent)
                    }
                },
                warningText = if (!areNotificationsAllowed)
                    "Please enable notifications to receive Adhan notifications"
                else null
            )

            // Battery Optimization Permission
            PermissionItem(
                title = "Battery Optimization",
                description = "Exempt from battery restrictions",
                icon = R.drawable.battery,
                isGranted = isBatteryExempt,
                onPermissionChange = { granted ->
                    if (granted) {
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } else {
                        val intent = Intent().apply {
                            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                        }
                        context.startActivity(intent)
                    }
                },
                warningText = if (!isBatteryExempt)
                    "Exempt Nimaz from battery optimization to receive Adhan notifications on time"
                else null
            )
        }
    }
}

@Composable
fun SettingsFooterSection(
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onRateApp: () -> Unit,
    onShareApp: () -> Unit,
    onNavigateToAbout: () -> Unit,
    isUpdateAvailable: Boolean,
    onUpdateApp: () -> Unit,
    isDebugMode: Boolean = false,
    onNavigateToDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Legal Section
        SettingsSection(
            title = "Legal",
            items = listOf(
                SettingsItem(
                    title = "Privacy Policy",
                    icon = R.drawable.privacy_policy_icon,
                    onClick = onNavigateToPrivacyPolicy,
                    showArrow = true
                ),
                SettingsItem(
                    title = "Terms of Service",
                    icon = R.drawable.document_icon,
                    onClick = onNavigateToTerms,
                    showArrow = true
                )
            )
        )

        // Support Section
        SettingsSection(
            title = "Support",
            items = listOf(
                SettingsItem(
                    title = "Help & FAQ",
                    icon = R.drawable.help_icon,
                    onClick = onNavigateToHelp,
                    showArrow = true
                ),
                SettingsItem(
                    title = "License & Acknowledgements",
                    subtitle = "Open source libraries",
                    icon = R.drawable.license_icon,
                    onClick = onNavigateToLicenses,
                    showArrow = true
                )
            )
        )

        // App Section
        SettingsSection(
            title = "App",
            items = buildList {
                add(
                    SettingsItem(
                        title = "Rate Nimaz",
                        icon = R.drawable.rating_icon,
                        onClick = onRateApp
                    )
                )
                add(
                    SettingsItem(
                        title = "Share Nimaz",
                        icon = R.drawable.share_icon,
                        onClick = onShareApp
                    )
                )
                add(SettingsItem(
                    title = "About",
                    subtitle = if (isUpdateAvailable) "Update Available" else "Nimaz is up to date",
                    icon = R.drawable.info_icon,
                    onClick = onNavigateToAbout,
                    action = if (isUpdateAvailable) {
                        {
                            Button(
                                onClick = onUpdateApp,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Update")
                            }
                        }
                    } else null,
                    showArrow = !isUpdateAvailable
                ))
                if (isDebugMode) {
                    add(
                        SettingsItem(
                            title = "Debug Tools",
                            subtitle = "For testing purposes only",
                            icon = R.drawable.debug_icon,
                            onClick = onNavigateToDebug,
                            showArrow = true
                        )
                    )
                }
            }
        )

        // Copyright
        AppCopyright()
    }
}

@Composable
private fun SettingsSection(
    title: String,
    items: List<SettingsItem>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            items.forEachIndexed { index, item ->
                SettingsItemRow(item = item)
            }
        }
    }
}

@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = item.onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = item.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                item.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item.action?.invoke() ?: run {
                if (item.showArrow) {
                    Icon(
                        painter = painterResource(id = R.drawable.angle_right_icon),
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private data class SettingsItem(
    val title: String,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val subtitle: String? = null,
    val showArrow: Boolean = false,
    val action: (@Composable () -> Unit)? = null
)


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
            // Top Section with App Bar and Location
            SettingsTopSection(
                onNavigateBack = { navController.popBackStack() },
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
                latitude = uiState.latitude,
                longitude = uiState.longitude,
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
                            Toast.makeText(
                                context,
                                task.exception?.message ?: "Error",
                                Toast.LENGTH_SHORT
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
                    viewModel.handleEvent(SettingsViewModel.SettingsEvent.CheckUpdate(activity))
                },
                isDebugMode = isDebugMode,
                onNavigateToDebug = onNavigateToDebugScreen
            )
        }

        // Error Banner
        if (uiState.error.isNotEmpty()) {
            ErrorBanner(
                error = uiState.error,
                onDismiss = {
                    // Add error dismissal handling if needed
                }
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    error: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}


