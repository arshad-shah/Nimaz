package com.arshadshah.nimaz.presentation.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.OnboardingEvent
import com.arshadshah.nimaz.presentation.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

private data class InfoPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val features: List<String>
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onEvent(OnboardingEvent.UpdatePermissionStatus(location = granted))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onEvent(OnboardingEvent.UpdatePermissionStatus(notification = granted))
    }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onEvent(OnboardingEvent.CheckBatteryOptimization)
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(OnboardingEvent.DismissError)
        }
    }

    // 3 info pages + 1 permissions page = 4 pages total
    val infoPages = listOf(
        InfoPage(
            title = stringResource(R.string.onboarding_welcome_title),
            description = stringResource(R.string.onboarding_welcome_description),
            icon = Icons.Default.Mosque,
            color = MaterialTheme.colorScheme.primary,
            features = listOf(
                stringResource(R.string.onboarding_feature_prayer_times),
                stringResource(R.string.onboarding_feature_quran),
                stringResource(R.string.onboarding_feature_hadith),
                stringResource(R.string.onboarding_feature_duas)
            )
        ),
        InfoPage(
            title = stringResource(R.string.onboarding_prayer_title),
            description = stringResource(R.string.onboarding_prayer_description),
            icon = Icons.Default.Schedule,
            color = MaterialTheme.colorScheme.primary,
            features = listOf(
                stringResource(R.string.onboarding_feature_calc_methods),
                stringResource(R.string.onboarding_feature_custom_adjustments),
                stringResource(R.string.onboarding_feature_tracking),
                stringResource(R.string.onboarding_feature_statistics)
            )
        ),
        InfoPage(
            title = stringResource(R.string.onboarding_quran_title),
            description = stringResource(R.string.onboarding_quran_description),
            icon = Icons.AutoMirrored.Filled.MenuBook,
            color = NimazColors.QuranColors.Meccan,
            features = listOf(
                stringResource(R.string.onboarding_feature_translations),
                stringResource(R.string.onboarding_feature_audio),
                stringResource(R.string.onboarding_feature_bookmarks),
                stringResource(R.string.onboarding_feature_search)
            )
        )
    )

    val totalPages = infoPages.size + 1 // +1 for permissions page
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val pageColors = infoPages.map { it.color } + MaterialTheme.colorScheme.secondary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Skip Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (pagerState.currentPage < totalPages - 1) {
                    TextButton(onClick = {
                        viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                        onComplete()
                    }) {
                        Text(
                            stringResource(R.string.onboarding_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                if (page < infoPages.size) {
                    InfoPageContent(page = infoPages[page])
                } else {
                    PermissionsPageContent(
                        locationGranted = state.locationPermissionGranted,
                        notificationGranted = state.notificationPermissionGranted,
                        batteryOptDisabled = state.batteryOptimizationDisabled,
                        locationName = if (state.locationDetected) state.locationName else null,
                        onRequestLocation = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onRequestNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.onEvent(OnboardingEvent.UpdatePermissionStatus(notification = true))
                            }
                        },
                        onRequestBattery = {
                            batteryOptimizationLauncher.launch(viewModel.getBatteryOptimizationIntent())
                        }
                    )
                }
            }

            // Bottom Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalPages) { index ->
                        val isSelected = index == pagerState.currentPage
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.8f,
                            label = "indicator_scale"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 10.dp else 8.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) pageColors[index]
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedVisibility(
                        visible = pagerState.currentPage > 0,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    }

                    if (pagerState.currentPage == 0) {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage == totalPages - 1) {
                                viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                                onComplete()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pageColors[pagerState.currentPage]
                        )
                    ) {
                        Text(
                            if (pagerState.currentPage == totalPages - 1)
                                stringResource(R.string.onboarding_get_started)
                            else
                                stringResource(R.string.onboarding_next)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (pagerState.currentPage == totalPages - 1)
                                Icons.Default.Check
                            else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Info page (Welcome, Prayer Times, Quran) ---

@Composable
private fun InfoPageContent(
    page: InfoPage,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxHeight < 500.dp
        val iconSize = if (isCompact) 80.dp else 120.dp
        val iconInnerSize = if (isCompact) 40.dp else 60.dp
        val sectionSpacing = if (isCompact) 16.dp else 32.dp
        val smallSpacing = if (isCompact) 6.dp else 12.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = if (isCompact) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(page.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = page.color,
                    modifier = Modifier.size(iconInnerSize)
                )
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = page.title,
                style = if (isCompact) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(smallSpacing))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = if (isCompact) 10.dp else 16.dp
                    )
                ) {
                    page.features.forEach { feature ->
                        FeatureRow(text = feature, color = page.color, compact = isCompact)
                    }
                }
            }
        }
    }
}

