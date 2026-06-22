package com.arshadshah.nimaz.presentation.screens.home

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.LocalInAppUpdateManager
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.UpdateState
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.PrayerTimeCard
import com.arshadshah.nimaz.presentation.components.molecules.PrayerTimesSectionHeader
import com.arshadshah.nimaz.presentation.components.organisms.HomeBannerCarousel
import com.arshadshah.nimaz.presentation.components.organisms.HomeBannerItem
import com.arshadshah.nimaz.presentation.components.organisms.HomeBannerVariant
import com.arshadshah.nimaz.presentation.components.organisms.HomeDynamicTopBar
import com.arshadshah.nimaz.presentation.components.organisms.HomeHeader
import com.arshadshah.nimaz.presentation.components.organisms.HomeHero
import com.arshadshah.nimaz.presentation.components.organisms.JumuahCard
import com.arshadshah.nimaz.presentation.components.organisms.TodayCarousel
import com.arshadshah.nimaz.presentation.components.organisms.TodayInfoCards
import com.arshadshah.nimaz.presentation.components.organisms.TodaysProgressCard
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import com.arshadshah.nimaz.presentation.viewmodel.HomeEvent
import com.arshadshah.nimaz.presentation.viewmodel.HomeUiState
import com.arshadshah.nimaz.presentation.viewmodel.HomeViewModel
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onNavigateToQuran: () -> Unit,
    onNavigateToHadith: () -> Unit,
    onNavigateToDua: () -> Unit,
    onNavigateToTasbih: () -> Unit,
    onNavigateToQibla: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFasting: () -> Unit,
    onNavigateToZakat: () -> Unit,
    onNavigateToPrayerTracker: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPrayerSettings: () -> Unit,
    onNavigateToPrayerTimes: () -> Unit = {},
    onOpenHadith: (hadithId: String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val updateManager = LocalInAppUpdateManager.current
    val updateState = updateManager?.updateState?.collectAsState()?.value ?: UpdateState.Idle

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.onEvent(HomeEvent.RefreshPermissions) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { viewModel.onEvent(HomeEvent.RefreshPermissions) }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.onEvent(HomeEvent.RefreshPermissions) }

    val windowSizeClass = currentWindowSizeClass()

    // Compact-mode scroll state — drives both the LazyColumn and the dynamic
    // top bar's transition progress.
    val compactListState = rememberLazyListState()

    // Continuous 0..1 progress driving the top-bar morph. Maps the hero's
    // scroll-fraction (0 = fully visible, 1 = fully scrolled past) onto the
    // transition, completing it by the time ~60% of the hero has scrolled —
    // so the compact view is fully in place before the hero leaves the
    // viewport. Once item 0 is past, progress stays at 1.
    val topBarProgress by remember {
        derivedStateOf {
            val first = compactListState.layoutInfo.visibleItemsInfo.firstOrNull()
            when {
                first == null -> 0f
                first.index > 0 -> 1f
                first.size <= 0 -> 0f
                else -> {
                    val scrollFraction = (-first.offset.toFloat()) / first.size
                    (scrollFraction / 0.6f).coerceIn(0f, 1f)
                }
            }
        }
    }

    // Status-bar icon contrast: white over the living-sky hero, switching to
    // theme-appropriate (dark icons in a light theme, white in dark) once the
    // top bar solidifies on scroll. Tablet has no sky hero, so it always uses
    // the theme-appropriate contrast. (isAppearanceLightStatusBars == true means
    // a light status-bar background, i.e. dark icons.)
    val view = LocalView.current
    val isLightTheme = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val overSkyHero = windowSizeClass.isCompact && topBarProgress < 0.5f
    val appearanceLightStatusBars = if (overSkyHero) false else isLightTheme
    DisposableEffect(view, appearanceLightStatusBars) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = appearanceLightStatusBars
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }

    val nextPrayerTimeText = state.prayerTimes.find { it.type == state.nextPrayer }?.time ?: ""

    // Draw edge-to-edge: the compact hero's living sky extends behind the
    // status bar and the dynamic top bar is an overlay (below) that manages its
    // own status-bar padding. The tablet path adds statusBarsPadding itself.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                windowSizeClass.isCompact -> {
                    // List draws under the bar; the bar overlays the sky and
                    // morphs from a blended location pill into a prayer summary.
                    Box(modifier = Modifier.fillMaxSize()) {
                        HomeCompactContent(
                            state = state,
                            listState = compactListState,
                            updateState = updateState,
                            updateManager = updateManager,
                            onNavigateToPrayerSettings = onNavigateToPrayerSettings,
                            onNavigateToPrayerTracker = onNavigateToPrayerTracker,
                            onNavigateToPrayerTimes = onNavigateToPrayerTimes,
                            onOpenHadith = onOpenHadith,
                            onTogglePrayer = { viewModel.onEvent(HomeEvent.TogglePrayerStatus(it)) },
                            notificationPermissionLauncher = notificationPermissionLauncher,
                            locationPermissionLauncher = locationPermissionLauncher,
                            batteryOptimizationLauncher = batteryOptimizationLauncher,
                            viewModel = viewModel,
                        )
                        HomeDynamicTopBar(
                            transitionProgress = topBarProgress,
                            locationName = state.locationName,
                            nextPrayer = state.nextPrayer,
                            nextPrayerTime = nextPrayerTimeText,
                            timeUntilNextPrayer = state.timeUntilNextPrayer,
                            onSettingsClick = onNavigateToSettings,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                }

                else -> {
                    HomeTabletContent(
                        state = state,
                        updateState = updateState,
                        updateManager = updateManager,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToPrayerSettings = onNavigateToPrayerSettings,
                        onNavigateToPrayerTracker = onNavigateToPrayerTracker,
                        onNavigateToPrayerTimes = onNavigateToPrayerTimes,
                        onOpenHadith = onOpenHadith,
                        onTogglePrayer = { viewModel.onEvent(HomeEvent.TogglePrayerStatus(it)) },
                        notificationPermissionLauncher = notificationPermissionLauncher,
                        locationPermissionLauncher = locationPermissionLauncher,
                        batteryOptimizationLauncher = batteryOptimizationLauncher,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

// ──── Compact (phone) layout ────────────────────────────────────────────────

@Composable
private fun HomeCompactContent(
    state: HomeUiState,
    listState: LazyListState,
    updateState: UpdateState,
    updateManager: com.arshadshah.nimaz.core.util.InAppUpdateManager?,
    onNavigateToPrayerSettings: () -> Unit,
    onNavigateToPrayerTracker: () -> Unit,
    onNavigateToPrayerTimes: () -> Unit,
    onOpenHadith: (hadithId: String) -> Unit,
    onTogglePrayer: (PrayerType) -> Unit,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    batteryOptimizationLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    viewModel: HomeViewModel,
) {
    val gregorianDate = remember {
        java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    }
    val nextPrayerTime = state.prayerTimes.find { it.type == state.nextPrayer }?.time ?: ""

    // Compact horizontal banner pills — never push content down by more than
    // one pill height regardless of how many banners are active. Built once
    // per state/update change; the LazyColumn just reads the resulting list.
    val banners = buildHomeBannerItems(
        state = state,
        updateState = updateState,
        updateManager = updateManager,
        notificationPermissionLauncher = notificationPermissionLauncher,
        locationPermissionLauncher = locationPermissionLauncher,
        batteryOptimizationLauncher = batteryOptimizationLauncher,
        getBatteryIntent = { viewModel.getBatteryOptimizationIntent() },
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "hero") {
            HomeHero(
                hijriDate = state.hijriDate,
                gregorianDate = gregorianDate,
                nextPrayer = state.nextPrayer,
                nextPrayerTime = nextPrayerTime,
                timeUntilNextPrayer = state.timeUntilNextPrayer
            )
        }

        // Breathing room between the hero's curved bottom and what follows
        // so the two read as distinct containers rather than abutting slabs.
        item(key = "hero_spacer") {
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (banners.isNotEmpty()) {
            item(key = "banners") {
                HomeBannerCarousel(banners = banners)
            }
            item(key = "banners_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (state.isFriday) {
            item {
                JumuahCard(
                    jumuahTime = state.jumuahTime,
                    timeUntilJumuah = state.timeUntilJumuah,
                    isJumuahPassed = state.isJumuahPassed,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        // "Today" section: header + swipeable carousel combining progress,
        // fasting, hadith (and any future widgets) into one card-height pager.
        item(key = "today_section") {
            Spacer(modifier = Modifier.height(8.dp))
            TodayCarousel(
                prayerTimes = state.prayerTimes,
                fastingToday = state.fastingToday,
                dailyHadith = state.dailyHadith,
                dailyHadithReference = state.dailyHadithReference,
                dailyHadithGrade = state.dailyHadithGrade,
                dailyDua = state.dailyDua,
                onHadithClick = state.dailyHadithId?.let { id -> { onOpenHadith(id) } },
                prayerTimelineProgress = state.prayerTimelineProgress,
            )
        }

        item("prayer_times_header") {
            Spacer(modifier = Modifier.height(24.dp))
            PrayerTimesSectionHeader(
                passedCount = state.prayerTimes.count { it.isPassed },
                upcomingCount = state.prayerTimes.count { !it.isPassed },
                onSettingsClick = onNavigateToPrayerSettings,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clickable { onNavigateToPrayerTimes() }
            )
        }

        items(state.prayerTimes) { prayer ->
            PrayerTimeCard(
                prayer = prayer,
                isActive = prayer.type == state.nextPrayer,
                onClick = { onNavigateToPrayerTracker() },
                onToggle = { onTogglePrayer(prayer.type) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
}

// ──── Tablet layout ─────────────────────────────────────────────────────────

@Composable
private fun HomeTabletContent(
    state: HomeUiState,
    updateState: UpdateState,
    updateManager: com.arshadshah.nimaz.core.util.InAppUpdateManager?,
    onNavigateToSettings: () -> Unit,
    onNavigateToPrayerSettings: () -> Unit,
    onNavigateToPrayerTracker: () -> Unit,
    onNavigateToPrayerTimes: () -> Unit,
    onOpenHadith: (hadithId: String) -> Unit,
    onTogglePrayer: (PrayerType) -> Unit,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    batteryOptimizationLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    viewModel: HomeViewModel,
) {
    val gregorianDate = remember {
        java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    }
    val nextPrayerTime = state.prayerTimes.find { it.type == state.nextPrayer }?.time ?: ""

    val banners = buildHomeBannerItems(
        state = state,
        updateState = updateState,
        updateManager = updateManager,
        notificationPermissionLauncher = notificationPermissionLauncher,
        locationPermissionLauncher = locationPermissionLauncher,
        batteryOptimizationLauncher = batteryOptimizationLauncher,
        getBatteryIntent = { viewModel.getBatteryOptimizationIntent() },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        HomeHeader(
            locationName = state.locationName,
            hijriDate = state.hijriDate,
            gregorianDate = gregorianDate,
            nextPrayer = state.nextPrayer,
            nextPrayerTime = nextPrayerTime,
            timeUntilNextPrayer = state.timeUntilNextPrayer,
            onSettingsClick = onNavigateToSettings
        )

        // Tablet shares the same compact pill carousel — keeps both layouts
        // consistent and avoids two separate banner code paths.
        if (banners.isNotEmpty()) {
            HomeBannerCarousel(
                banners = banners,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left column: Prayer times
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                item("prayer_times_header") {
                    PrayerTimesSectionHeader(
                        passedCount = state.prayerTimes.count { it.isPassed },
                        upcomingCount = state.prayerTimes.count { !it.isPassed },
                        onSettingsClick = onNavigateToPrayerSettings,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .clickable { onNavigateToPrayerTimes() }
                    )
                }

                items(state.prayerTimes) { prayer ->
                    PrayerTimeCard(
                        prayer = prayer,
                        isActive = prayer.type == state.nextPrayer,
                        onClick = { onNavigateToPrayerTracker() },
                        onToggle = { onTogglePrayer(prayer.type) },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }

            // Right column: Progress + Today info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.isFriday) {
                    JumuahCard(
                        jumuahTime = state.jumuahTime,
                        timeUntilJumuah = state.timeUntilJumuah,
                        isJumuahPassed = state.isJumuahPassed,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                TodaysProgressCard(
                    prayerTimes = state.prayerTimes,
                    timelineProgress = state.prayerTimelineProgress,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                NimazSectionHeader(
                    title = stringResource(R.string.today),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                TodayInfoCards(
                    fastingToday = state.fastingToday,
                    dailyHadith = state.dailyHadith,
                    dailyHadithReference = state.dailyHadithReference,
                    dailyHadithGrade = state.dailyHadithGrade,
                    dailyDua = state.dailyDua,
                    onHadithClick = state.dailyHadithId?.let { id -> { onOpenHadith(id) } },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ──── Banner item mapping (shared by both layouts) ──────────────────────────

/**
 * Build the list of compact banner pills to display, in priority order
 * (warnings first because they require user action; updates last because
 * they're informational). Order matters: warning banners appear before
 * update banners in the carousel.
 *
 * Returns an empty list when no banners apply — the carousel renders nothing
 * in that case so the layout collapses naturally.
 */
@Composable
private fun buildHomeBannerItems(
    state: HomeUiState,
    updateState: UpdateState,
    updateManager: com.arshadshah.nimaz.core.util.InAppUpdateManager?,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    batteryOptimizationLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    getBatteryIntent: () -> android.content.Intent,
): List<HomeBannerItem> {
    val notificationsDisabledTitle = stringResource(R.string.notifications_disabled_title)
    val locationTitle = stringResource(R.string.location_permission_title)
    val batteryTitle = stringResource(R.string.battery_optimization_title)
    val enable = stringResource(R.string.enable)
    val grant = stringResource(R.string.grant)
    val fix = stringResource(R.string.fix)

    val notificationsSubtitle = stringResource(R.string.notifications_disabled_subtitle)
    val locationSubtitle = stringResource(R.string.location_permission_subtitle)
    val batterySubtitle = stringResource(R.string.battery_optimization_subtitle)
    val updateAvailableSubtitle = stringResource(R.string.update_available_subtitle)
    val updateReadySubtitle = stringResource(R.string.update_ready_subtitle)

    val updateAvailable = stringResource(R.string.update_available)
    val startingUpdate = stringResource(R.string.starting_update)
    val downloadingUpdate = stringResource(R.string.downloading_update)
    val updateReady = stringResource(R.string.update_ready)
    val updateAction = stringResource(R.string.update_action)
    val restart = stringResource(R.string.restart)

    return buildList {
        if (!state.hasNotificationPermission) {
            add(
                HomeBannerItem(
                    id = "notifications",
                    icon = Icons.Default.Notifications,
                    title = notificationsDisabledTitle,
                    variant = HomeBannerVariant.WARNING,
                    subtitle = notificationsSubtitle,
                    actionLabel = enable,
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            )
        }
        if (!state.hasLocationPermission) {
            add(
                HomeBannerItem(
                    id = "location",
                    icon = Icons.Default.LocationOn,
                    title = locationTitle,
                    variant = HomeBannerVariant.WARNING,
                    subtitle = locationSubtitle,
                    actionLabel = grant,
                    onAction = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                )
            )
        }
        if (state.isBatteryOptimized) {
            add(
                HomeBannerItem(
                    id = "battery",
                    icon = Icons.Default.BatteryAlert,
                    title = batteryTitle,
                    variant = HomeBannerVariant.WARNING,
                    subtitle = batterySubtitle,
                    actionLabel = fix,
                    onAction = { batteryOptimizationLauncher.launch(getBatteryIntent()) },
                )
            )
        }
        when (updateState) {
            is UpdateState.UpdateAvailable -> add(
                HomeBannerItem(
                    id = "update_available",
                    icon = Icons.Default.Download,
                    title = updateAvailable,
                    variant = HomeBannerVariant.UPDATE,
                    subtitle = updateAvailableSubtitle,
                    actionLabel = updateAction,
                    onAction = { updateManager?.startUpdate() },
                )
            )

            is UpdateState.Starting -> add(
                HomeBannerItem(
                    id = "starting_update",
                    icon = Icons.Default.Download,
                    title = startingUpdate,
                    variant = HomeBannerVariant.UPDATE,
                    isLoading = true,
                )
            )

            is UpdateState.Downloading -> add(
                HomeBannerItem(
                    id = "downloading",
                    icon = Icons.Default.Download,
                    title = downloadingUpdate,
                    variant = HomeBannerVariant.UPDATE,
                    isLoading = true,
                )
            )

            is UpdateState.Downloaded -> add(
                HomeBannerItem(
                    id = "update_ready",
                    icon = Icons.Default.Refresh,
                    title = updateReady,
                    variant = HomeBannerVariant.UPDATE,
                    subtitle = updateReadySubtitle,
                    actionLabel = restart,
                    onAction = { updateState.completeUpdate() },
                )
            )

            else -> Unit
        }
    }
}
