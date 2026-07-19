package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.arshadshah.nimaz.presentation.components.atoms.NimazChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazChipVariant
import com.arshadshah.nimaz.presentation.viewmodel.CityRegion
import com.arshadshah.nimaz.presentation.viewmodel.CurrentLocationState
import com.arshadshah.nimaz.presentation.viewmodel.LocationEvent
import com.arshadshah.nimaz.presentation.viewmodel.LocationViewModel
import com.arshadshah.nimaz.presentation.viewmodel.SearchLocation
import com.arshadshah.nimaz.presentation.viewmodel.citiesForRegion
import com.arshadshah.nimaz.presentation.viewmodel.formatCoordinates
import com.arshadshah.nimaz.presentation.viewmodel.groupCitiesByRegion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    onNavigateBack: () -> Unit,
    viewModel: LocationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var pendingLocationDetection by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted && pendingLocationDetection) {
            viewModel.onEvent(LocationEvent.UseCurrentGpsLocation)
            pendingLocationDetection = false
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(LocationEvent.DismissError)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.location),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NimazSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onEvent(LocationEvent.UpdateSearchQuery(it)) },
                    onSearch = {
                        focusManager.clearFocus()
                        viewModel.onEvent(LocationEvent.Search)
                    },
                    onClear = { viewModel.onEvent(LocationEvent.ClearSearch) },
                    isLoading = state.isSearching,
                    placeholder = stringResource(R.string.location_search_hint)
                )
            }

            // Search Results
            if (state.searchResults.isNotEmpty()) {
                item {
                    NimazSectionTitle(text = stringResource(R.string.location_search_results))
                }
                items(state.searchResults) { location ->
                    LocationListItem(
                        location = location,
                        isSelected = isLocationSelected(state.currentLocation, location),
                        onClick = { viewModel.onEvent(LocationEvent.SelectLocation(location)) }
                    )
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            // Current Location Card
            item {
                CurrentLocationCard(currentLocation = state.currentLocation)
            }

            // Use Current Location Button
            item {
                UseCurrentLocationButton(
                    isLoading = state.isLoadingGps,
                    onClick = {
                        if (hasLocationPermission()) {
                            viewModel.onEvent(LocationEvent.UseCurrentGpsLocation)
                        } else {
                            pendingLocationDetection = true
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )
            }

            // Recent Locations
            if (state.recentLocations.isNotEmpty()) {
                item {
                    NimazSectionTitle(text = stringResource(R.string.location_recent))
                }
                items(state.recentLocations) { location ->
                    LocationListItem(
                        location = location,
                        isSelected = isLocationSelected(state.currentLocation, location),
                        onClick = { viewModel.onEvent(LocationEvent.SelectLocation(location)) },
                        showGlobeIcon = true
                    )
                }
            }

            // Browse by region (hidden while showing live search results)
            if (state.searchResults.isEmpty()) {
                item {
                    NimazSectionTitle(text = stringResource(R.string.location_browse_by_region))
                }
                item {
                    RegionFilterRow(
                        selectedRegion = state.selectedRegion,
                        onSelect = { viewModel.onEvent(LocationEvent.SelectRegion(it)) }
                    )
                }

                if (state.selectedRegion == null) {
                    // "All" → grouped, with a region sub-header before each group
                    groupCitiesByRegion(state.popularCities).forEach { (region, cities) ->
                        item(key = "region-${region.name}") {
                            Text(
                                text = region.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp)
                            )
                        }
                        items(cities, key = { "${it.name}-${it.country}" }) { location ->
                            LocationListItem(
                                location = location,
                                isSelected = isLocationSelected(state.currentLocation, location),
                                onClick = { viewModel.onEvent(LocationEvent.SelectLocation(location)) }
                            )
                        }
                    }
                } else {
                    // Single region → flat list
                    items(
                        citiesForRegion(state.popularCities, state.selectedRegion),
                        key = { "${it.name}-${it.country}" }
                    ) { location ->
                        LocationListItem(
                            location = location,
                            isSelected = isLocationSelected(state.currentLocation, location),
                            onClick = { viewModel.onEvent(LocationEvent.SelectLocation(location)) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

private fun isLocationSelected(
    currentLocation: CurrentLocationState,
    location: SearchLocation
): Boolean {
    return when (currentLocation) {
        is CurrentLocationState.Set -> {
            kotlin.math.abs(currentLocation.latitude - location.latitude) < 0.001 &&
                    kotlin.math.abs(currentLocation.longitude - location.longitude) < 0.001
        }

        else -> false
    }
}


@Composable
private fun CurrentLocationCard(
    currentLocation: CurrentLocationState,
    modifier: Modifier = Modifier
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.GRADIENT,
        shape = RoundedCornerShape(16.dp),
        gradient = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                NimazIcon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    size = NimazIconSize.LARGE
                )
            }

            Spacer(modifier = Modifier.width(15.dp))

            // Location info
            Column(modifier = Modifier.weight(1f)) {
                when (currentLocation) {
                    is CurrentLocationState.Set -> {
                        Text(
                            text = currentLocation.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatCoordinates(
                                currentLocation.latitude,
                                currentLocation.longitude
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    CurrentLocationState.Loading -> {
                        Text(
                            text = stringResource(R.string.location_detecting),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    CurrentLocationState.NotSet -> {
                        Text(
                            text = stringResource(R.string.location_not_set),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Current badge
            if (currentLocation is CurrentLocationState.Set) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.location_current),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun UseCurrentLocationButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(14.dp),
        tone = NimazTone.NEUTRAL,
        enabled = !isLoading,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                NimazIcon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    variant = NimazIconVariant.PRIMARY,
                    size = NimazIconSize.MEDIUM
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isLoading) "Detecting Location..." else "Use Current Location",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RegionFilterRow(
    selectedRegion: CityRegion?,
    onSelect: (CityRegion?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NimazChip(
            text = stringResource(R.string.location_region_all),
            onClick = { onSelect(null) },
            variant = NimazChipVariant.FILTER,
            selected = selectedRegion == null
        )
        CityRegion.entries.sortedBy { it.order }.forEach { region ->
            NimazChip(
                text = region.label,
                onClick = { onSelect(region) },
                variant = NimazChipVariant.FILTER,
                selected = selectedRegion == region
            )
        }
    }
}

@Composable
private fun LocationListItem(
    location: SearchLocation,
    isSelected: Boolean,
    onClick: () -> Unit,
    showGlobeIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        selected = isSelected,
        colors = NimazCardDefaults.selectable(),
        elevation = 0.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Icon (country flag for curated cities, glyph otherwise)
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (location.flag != null) {
                Text(text = location.flag, style = MaterialTheme.typography.titleMedium)
            } else {
                NimazIcon(
                    imageVector = if (showGlobeIcon) Icons.Default.Public else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconSize = 18.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(15.dp))

        // Location info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = location.country,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Selection check
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                NimazIcon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    variant = NimazIconVariant.ON_ACCENT,
                    iconSize = 14.dp
                )
            }
        }
        }
    }
}
