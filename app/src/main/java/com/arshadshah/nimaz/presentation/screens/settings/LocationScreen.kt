package com.arshadshah.nimaz.presentation.screens.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.CurrentLocationState
import com.arshadshah.nimaz.presentation.viewmodel.LocationEvent
import com.arshadshah.nimaz.presentation.viewmodel.LocationViewModel
import com.arshadshah.nimaz.presentation.viewmodel.SearchLocation

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

    // Track if we need to detect location after permission granted
    var pendingLocationDetection by remember { mutableStateOf(false) }

    // Permission launcher
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

    // Check if location permission is granted
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

    // Show error in snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(LocationEvent.DismissError)
        }
    }

    Scaffold(
        containerColor = NimazColors.Neutral950,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Location",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NimazColors.Neutral950
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            item {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onEvent(LocationEvent.UpdateSearchQuery(it)) },
                    onSearch = {
                        focusManager.clearFocus()
                        viewModel.onEvent(LocationEvent.Search)
                    },
                    onClear = { viewModel.onEvent(LocationEvent.ClearSearch) },
                    isLoading = state.isSearching
                )
            }

            // Search Results
            if (state.searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Search Results",
                        style = MaterialTheme.typography.titleSmall,
                        color = NimazColors.Neutral400,
                        fontWeight = FontWeight.Medium
                    )
                }

                items(state.searchResults) { location ->
                    LocationListItem(
                        location = location,
                        isSelected = isLocationSelected(state.currentLocation, location),
                        onClick = { viewModel.onEvent(LocationEvent.SelectLocation(location)) }
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Current Location Card
            item {
                CurrentLocationCard(
                    currentLocation = state.currentLocation
                )
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recent Locations",
                        style = MaterialTheme.typography.titleSmall,
                        color = NimazColors.Neutral400,
                        fontWeight = FontWeight.Medium
                    )
                }

                items(state.recentLocations) { location ->
                    LocationListItem(
                        location = location,
                        isSelected = isLocationSelected(state.currentLocation, location),
                        onClick = { viewModel.onEvent(LocationEvent.SelectLocation(location)) }
                    )
                }
            }

            // Popular Cities
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Popular Cities",
                    style = MaterialTheme.typography.titleSmall,
                    color = NimazColors.Neutral400,
                    fontWeight = FontWeight.Medium
                )
            }

            items(state.popularCities) { location ->
                LocationListItem(
                    location = location,
                    isSelected = isLocationSelected(state.currentLocation, location),
                    onClick = { viewModel.onEvent(LocationEvent.SelectLocation(location)) }
                )
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
            kotlin.math.abs(currentLocation.latitude - location.latitude) < 0.01 &&
            kotlin.math.abs(currentLocation.longitude - location.longitude) < 0.01
        }
        else -> false
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.Neutral900
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = NimazColors.Neutral400,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White
                ),
                cursorBrush = SolidColor(NimazColors.Primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search for a city...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = NimazColors.Neutral500
                            )
                        }
                        innerTextField()
                    }
                }
            )

            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = NimazColors.Primary,
                    strokeWidth = 2.dp
                )
            }

            AnimatedVisibility(
                visible = query.isNotEmpty() && !isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = NimazColors.Neutral400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentLocationCard(
    currentLocation: CurrentLocationState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            NimazColors.Primary800,
                            NimazColors.Primary700
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Current Location",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            when (currentLocation) {
                                is CurrentLocationState.Set -> {
                                    Text(
                                        text = currentLocation.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                CurrentLocationState.Loading -> {
                                    Text(
                                        text = "Detecting...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                CurrentLocationState.NotSet -> {
                                    Text(
                                        text = "Not set",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Current badge
                    if (currentLocation is CurrentLocationState.Set) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Coordinates
                if (currentLocation is CurrentLocationState.Set) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lat: ${String.format("%.4f", currentLocation.latitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Lng: ${String.format("%.4f", currentLocation.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
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
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NimazColors.Neutral900,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = NimazColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isLoading) "Detecting Location..." else "Use Current Location",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LocationListItem(
    location: SearchLocation,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                NimazColors.Primary.copy(alpha = 0.15f)
            } else {
                NimazColors.Neutral900
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSelected) {
                            NimazColors.Primary.copy(alpha = 0.2f)
                        } else {
                            NimazColors.Neutral800
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isSelected) NimazColors.Primary else NimazColors.Neutral400,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Location info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = location.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = NimazColors.Neutral400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = NimazColors.Primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
