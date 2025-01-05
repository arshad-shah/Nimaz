package com.arshadshah.nimaz.ui.components.intro

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
import com.arshadshah.nimaz.ui.components.settings.SettingsMenuLink
import com.arshadshah.nimaz.ui.components.settings.SettingsSwitch
import com.arshadshah.nimaz.viewModel.IntroductionViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun IntroLocation(
    viewModel: IntroductionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val locationNameState = viewModel.locationName.collectAsState()
    val locationAuto = viewModel.isLocationAuto.collectAsState()
    val latitudeState = viewModel.latitude.collectAsState()
    val longitudeState = viewModel.longitude.collectAsState()
    val error = viewModel.error.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()
    val autoParams = viewModel.autoParams.collectAsState()

    Log.d("IntroLocation", "locationNameState: ${locationNameState.value}")
    Log.d("IntroLocation", "locationAuto: ${locationAuto.value}")
    Log.d("IntroLocation", "latitudeState: ${latitudeState.value}")
    Log.d("IntroLocation", "longitudeState: ${longitudeState.value}")
    Log.d("IntroLocation", "error: ${error.value}")
    Log.d("IntroLocation", "isLoading: ${isLoading.value}")
    Log.d("IntroLocation", "autoParams: ${autoParams.value}")

    if (error.value?.isNotBlank() == true) {
        Toasty.error(context, error.value!!, Toasty.LENGTH_SHORT).show()
    } else {
        // Location permission state
        val locationPermissionState = rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        if (locationAuto.value) {
            PermissionExplanation(
                locationPermissionState = locationPermissionState,
                state = createBooleanState(locationAuto.value)
            )
        }

        // Update location permission in ViewModel when permissions change
        LaunchedEffect(locationPermissionState.allPermissionsGranted) {
            viewModel.handleEvent(
                IntroductionViewModel.IntroEvent.UpdateLocationPermission(
                    locationPermissionState.allPermissionsGranted
                )
            )
        }

        // Handle auto parameters updates when location changes
        LaunchedEffect(
            locationNameState.value,
            latitudeState.value,
            longitudeState.value
        ) {
            if (autoParams.value) {
                viewModel.handleEvent(IntroductionViewModel.IntroEvent.UpdateAutoParams(true))
            }
        }
        LocationToggleSwitch(
            state = createBooleanState(locationAuto.value),
            locationPermissionState = locationPermissionState,
            locationName = locationNameState.value,
            isLoading = isLoading.value,
            onLocationToggle = {
                viewModel.handleEvent(IntroductionViewModel.IntroEvent.HandleLocationToggle(it))
            },
        )
        AnimatedVisibility(
            visible = !locationAuto.value,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ElevatedCard(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                ),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                LocationInput(name = locationNameState.value, onInput = {
                    viewModel.handleEvent(IntroductionViewModel.IntroEvent.LocationInput(it))
                })
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationToggleSwitch(
    state: SettingValueState<Boolean>,
    locationName: String,
    isLoading: Boolean,
    locationPermissionState: MultiplePermissionsState,
    onLocationToggle: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (locationPermissionState.permissions[0].status.isGranted ||
                        locationPermissionState.permissions[1].status.isGranted
                    ) {
                        onLocationToggle(true)
                    }
                }

                else -> {}
            }
        }

        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
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
                Text(text = "Enable Auto Location")
            },
            subtitle = {
                if (state.value) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp),
                            painter = painterResource(id = R.drawable.checkbox_icon),
                            contentDescription = "Location Allowed"
                        )
                        Text(text = "Enabled")
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp),
                            painter = painterResource(id = R.drawable.cross_circle_icon),
                            contentDescription = "Location Not Allowed"
                        )
                        Text(text = "Disabled")
                    }
                }
            },
            onCheckedChange = { enabled ->
                if (enabled) {
                    if (locationPermissionState.allPermissionsGranted) {
                        onLocationToggle(true)
                    } else {
                        locationPermissionState.launchMultiplePermissionRequest()
                    }
                } else {
                    onLocationToggle(false)
                    Toasty.info(
                        context,
                        "Please disable location permission for Nimaz in \n Permissions -> Location -> Don't Allow",
                        Toasty.LENGTH_LONG
                    ).show()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addCategory(Intent.CATEGORY_DEFAULT)
                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    }
                    context.startActivity(intent)
                }
            }
        )
    }

    if (state.value) {
        ElevatedCard(
            modifier = Modifier
                .padding(4.dp)
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
                            .padding(8.dp)
                            .placeholder(
                                visible = isLoading,
                                highlight = PlaceholderHighlight.shimmer()
                            ),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            ) {}
        }
    }
}


@Composable
fun LocationInput(
    name: String,
    onInput: (String) -> Unit,
) {
    val input = remember { mutableStateOf(name) }
    val showDialog = remember { mutableStateOf(false) }
    //show manual location input
    //onclick open dialog
    SettingsMenuLink(
        title = { Text(text = "Edit Location") },
        subtitle = {
            Text(
                text = name,
            )
        },
        onClick = {
            showDialog.value = true
        },
        icon = {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.location_marker_edit_icon),
                contentDescription = "Location"
            )
        }
    )

    if (!showDialog.value) return

    AlertDialogNimaz(
        cardContent = false,
        bottomDivider = false,
        topDivider = false,
        contentHeight = 100.dp,
        confirmButtonText = "Submit",
        contentDescription = "Edit Location",
        title = "Edit Location",
        contentToShow = {
            OutlinedTextField(
                shape = MaterialTheme.shapes.extraLarge,
                value = input.value,
                onValueChange = {
                    input.value = it
                },
                label = { Text(text = "Location") },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        },
        onDismissRequest = {
            showDialog.value = false
        },
        onConfirm = {
            onInput(input.value)
            showDialog.value = false

        },
        onDismiss = {
            showDialog.value = false
        })
}

private fun createBooleanState(value: Boolean) = object : SettingValueState<Boolean> {
    override var value: Boolean = value
    override fun reset() {}
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionExplanation(
    locationPermissionState: MultiplePermissionsState,
    state: SettingValueState<Boolean>,
) {

    val descToShow = remember { mutableStateOf("") }
    val showDialog = remember { mutableStateOf(false) }
    //check if location permission is granted
    if (locationPermissionState.allPermissionsGranted) {
        state.value = true
    } else {
        if (locationPermissionState.shouldShowRationale) {
            showDialog.value = true
            descToShow.value =
                "Location permission is required to get accurate prayer times. Please allow location permission."

        } else {
            showDialog.value = false
            descToShow.value =
                "Location permission is required to get accurate prayer times, Nimaz will revert to manual location."
        }
    }

    if (showDialog.value) {
        //permission not granted
        //show dialog
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            title = { Text(text = "Location Permission Required") },
            text = { Text(text = descToShow.value) },
            confirmButton = {
                Button(onClick = {
                    showDialog.value = false
                    locationPermissionState.launchMultiplePermissionRequest()
                }) {
                    Text(
                        text = "Grant Permission",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDialog.value = false
                }) {
                    Text(text = "Close", style = MaterialTheme.typography.titleMedium)
                }
            }
        )
    }
}