package com.arshadshah.nimaz.ui.components.settings

import android.Manifest
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.NimazTextField
import com.arshadshah.nimaz.ui.components.common.NimazTextFieldType
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Location settings card with auto/manual mode selection.
 *
 * Design System Alignment:
 * - ElevatedCard with extraLarge shape
 * - 4dp elevation
 * - 8dp inner padding
 * - 12dp section spacing
 * - Header: primaryContainer with 16dp corners
 * - Content: surfaceVariant @ 0.5 alpha with 16dp corners
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationSettings(
    location: String,
    isLoading: Boolean,
    locationSettingsState: SettingsViewModel.LocationSettingsState,
    onToggleLocation: (Boolean) -> Unit,
    onLocationInput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val locationInput = remember { mutableStateOf(location) }
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    val focusRequester = remember { FocusRequester() }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
            LocationHeader()

            // Mode Selector
            LocationModeSelector(
                locationSettingsState = locationSettingsState,
                locationPermissionState = locationPermissionState,
                onToggleLocation = onToggleLocation
            )

            // Current Location Display
            CurrentLocationDisplay(
                location = location,
                isLoading = isLoading
            )

            // Manual Search (when manual mode)
            AnimatedVisibility(
                visible = !locationSettingsState.isAuto,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LocationSearch(
                    locationInput = locationInput.value,
                    onSearch = { query ->
                        onLocationInput(query)
                        focusRequester.freeFocus()
                    },
                    focusRequester = focusRequester
                )
            }
        }
    }
}

/**
 * Header section with icon and title.
 */
@Composable
private fun LocationHeader(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Location Settings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Mode selector with Auto/Manual toggle cards.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationModeSelector(
    locationSettingsState: SettingsViewModel.LocationSettingsState,
    locationPermissionState: MultiplePermissionsState,
    onToggleLocation: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LocationModeCard(
                title = "Automatic",
                description = "Use GPS",
                icon = R.drawable.marker_icon,
                isSelected = locationSettingsState.isAuto,
                onClick = {
                    if (!locationSettingsState.isAuto) {
                        if (locationPermissionState.allPermissionsGranted) {
                            onToggleLocation(true)
                        } else {
                            locationPermissionState.launchMultiplePermissionRequest()
                        }
                    }
                }
            )

            LocationModeCard(
                title = "Manual",
                description = "Search city",
                icon = R.drawable.location_marker_edit_icon,
                isSelected = !locationSettingsState.isAuto,
                onClick = { onToggleLocation(false) }
            )
        }
    }
}

/**
 * Individual mode selection card.
 */
@Composable
private fun RowScope.LocationModeCard(
    title: String,
    description: String,
    @DrawableRes icon: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = title,
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Text Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Current location display with loading indicator.
 */
@Composable
fun CurrentLocationDisplay(
    location: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Current Location",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = location.ifEmpty { "Not set" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Loading indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Location search input field.
 */
@Composable
fun LocationSearch(
    locationInput: String,
    onSearch: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val searchQuery = remember { mutableStateOf(locationInput) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search Location",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Badge
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Manual",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Search Input
            NimazTextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                type = NimazTextFieldType.SEARCH,
                placeholder = "Enter city or address",
                leadingIconVector = Icons.Rounded.Search,
                onSearchClick = { onSearch(searchQuery.value) },
                requestFocus = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch(searchQuery.value) }
                )
            )


            // Helper Text
            Text(
                text = "Enter a city name, postal code, or full address",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// =============================================================================
// PREVIEWS
// =============================================================================

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun LocationSettingsPreview() {
    MaterialTheme {
        LocationSettings(
            location = "Dublin, Ireland",
            isLoading = false,
            locationSettingsState = SettingsViewModel.LocationSettingsState(isAuto = true),
            onToggleLocation = {},
            onLocationInput = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun LocationSettingsPreview_Manual() {
    MaterialTheme {
        LocationSettings(
            location = "London, UK",
            isLoading = false,
            locationSettingsState = SettingsViewModel.LocationSettingsState(isAuto = false),
            onToggleLocation = {},
            onLocationInput = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun LocationSettingsPreview_Loading() {
    MaterialTheme {
        LocationSettings(
            location = "Fetching location...",
            isLoading = true,
            locationSettingsState = SettingsViewModel.LocationSettingsState(isAuto = true),
            onToggleLocation = {},
            onLocationInput = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun CurrentLocationDisplayPreview() {
    MaterialTheme {
        CurrentLocationDisplay(
            location = "Dublin, Ireland",
            isLoading = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun LocationSearchPreview() {
    MaterialTheme {
        LocationSearch(
            locationInput = "",
            onSearch = {},
            focusRequester = remember { FocusRequester() },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LocationSettingsPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LocationSettings(
                location = "Dublin, Ireland",
                isLoading = false,
                locationSettingsState = SettingsViewModel.LocationSettingsState(isAuto = true),
                onToggleLocation = {},
                onLocationInput = {},
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}