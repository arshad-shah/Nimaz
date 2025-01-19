package com.arshadshah.nimaz.ui.components.intro

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.viewModel.IntroductionViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun IntroLocation(
    viewModel: IntroductionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val locationNameState = viewModel.locationName.collectAsState()
    val locationSettings = viewModel.locationSettingsState.collectAsState().value
    val isLoading = viewModel.uiState.collectAsState().value.isLoading

    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // Handle permission changes
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        viewModel.handleEvent(
            IntroductionViewModel.IntroEvent.UpdateLocationPermission(
                locationPermissions.allPermissionsGranted
            )
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Location Toggle Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.marker_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = locationNameState.value.takeIf { it.isNotEmpty() }
                                ?: "Not set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = locationSettings.isAuto,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (!locationPermissions.allPermissionsGranted) {
                                locationPermissions.launchMultiplePermissionRequest()
                            } else {
                                viewModel.handleEvent(
                                    IntroductionViewModel.IntroEvent.HandleLocationToggle(true)
                                )
                            }
                        } else {
                            viewModel.handleEvent(
                                IntroductionViewModel.IntroEvent.HandleLocationToggle(false)
                            )
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            // Manual Location Input
            AnimatedVisibility(
                visible = !locationSettings.isAuto,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val locationInput = remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }

                OutlinedTextField(
                    value = locationInput.value,
                    onValueChange = { locationInput.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Enter city name") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (locationInput.value.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = {
                                    viewModel.handleEvent(
                                        IntroductionViewModel.IntroEvent.LocationInput(
                                            locationInput.value
                                        )
                                    )
                                    focusRequester.freeFocus()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search"
                                )
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.handleEvent(
                                IntroductionViewModel.IntroEvent.LocationInput(
                                    locationInput.value
                                )
                            )
                            focusRequester.freeFocus()
                        }
                    )
                )
            }

            // Loading Indicator
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    }

    // Permission Dialog
    if (locationSettings.isAuto && !locationPermissions.allPermissionsGranted) {
        LocationPermissionDialog(
            showRationale = locationPermissions.shouldShowRationale,
            onConfirm = { locationPermissions.launchMultiplePermissionRequest() }
        )
    }
}

@Composable
private fun LocationPermissionDialog(
    showRationale: Boolean,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Location Access") },
        text = {
            Text(
                if (showRationale) {
                    "Location permission is required for accurate prayer times. Please grant access."
                } else {
                    "Location access helps provide accurate prayer times for your area."
                }
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Grant Access")
            }
        },
        dismissButton = {
            Button(onClick = { }) {
                Text("Manual Setup")
            }
        }
    )
}