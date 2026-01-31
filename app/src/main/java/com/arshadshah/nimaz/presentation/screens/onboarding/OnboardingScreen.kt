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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.OnboardingEvent
import com.arshadshah.nimaz.presentation.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val features: List<String> = emptyList(),
    val permissionType: PermissionType? = null
)

enum class PermissionType {
    LOCATION,
    NOTIFICATION,
    BATTERY
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    LocalContext.current
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

    // Show errors
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(OnboardingEvent.DismissError)
        }
    }

    val pages = listOf(
        OnboardingPage(
            title = "Welcome to Nimaz",
            description = "Your complete Islamic companion app for prayer times, Quran, Hadith, and more",
            icon = Icons.Default.Mosque,
            color = MaterialTheme.colorScheme.primary,
            features = listOf(
                "Accurate prayer times",
                "Complete Quran with audio",
                "Authentic Hadith collections",
                "Daily duas and supplications"
            )
        ),
        OnboardingPage(
            title = "Prayer Times",
            description = "Never miss a prayer with accurate times based on your location",
            icon = Icons.Default.Schedule,
            color = MaterialTheme.colorScheme.primary,
            features = listOf(
                "Multiple calculation methods",
                "Custom adjustments",
                "Prayer tracking",
                "Statistics and streaks"
            )
        ),
        OnboardingPage(
            title = "Quran & Hadith",
            description = "Read and listen to the Quran with translations and explore authentic Hadith",
            icon = Icons.Default.MenuBook,
            color = NimazColors.QuranColors.Meccan,
            features = listOf(
                "Multiple translations",
                "Audio recitations",
                "Bookmarks and notes",
                "Search functionality"
            )
        ),
        OnboardingPage(
            title = "Location Access",
            description = "Allow location access for accurate prayer times and Qibla direction",
            icon = Icons.Default.LocationOn,
            color = MaterialTheme.colorScheme.secondary,
            features = listOf(
                "Automatic location detection",
                "Precise prayer times",
                "Accurate Qibla direction",
                "Distance to Mecca"
            ),
            permissionType = PermissionType.LOCATION
        ),
        OnboardingPage(
            title = "Notifications",
            description = "Get reminded for prayers with beautiful Adhan notifications",
            icon = Icons.Default.Notifications,
            color = NimazColors.StatusColors.Late,
            features = listOf(
                "Prayer time alerts",
                "Custom Adhan sounds",
                "Pre-prayer reminders",
                "Quiet hours support"
            ),
            permissionType = PermissionType.NOTIFICATION
        ),
        OnboardingPage(
            title = "Battery Optimization",
            description = "Disable battery optimization for reliable prayer notifications",
            icon = Icons.Default.BatteryChargingFull,
            color = NimazColors.StatusColors.Active,
            features = listOf(
                "Reliable notifications",
                "Background prayer alerts",
                "Consistent reminders",
                "No missed prayers"
            ),
            permissionType = PermissionType.BATTERY
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Skip Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = {
                        viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                        onComplete()
                    }) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    isPermissionGranted = when (pages[page].permissionType) {
                        PermissionType.LOCATION -> state.locationPermissionGranted
                        PermissionType.NOTIFICATION -> state.notificationPermissionGranted
                        PermissionType.BATTERY -> state.batteryOptimizationDisabled
                        null -> false
                    },
                    locationName = if (pages[page].permissionType == PermissionType.LOCATION && state.locationDetected) {
                        state.locationName
                    } else null,
                    onRequestPermission = {
                        when (pages[page].permissionType) {
                            PermissionType.LOCATION -> {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            PermissionType.NOTIFICATION -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    viewModel.onEvent(
                                        OnboardingEvent.UpdatePermissionStatus(notification = true)
                                    )
                                }
                            }
                            PermissionType.BATTERY -> {
                                batteryOptimizationLauncher.launch(
                                    viewModel.getBatteryOptimizationIntent()
                                )
                            }
                            null -> {}
                        }
                    }
                )
            }

            // Bottom Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { index, _ ->
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
                                    if (isSelected) pages[index].color
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back Button
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
                            Text("Back")
                        }
                    }

                    if (pagerState.currentPage == 0) {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Next/Get Started Button
                    Button(
                        onClick = {
                            if (pagerState.currentPage == pages.size - 1) {
                                viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                                onComplete()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pages[pagerState.currentPage].color
                        )
                    ) {
                        Text(
                            if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (pagerState.currentPage == pages.size - 1) {
                                Icons.Default.Check
                            } else {
                                Icons.AutoMirrored.Filled.ArrowForward
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    isPermissionGranted: Boolean,
    locationName: String?,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(page.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.color,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Features Card
        if (page.features.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    page.features.forEach { feature ->
                        FeatureRow(
                            text = feature,
                            color = page.color
                        )
                    }
                }
            }
        }

        // Permission Button
        if (page.permissionType != null) {
            Spacer(modifier = Modifier.height(24.dp))

            if (isPermissionGranted) {
                // Permission granted indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = NimazColors.StatusColors.Active.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NimazColors.StatusColors.Active,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (page.permissionType) {
                            PermissionType.LOCATION -> locationName ?: "Location Access Granted"
                            PermissionType.NOTIFICATION -> "Notifications Enabled"
                            PermissionType.BATTERY -> "Battery Optimization Disabled"
                        },
                        color = NimazColors.StatusColors.Active,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = page.color
                    )
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when (page.permissionType) {
                            PermissionType.LOCATION -> "Grant Location Access"
                            PermissionType.NOTIFICATION -> "Enable Notifications"
                            PermissionType.BATTERY -> "Disable Battery Optimization"
                        }
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Onboarding Welcome")
@Composable
private fun OnboardingWelcomePreview() {
    NimazTheme {
        OnboardingPageContent(
            page = OnboardingPage(
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
            ),
            isPermissionGranted = false,
            locationName = null,
            onRequestPermission = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Onboarding Permission Granted")
@Composable
private fun OnboardingPermissionGrantedPreview() {
    NimazTheme {
        OnboardingPageContent(
            page = OnboardingPage(
                title = "Location Access",
                description = "Allow location access for accurate prayer times",
                icon = Icons.Default.LocationOn,
                color = Color(0xFF625B71),
                features = listOf(
                    "Automatic location detection",
                    "Precise prayer times"
                ),
                permissionType = PermissionType.LOCATION
            ),
            isPermissionGranted = true,
            locationName = "Dublin, Ireland",
            onRequestPermission = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Onboarding Permission Needed")
@Composable
private fun OnboardingPermissionNeededPreview() {
    NimazTheme {
        OnboardingPageContent(
            page = OnboardingPage(
                title = "Notifications",
                description = "Get reminded for prayers",
                icon = Icons.Default.Notifications,
                color = Color(0xFFF97316),
                features = listOf(
                    "Prayer time alerts",
                    "Custom Adhan sounds"
                ),
                permissionType = PermissionType.NOTIFICATION
            ),
            isPermissionGranted = false,
            locationName = null,
            onRequestPermission = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Feature Row")
@Composable
private fun FeatureRowPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FeatureRow(text = "Accurate prayer times", color = Color(0xFF6750A4))
            FeatureRow(text = "Complete Quran with audio", color = Color(0xFF6750A4))
        }
    }
}
