package com.arshadshah.nimaz.ui.components.intro
import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val locationSettings by viewModel.locationSettingsState.collectAsState()
    val locationState by viewModel.locationState.collectAsState()
    val isLoading by viewModel.uiState.collectAsState()
    val locationNameState = viewModel.locationName.collectAsState()

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

    // Lifecycle observer
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.handleEvent(
                    IntroductionViewModel.IntroEvent.HandleLocationToggle(
                        locationSettings.isAuto
                    )
                )
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

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
            // Header Section with Location Toggle
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
                            painter = painterResource(id = R.drawable.marker_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Location Services",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (locationSettings.isAuto) "Automatic" else "Manual",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
                    )
                }
            }

            // Current Location Display
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Current Location",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = locationNameState.value.takeIf { it.isNotEmpty() }
                                ?: "Location not set",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Manual Location Search
            AnimatedVisibility(
                visible = !locationSettings.isAuto,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val locationInput = remember { mutableStateOf("") }
                val focusRequester = remember { FocusRequester() }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Search Location",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = locationInput.value,
                            onValueChange = { locationInput.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = { Text("Enter city name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
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
                }
            }

            // Loading Indicator
            AnimatedVisibility(
                visible = isLoading.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            // Location Features
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LocationFeature(
                        icon = R.drawable.time_calculation,
                        title = "Accurate Prayer Times",
                        description = "Get precise prayer times based on your location"
                    )
                    LocationFeature(
                        icon = R.drawable.qibla,
                        title = "Qibla Direction",
                        description = "Find the correct Qibla direction from your location"
                    )
                    LocationFeature(
                        icon = R.drawable.calendar_icon,
                        title = "Local Calendar",
                        description = "Access Islamic calendar adjusted to your timezone"
                    )
                }
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
private fun LocationFeature(
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
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Grant Access")
            }
        },
        dismissButton = {
            TextButton(onClick = { }) {
                Text("Manual Setup")
            }
        }
    )
}