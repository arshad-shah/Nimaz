package com.arshadshah.nimaz.ui.components.intro


import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.PermissionItem
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.utils.FeatureThatRequiresNotificationPermission
import com.arshadshah.nimaz.viewModel.IntroductionViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun IntroNotification(viewModel: IntroductionViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val notificationAllowed =
        viewModel.notificationSettingsState.collectAsState().value.areNotificationsAllowed
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Notification permission state
    val notificationPermissionState =
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val isChecked = remember { mutableStateOf(notificationPermissionState.status.isGranted) }

    // Handle permission requirements
    if (isChecked.value) {
        FeatureThatRequiresNotificationPermission(
            notificationPermissionState,
            isChecked
        )
    }

    // Lifecycle observer for permission checks
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
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // Handle permission grant effects
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

    PermissionItem(
        title = "Notifications",
        description = "Receive Adhan notifications",
        icon = Icons.Filled.Notifications,
        isGranted = notificationPermissionState.status.isGranted,
        onPermissionChange = { enabled ->
            viewModel.handleEvent(
                IntroductionViewModel.IntroEvent.HandleNotificationToggle(enabled)
            )
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!notificationPermissionState.status.isGranted) {
                    notificationPermissionState.launchPermissionRequest()
                }
            }
        },
        warningText = if (!notificationAllowed) "Please enable notifications to receive Adhan notifications" else null
    )
}