// --- Unified permissions page ---

@Composable
private fun PermissionsPageContent(
    locationGranted: Boolean,
    notificationGranted: Boolean,
    batteryOptDisabled: Boolean,
    locationName: String?,
    onRequestLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBattery: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxHeight < 500.dp
        val iconSize = if (isCompact) 64.dp else 88.dp
        val iconInnerSize = if (isCompact) 32.dp else 44.dp
        val sectionSpacing = if (isCompact) 12.dp else 20.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = if (isCompact) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header icon
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(iconInnerSize)
                )
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = stringResource(R.string.onboarding_permissions_title),
                style = if (isCompact) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))

            Text(
                text = stringResource(R.string.onboarding_permissions_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            // Permission cards
            PermissionCard(
                icon = Icons.Default.LocationOn,
                title = stringResource(R.string.onboarding_location_title),
                description = stringResource(R.string.onboarding_location_description),
                isGranted = locationGranted,
                grantedLabel = locationName ?: stringResource(R.string.onboarding_location_granted),
                buttonLabel = stringResource(R.string.onboarding_grant_location),
                color = MaterialTheme.colorScheme.secondary,
                onRequest = onRequestLocation,
                compact = isCompact
            )

            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

            PermissionCard(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.onboarding_notification_title),
                description = stringResource(R.string.onboarding_notification_description),
                isGranted = notificationGranted,
                grantedLabel = stringResource(R.string.onboarding_notification_granted),
                buttonLabel = stringResource(R.string.onboarding_enable_notifications),
                color = NimazColors.StatusColors.Late,
                onRequest = onRequestNotification,
                compact = isCompact
            )

            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

            PermissionCard(
                icon = Icons.Default.BatteryChargingFull,
                title = stringResource(R.string.onboarding_battery_title),
                description = stringResource(R.string.onboarding_battery_description),
                isGranted = batteryOptDisabled,
                grantedLabel = stringResource(R.string.onboarding_battery_granted),
                buttonLabel = stringResource(R.string.onboarding_disable_battery_opt),
                color = NimazColors.StatusColors.Active,
                onRequest = onRequestBattery,
                compact = isCompact
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    grantedLabel: String,
    buttonLabel: String,
    color: Color,
    onRequest: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                NimazColors.StatusColors.Active.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(if (compact) 40.dp else 48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) NimazColors.StatusColors.Active.copy(alpha = 0.15f)
                        else color.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (isGranted) NimazColors.StatusColors.Active else color,
                    modifier = Modifier.size(if (compact) 20.dp else 24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = if (compact) MaterialTheme.typography.bodyMedium
                    else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isGranted) grantedLabel else description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) NimazColors.StatusColors.Active
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            // Action
            if (!isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    modifier = Modifier.height(if (compact) 36.dp else 40.dp),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_grant),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    text: String,
    color: Color,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 20.dp else 24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(if (compact) 12.dp else 14.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.bodySmall
            else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Info Page")
@Composable
private fun InfoPagePreview() {
    NimazTheme {
        InfoPageContent(
            page = InfoPage(
                title = "Welcome to Nimaz",
                description = "Your complete Islamic companion app",
                icon = Icons.Default.Mosque,
                color = Color(0xFF6750A4),
                features = listOf(
                    "Accurate prayer times",
                    "Complete Quran with audio",
                    "Authentic Hadith collections",
                    "Daily duas and supplications"
                )
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Permissions Page")
@Composable
private fun PermissionsPagePreview() {
    NimazTheme {
        PermissionsPageContent(
            locationGranted = true,
            notificationGranted = false,
            batteryOptDisabled = false,
            locationName = "Dublin, Ireland",
            onRequestLocation = {},
            onRequestNotification = {},
            onRequestBattery = {}
        )
    }
}
