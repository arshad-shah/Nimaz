package com.arshadshah.nimaz.ui.components.common

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.settings.SettingsSwitch
import com.arshadshah.nimaz.ui.components.settings.state.rememberPreferenceBooleanSettingState
import com.arshadshah.nimaz.utils.FeatureThatRequiresNotificationPermission
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationScreenUI() {
    val context = LocalContext.current
    //get shared preference
    val sharedpref = PrivateSharedPreferences(context)

    val viewModel = viewModel(
        key = AppConstants.SETTINGS_VIEWMODEL_KEY,
        initializer = { SettingsViewModel(context) },
        viewModelStoreOwner = context as ComponentActivity
    )
    val notificationAllowed = remember {
        viewModel.areNotificationsAllowed
    }.collectAsState()

    //battery optimization exemption
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    //notification permission state
    val notificationPermissionState =
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    val isChecked =
        remember { mutableStateOf(notificationPermissionState.status.isGranted) }

    //the state of the switch
    val state =
        rememberPreferenceBooleanSettingState(
            AppConstants.NOTIFICATION_ALLOWED,
            notificationPermissionState.status.isGranted
        )

    if (isChecked.value) {
        FeatureThatRequiresNotificationPermission(
            notificationPermissionState,
            isChecked
        )
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.handleEvent(
                        SettingsViewModel.SettingsEvent.NotificationsAllowed(
                            notificationManager.areNotificationsEnabled()
                        )
                    )
                    state.value = notificationManager.areNotificationsEnabled()
                    isChecked.value = notificationManager.areNotificationsEnabled()
                }

                else -> {

                }
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    //a laucnhed affect to check if the user has granted the notification permission
    LaunchedEffect(notificationPermissionState.status.isGranted) {
        if (notificationPermissionState.status.isGranted) {
            //if the user has granted the notification permission then set the state of the switch to true
            state.value = true
            //set the isChecked to true
            isChecked.value = true

            val sharedPreferences = PrivateSharedPreferences(context)
            val channelLock =
                sharedPreferences.getDataBoolean(AppConstants.CHANNEL_LOCK, false)
            if (!channelLock) {
                CreateAlarms().createAllNotificationChannels(context)
                sharedPreferences.saveDataBoolean(AppConstants.CHANNEL_LOCK, true)
            }

            sharedpref.saveDataBoolean(AppConstants.NOTIFICATION_ALLOWED, true)
        }
    }

    SettingsSwitch(
        modifier = Modifier.testTag("notification_switch_on_intro_screen"),
        state = state,
        onCheckedChange = {
            if (it) {
                //if its android 13 or above then check if the notification permission is granted else take the user to the notification settings
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (notificationPermissionState.status.isGranted) {
                        //if the permission is granted, then save the value in the shared preferences
                        sharedpref.saveDataBoolean(
                            AppConstants.NOTIFICATION_ALLOWED,
                            true
                        )
                    } else {
                        notificationPermissionState.launchPermissionRequest()
                    }
                } else {
                    //take the user to the notification settings
                    val intent = Intent()
                    intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    intent.putExtra(
                        "android.provider.extra.APP_PACKAGE",
                        context.packageName
                    )
                    context.startActivity(intent)
                }
            } else {
                //if its unchecked, then we need to remove the notification permission
                //and remove the value from the shared preferences
                sharedpref.removeData(AppConstants.NOTIFICATION_ALLOWED)
                //take the user to the notification settings
                val intent = Intent()
                intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                intent.putExtra(
                    "android.provider.extra.APP_PACKAGE",
                    context.packageName
                )
                context.startActivity(intent)
            }
        },
        title = {
            Text(text = "Notifications")
        },
        subtitle = {
            //if the permission is granted, show a checkmark and text saying "Allowed"
            if (notificationAllowed.value) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp),
                        painter = painterResource(id = R.drawable.checkbox_icon),
                        contentDescription = "Notifications Allowed"
                    )
                    Text(text = "Enabled")
                }
            } else {
                //if the permission is not granted, show a notification icon and text saying "Not Allowed"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp),
                        painter = painterResource(id = R.drawable.cross_circle_icon),
                        contentDescription = "Notifications Not Allowed"
                    )
                    Text(text = "Disabled")
                }
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notifications"
            )
        }
    )

    if (!notificationAllowed.value) {
        BannerSmall(
            message = "Please enable notifications to receive Adhan notifications",
            showFor = BannerDuration.FOREVER.value
        )
    }
}