package com.arshadshah.nimaz.ui.components.intro

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.BannerDuration
import com.arshadshah.nimaz.ui.components.common.BannerSmall
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.utils.FeatureThatRequiresNotificationPermission
import com.arshadshah.nimaz.viewModel.IntroductionViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun IntroNotification(
    viewModel: IntroductionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val notificationSettings by viewModel.notificationSettingsState.collectAsState()
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Permission state
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val isChecked = remember { mutableStateOf(notificationPermissionState.status.isGranted) }

    // Handle permission requirements
    if (isChecked.value) {
        FeatureThatRequiresNotificationPermission(notificationPermissionState, isChecked)
    }

    // Lifecycle observer
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.handleEvent(
                    IntroductionViewModel.IntroEvent.NotificationsAllowed(
                        notificationManager.areNotificationsEnabled()
                    )
                )
                isChecked.value = notificationManager.areNotificationsEnabled()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Permission grant effects
    LaunchedEffect(notificationPermissionState.status.isGranted) {
        if (notificationPermissionState.status.isGranted) {
            isChecked.value = true
            viewModel.handleEvent(IntroductionViewModel.IntroEvent.CreateNotificationChannels)
            viewModel.handleEvent(
                IntroductionViewModel.IntroEvent.UpdateNotificationPermission(true)
            )
        }
    }

    val state = rememberPreferenceBooleanSettingState(
        AppConstants.NOTIFICATION_ALLOWED,
        notificationPermissionState.status.isGranted
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
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
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = "Prayer Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (notificationSettings.areNotificationsAllowed) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Switch(
                        checked = notificationSettings.areNotificationsAllowed,
                        onCheckedChange = { enabled ->
                            viewModel.handleEvent(
                                IntroductionViewModel.IntroEvent.HandleNotificationToggle(enabled)
                            )
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (!notificationPermissionState.status.isGranted) {
                                    notificationPermissionState.launchPermissionRequest()
                                }
                            }
                        },
                    )
                }
            }

            // Features Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NotificationFeature(
                        icon = R.drawable.adhan,
                        title = "Adhan Alerts",
                        description = "Receive beautiful Adhan notifications for each prayer time"
                    )
                    NotificationFeature(
                        icon = R.drawable.time_calculation,
                        title = "Precise Timing",
                        description = "Get notifications at exact prayer times based on your location"
                    )
                    NotificationFeature(
                        icon = R.drawable.tracker_icon,
                        title = "Prayer Tracking",
                        description = "Track your prayer history with regular reminders"
                    )
                }
            }

            // Warning Message
            AnimatedVisibility(
                visible = !notificationSettings.areNotificationsAllowed,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BannerSmall(
                    variant = BannerVariant.Warning,
                    message = "Please enable notifications to receive prayer time alerts",
                    showFor = BannerDuration.FOREVER.value
                )
            }
        }
    }
}

@Composable
private fun NotificationFeature(
    icon: Int,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(48.dp)
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier
                    .padding(12.dp)
                    .size(24.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}