package com.arshadshah.nimaz.ui.components.intro

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.settings.SettingValueState
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
    val locationAuto = viewModel.locationSettingsState.collectAsState().value
    val latitude = viewModel.latitude.collectAsState()
    val longitude = viewModel.longitude.collectAsState()
    val error = viewModel.uiState.collectAsState().value.error
    val isLoading = viewModel.uiState.collectAsState().value.isLoading
    val locationSettingState = viewModel.locationSettingsState.collectAsState()

    // Location permission state
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )


    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (locationPermissionState.permissions[0].status.isGranted ||
                        locationPermissionState.permissions[1].status.isGranted
                    ) {
                        viewModel.handleEvent(
                            IntroductionViewModel.IntroEvent.HandleLocationToggle(
                                true
                            )
                        )
                    }
                }

                else -> {}
            }
        }

        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (locationAuto.isAuto) {
        PermissionExplanation(
            locationPermissionState = locationPermissionState,
            state = createBooleanState(true)
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
    LocationSettings(
        location = locationNameState.value,
        latitude = latitude.value,
        longitude = longitude.value,
        isLoading = isLoading,
        locationSettingsState = locationSettingState.value,
        onToggleLocation = {
            if (it) {
                if (locationPermissionState.allPermissionsGranted) {
                    viewModel.handleEvent(
                        IntroductionViewModel.IntroEvent.HandleLocationToggle(
                            false
                        )
                    )
                } else {
                    locationPermissionState.launchMultiplePermissionRequest()
                }
            } else {
                viewModel.handleEvent(IntroductionViewModel.IntroEvent.HandleLocationToggle(false))
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
        },
        onLocationInput = {
            viewModel.handleEvent(IntroductionViewModel.IntroEvent.LocationInput(it))
        }
    )
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


@Composable
fun LocationSettings(
    location: String,
    latitude: Double,
    longitude: Double,
    isLoading: Boolean,
    locationSettingsState: IntroductionViewModel.LocationSettingsState,
    onToggleLocation: (Boolean) -> Unit,
    onLocationInput: (String) -> Unit,
) {


    val locationInput = remember {
        mutableStateOf(location)
    }
    //custom focus for the text field
    val focusRequester = remember { FocusRequester() }
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
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
                            onCheckedChange = onToggleLocation,
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

                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
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
                                    trailingIcon = {
                                        if (locationInput.value.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    onLocationInput(locationInput.value)
                                                    //remove focus from the text field
                                                    focusRequester.freeFocus()
                                                },

                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Search,
                                                    contentDescription = "Search",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
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
            }
        }
    }
}