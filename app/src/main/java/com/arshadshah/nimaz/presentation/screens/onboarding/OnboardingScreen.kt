package com.arshadshah.nimaz.presentation.screens.onboarding

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazPageIndicator
import com.arshadshah.nimaz.presentation.components.atoms.NimazPager
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.rememberNimazPagerState
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.onboarding.OnboardingEvent
import com.arshadshah.nimaz.presentation.viewmodel.onboarding.OnboardingViewModel
import kotlinx.coroutines.launch

private data class InfoPage(
    val title: String,
    val description: String,
    val emblem: OnboardingEmblem,
    val features: List<String>
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Onboarding always uses a dark, illuminated background, so the status-bar
    // icons must be light regardless of the app theme — otherwise dark icons
    // disappear into the dark background. Restore the previous setting on exit.
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            val previousLightStatus = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = false
            onDispose {
                controller.isAppearanceLightStatusBars = previousLightStatus
            }
        }
    }

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
            emblem = OnboardingEmblem.MOSQUE,
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
            emblem = OnboardingEmblem.PRAYER_TIMES,
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
            emblem = OnboardingEmblem.QURAN,
            features = listOf(
                stringResource(R.string.onboarding_feature_translations),
                stringResource(R.string.onboarding_feature_audio),
                stringResource(R.string.onboarding_feature_bookmarks),
                stringResource(R.string.onboarding_feature_search)
            )
        )
    )

    val totalPages = infoPages.size + 1 // +1 for permissions page
    val pagerState = rememberNimazPagerState(pageCount = { totalPages })

    NimazScreenScaffold(
        // Opts out of the app ornament: onboarding owns its own gradient backdrop.
        containerColor = NimazColors.OnboardingBgTop,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        // Let the illuminated background bleed all the way up behind the status
        // bar (edge-to-edge); only the interactive content respects the insets.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(illuminatedBackground)
        ) {
            KhatamBand(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .align(Alignment.TopCenter)
            )
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
                        NimazButton(
                            text = stringResource(R.string.onboarding_skip),
                            onClick = {
                                viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
                                onComplete()
                            },
                            variant = NimazButtonVariant.DESTRUCTIVE
                        )
                    }
                }

                // Pager
                NimazPager(
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
                                    viewModel.onEvent(
                                        OnboardingEvent.UpdatePermissionStatus(
                                            notification = true
                                        )
                                    )
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
                    // Page Indicators — canonical pill indicator tinted to the
                    // illuminated palette (gold active dot on the dark background).
                    NimazPageIndicator(
                        state = pagerState,
                        activeColor = IllumGold,
                        inactiveColor = Color.White.copy(alpha = 0.28f),
                    )

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
                            NimazButton(
                                text = stringResource(R.string.onboarding_back),
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                variant = NimazButtonVariant.OUTLINED
                            )
                        }

                        if (pagerState.currentPage == 0) {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        NimazButton(
                            text = if (pagerState.currentPage == totalPages - 1)
                                stringResource(R.string.onboarding_get_started)
                            else
                                stringResource(R.string.onboarding_next),
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
                            variant = NimazButtonVariant.TONAL,
                            trailingIcon = if (pagerState.currentPage == totalPages - 1)
                                Icons.Default.Check
                            else Icons.AutoMirrored.Filled.ArrowForward
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
        val sectionSpacing = if (isCompact) 16.dp else 32.dp
        val smallSpacing = if (isCompact) 6.dp else 12.dp
        val emblemHeight = if (isCompact) 150.dp else 196.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = if (isCompact) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OnboardingEmblem(
                kind = page.emblem,
                modifier = Modifier.size(width = emblemHeight * 0.8f, height = emblemHeight)
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = page.title,
                style = if (isCompact) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                color = IllumCream
            )

            Spacer(modifier = Modifier.height(smallSpacing))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = IllumTextSoft
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            NimazCard(
                style = NimazCardStyle.FILLED,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = NimazCardDefaults.colors(
                    container = Color.White.copy(alpha = 0.05f),
                    border = IllumGold.copy(alpha = 0.18f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = if (isCompact) 10.dp else 16.dp
                    )
                ) {
                    page.features.forEach { feature ->
                        FeatureRow(text = feature, compact = isCompact)
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
        val sectionSpacing = if (isCompact) 12.dp else 20.dp
        val emblemHeight = if (isCompact) 110.dp else 140.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = if (isCompact) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header emblem — shield enclosing a khatam star
            OnboardingEmblem(
                kind = OnboardingEmblem.SHIELD,
                modifier = Modifier.size(width = emblemHeight * 0.78f, height = emblemHeight)
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = stringResource(R.string.onboarding_permissions_title),
                style = if (isCompact) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                color = IllumCream
            )

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))

            Text(
                text = stringResource(R.string.onboarding_permissions_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = IllumTextSoft
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
    onRequest: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val green = NimazColors.StatusColors.Active

    NimazCard(
        style = NimazCardStyle.OUTLINED,
        colors = NimazCardDefaults.colors(
            container = Color.Transparent,
            content = MaterialTheme.colorScheme.onSurface,
            border = if (isGranted) green else MaterialTheme.colorScheme.secondary
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
                        if (isGranted) green.copy(alpha = 0.2f)
                        else IllumGold.copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                NimazIcon(
                    imageVector = if (isGranted) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (isGranted) green else IllumGold,
                    iconSize = if (compact) 20.dp else 24.dp
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
                    color = Color.White
                )
                Text(
                    text = if (isGranted) grantedLabel else description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGranted) green else IllumTextSoft,
                    maxLines = 2
                )
            }

            // Action
            if (!isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                NimazButton(
                    text = stringResource(R.string.onboarding_grant),
                    onClick = onRequest,
                    variant = NimazButtonVariant.TONAL,
                    size = NimazButtonSize.SMALL
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    text: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 5.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "✦", // ✦ gold star bullet
            color = IllumGold,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.bodySmall
            else MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.92f)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 740, name = "Info Page")
@Composable
private fun InfoPagePreview() {
    NimazTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(illuminatedBackground)
        ) {
            InfoPageContent(
                page = InfoPage(
                    title = "Welcome to Nimaz",
                    description = "Your complete Islamic companion app",
                    emblem = OnboardingEmblem.MOSQUE,
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
}

@Preview(showBackground = true, widthDp = 400, heightDp = 740, name = "Permissions Page")
@Composable
private fun PermissionsPagePreview() {
    NimazTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(illuminatedBackground)
        ) {
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
}
