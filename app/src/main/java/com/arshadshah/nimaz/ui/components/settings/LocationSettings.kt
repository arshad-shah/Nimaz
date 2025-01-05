package com.arshadshah.nimaz.ui.components.settings

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.FeatureThatRequiresLocationPermission
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationSettings(
    locationState: SettingsViewModel.LocationState,
    onToggleLocation: (Boolean) -> Unit,
    onLocationInput: (String) -> Unit,
    isLoading: Boolean
) {

    //location permission state
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    if (locationState.isAuto) {
        FeatureThatRequiresLocationPermission(locationPermissionState, onLocationToggle = {
            onToggleLocation(it)
        })
    }
    SettingsGroup(title = { Text(text = "Location") }) {
        LocationToggleSwitch(
            state = createBooleanState(locationState.isAuto),
            locationPermissionState = locationPermissionState,
            locationAuto = locationState.isAuto,
            locationName = locationState.name,
            onLocationToggle = {
                onToggleLocation(it)
            }
        )
        AnimatedVisibility(
            visible = !locationState.isAuto,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                ElevatedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    ManualLocationInput(
                        locationName = locationState.name,
                        onLocationInput = onLocationInput,
                        isLoading
                    )
                }
            }
        }
        CoordinatesView(
            longitudeState = locationState.longitude,
            latitudeState = locationState.latitude,
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationToggleSwitch(
    state: SettingValueState<Boolean>,
    locationPermissionState: MultiplePermissionsState,
    onLocationToggle: (Boolean) -> Unit,
    locationAuto: Boolean,
    locationName: String,
) {
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (locationPermissionState.permissions[0].status.isGranted || locationPermissionState.permissions[1].status.isGranted) {
                        //check if the value saved in the shared preferences is true
                        val isLocationAutoInPref = PrivateSharedPreferences(context).getDataBoolean(
                            AppConstants.LOCATION_TYPE,
                            false
                        )
                        if (isLocationAutoInPref) {
                            onLocationToggle(true)
                        } else {
                            onLocationToggle(false)
                        }
                    }
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

    ElevatedCard(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        SettingsSwitch(
            state = state,
            icon = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.marker_icon),
                    contentDescription = "Location"
                )
            },
            title = {
                if (state.value) {
                    Text(text = "Automatic")
                } else {
                    Text(text = "Manual")
                }
            },
            subtitle = {
                if (locationAuto) {
                    Text(text = locationName)
                }
            },
            onCheckedChange = {
                if (it) {
                    if (locationPermissionState.allPermissionsGranted) {
                        onLocationToggle(true)
                    } else {
                        locationPermissionState.launchMultiplePermissionRequest()
                    }
                } else {
                    onLocationToggle(false)
                    PrivateSharedPreferences(context).saveDataBoolean(
                        AppConstants.ALARM_LOCK,
                        false
                    )
                }
            }
        )
    }

    if (state.value) {
        ElevatedCard(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            SettingsMenuLink(
                icon = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.target_icon),
                        contentDescription = "Location"
                    )
                },
                title = {
                    Text(
                        textAlign = TextAlign.Center,
                        text = locationName,
                        modifier = Modifier
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }) {

            }
        }
    }
}

private fun createBooleanState(value: Boolean) = object : SettingValueState<Boolean> {
    override var value: Boolean = value
    override fun reset() {}
}