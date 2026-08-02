package com.arshadshah.nimaz.presentation.screens.home

import android.Manifest
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.LocalInAppUpdateManager
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.FULL_DATE_FORMATTER
import com.arshadshah.nimaz.core.util.UpdateState
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.components.molecules.AnnouncementBanner
import com.arshadshah.nimaz.presentation.components.molecules.PrayerTimeCard
import com.arshadshah.nimaz.presentation.components.molecules.PrayerTimesSectionHeader
import com.arshadshah.nimaz.presentation.components.organisms.EventAction
import com.arshadshah.nimaz.presentation.components.organisms.EventCardUi
import com.arshadshah.nimaz.presentation.components.organisms.EventOccasion
import com.arshadshah.nimaz.presentation.components.organisms.EventsCarousel
import com.arshadshah.nimaz.presentation.components.organisms.HomeBannerCarousel
import com.arshadshah.nimaz.presentation.components.organisms.HomeBannerItem
import com.arshadshah.nimaz.presentation.components.organisms.HomeBannerVariant
import com.arshadshah.nimaz.presentation.components.organisms.HomeDynamicTopBar
import com.arshadshah.nimaz.presentation.components.organisms.HomeHeader
import com.arshadshah.nimaz.presentation.components.organisms.HomeHero
import com.arshadshah.nimaz.presentation.components.organisms.TodayCarousel
import com.arshadshah.nimaz.presentation.components.organisms.TodayInfoCards
import com.arshadshah.nimaz.presentation.components.organisms.TodaysProgressCard
import com.arshadshah.nimaz.presentation.components.organisms.toOccasion
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import com.arshadshah.nimaz.presentation.viewmodel.AnnouncementUiState
import com.arshadshah.nimaz.presentation.viewmodel.HomeEvent
import com.arshadshah.nimaz.presentation.viewmodel.HomeUiState
import com.arshadshah.nimaz.presentation.viewmodel.HomeViewModel
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimeDisplay
import com.arshadshah.nimaz.presentation.viewmodel.withClockState
import kotlin.time.Instant

/**
 * The clock-derived slice of Home. The ViewModel publishes prayer *instants*; this turns them into
 * "which one is next / current / passed" and the instant to count down to, re-deriving on the
 * shared ticker.
 *
 * Minute resolution on purpose: these flags only change on minute boundaries, so reading seconds
 * here would recompose the whole screen 60× more often for no visible difference. The countdowns
 * themselves escalate to seconds inside the leaf components that show them.
 */
private data class HomeClock(
    val prayers: List<PrayerTimeDisplay>,
    val nextPrayer: PrayerType?,
    val nextPrayerAt: Instant?,
)

@Composable
private fun rememberHomeClock(state: HomeUiState): HomeClock {
    val now by rememberNow(TickResolution.MINUTES)
    return remember(state.prayerTimes, state.tomorrowFajrAt, now) {
        val prayers = state.prayerTimes.withClockState(now)
        val next = prayers.firstOrNull { it.isNext }
        HomeClock(
            prayers = prayers,
            // After Isha there is no "next" today, so wrap to tomorrow's Fajr.
            nextPrayer = next?.type ?: PrayerType.FAJR,
            nextPrayerAt = next?.timeAt ?: state.tomorrowFajrAt,
        )
    }
}

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
    onOpenAnnouncementRoute: (String) -> Unit = {},
    /** Tapping the "Next Worship" card; NavGraph maps the type to a destination. */
    onOpenWorship: (WorshipReminderType) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val announcementState by viewModel.announcement.collectAsStateWithLifecycle()

    val onAnnouncementCta: () -> Unit = {
        viewModel.onEvent(HomeEvent.AnnouncementCtaClicked)
        announcementState.announcement?.route?.let { onOpenAnnouncementRoute(it) }
    }
    val onAnnouncementDismiss: () -> Unit = {
        viewModel.onEvent(HomeEvent.DismissAnnouncement)
    }
    val homeClock = rememberHomeClock(state)
    val updateManager = LocalInAppUpdateManager.current
    val updateState = updateManager?.updateState?.collectAsStateWithLifecycle()?.value ?: UpdateState.Idle

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.onEvent(HomeEvent.RefreshPermissions) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { viewModel.onEvent(HomeEvent.RefreshPermissions) }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.onEvent(HomeEvent.RefreshPermissions) }

    // Date rollover: with the per-second ViewModel loop gone, nothing else notices midnight.
    // The ticker reads the system clock, so this also covers timezone and manual time changes.
    val nowForDate by rememberNow(TickResolution.MINUTES)
    val today = remember(nowForDate) {
        java.time.Instant.ofEpochMilli(nowForDate.toEpochMilliseconds())
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }
    var lastSeenDate by remember { mutableStateOf(today) }
    LaunchedEffect(today) {
        if (today != lastSeenDate) {
            lastSeenDate = today
            viewModel.onEvent(HomeEvent.RefreshPrayerTimes)
        }
    }

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

    // Draw edge-to-edge: the compact hero's living sky extends behind the
    // status bar and the dynamic top bar is an overlay (below) that manages its
    // own status-bar padding. The tablet path adds statusBarsPadding itself.
    NimazScreenScaffold(
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
                            announcementState = announcementState,
                            onAnnouncementCta = onAnnouncementCta,
                            onAnnouncementDismiss = onAnnouncementDismiss,
                            listState = compactListState,
                            updateState = updateState,
                            updateManager = updateManager,
                            onNavigateToPrayerSettings = onNavigateToPrayerSettings,
                            onNavigateToPrayerTracker = onNavigateToPrayerTracker,
                            onNavigateToPrayerTimes = onNavigateToPrayerTimes,
                            onOpenHadith = onOpenHadith,
                            onOpenAnnouncementRoute = onOpenAnnouncementRoute,
                            onOpenWorship = onOpenWorship,
                            onTogglePrayer = { viewModel.onEvent(HomeEvent.TogglePrayerStatus(it)) },
                            notificationPermissionLauncher = notificationPermissionLauncher,
                            locationPermissionLauncher = locationPermissionLauncher,
                            batteryOptimizationLauncher = batteryOptimizationLauncher,
                            viewModel = viewModel,
                        )
                        HomeDynamicTopBar(
                            transitionProgress = topBarProgress,
                            locationName = state.locationName,
                            nextPrayer = homeClock.nextPrayer,
                            nextPrayerAt = homeClock.nextPrayerAt,
                            onSettingsClick = onNavigateToSettings,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                }

                else -> {
                    HomeTabletContent(
                        state = state,
                        announcementState = announcementState,
                        onAnnouncementCta = onAnnouncementCta,
                        onAnnouncementDismiss = onAnnouncementDismiss,
                        updateState = updateState,
                        updateManager = updateManager,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToPrayerSettings = onNavigateToPrayerSettings,
                        onNavigateToPrayerTracker = onNavigateToPrayerTracker,
                        onNavigateToPrayerTimes = onNavigateToPrayerTimes,
                        onOpenHadith = onOpenHadith,
                        onOpenAnnouncementRoute = onOpenAnnouncementRoute,
                        onOpenWorship = onOpenWorship,
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
    announcementState: AnnouncementUiState,
    onAnnouncementCta: () -> Unit,
    onAnnouncementDismiss: () -> Unit,
    listState: LazyListState,
    updateState: UpdateState,
    updateManager: com.arshadshah.nimaz.core.util.InAppUpdateManager?,
    onNavigateToPrayerSettings: () -> Unit,
    onNavigateToPrayerTracker: () -> Unit,
    onNavigateToPrayerTimes: () -> Unit,
    onOpenHadith: (hadithId: String) -> Unit,
    onOpenAnnouncementRoute: (String) -> Unit,
    onOpenWorship: (WorshipReminderType) -> Unit,
    onTogglePrayer: (PrayerType) -> Unit,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    batteryOptimizationLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    viewModel: HomeViewModel,
) {
    val gregorianDate = remember {
        java.time.LocalDate.now().format(FULL_DATE_FORMATTER)
    }
    val homeClock = rememberHomeClock(state)
    val jumuahMubarak = stringResource(R.string.jumuah_mubarak)
    val jumuahHadithQuote = stringResource(R.string.jumuah_hadith_quote)

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
                nextPrayer = homeClock.nextPrayer,
                nextPrayerAt = homeClock.nextPrayerAt,
                sunriseFraction = state.sunriseFraction,
                sunsetFraction = state.sunsetFraction,
            )
        }

        // Breathing room between the hero's curved bottom and what follows
        // so the two read as distinct containers rather than abutting slabs.
        item(key = "hero_spacer") {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // FCM engagement announcement — dismissable, renders nothing (zero
        // height) when no announcement is active.
        item(key = "announcement") {
            AnnouncementBanner(
                announcement = announcementState.announcement,
                showCta = announcementState.showCta,
                onCtaClick = onAnnouncementCta,
                onDismiss = onAnnouncementDismiss,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            )
        }

        if (banners.isNotEmpty()) {
            item(key = "banners") {
                HomeBannerCarousel(banners = banners)
            }
            item(key = "banners_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        val eventCards = buildList {
            // "Next Worship" card leads when an extended reminder is enabled and near.
            state.worshipCard?.let { w ->
                add(EventCardUi(occasion = EventOccasion.GENERIC, eyebrow = w.name, body = w.body, worship = w))
            }
            if (state.isFriday) {
                // Jumu'ah routes to JumuahCard, which sources its own strings; eyebrow/headline/body here are unused.
                add(
                    EventCardUi(
                        occasion = EventOccasion.JUMUAH,
                        eyebrow = jumuahMubarak,
                        body = jumuahHadithQuote,
                        jumuahAt = state.jumuahAt,
                    )
                )
            }
            state.celebrationCards.forEach { c ->
                add(
                    EventCardUi(
                        occasion = c.event.toOccasion(),
                        eyebrow = c.eyebrow,
                        // Direction A: compact card — name (eyebrow) + arabic + one body line + one
                        // action, matching the Jumu'ah card's height.
                        body = c.body,
                        arabic = c.arabic,
                        primaryAction = if (c.ctaLabel != null && c.route != null)
                            EventAction(c.ctaLabel) { onOpenAnnouncementRoute(c.route) } else null,
                        onDismiss = if (c.dismissable && c.announcementId != null)
                            { { viewModel.onEvent(HomeEvent.DismissAnnouncement) } } else null,
                    )
                )
            }
        }
        if (eventCards.isNotEmpty()) {
            item(key = "events") {
                EventsCarousel(events = eventCards, onWorshipClick = onOpenWorship)
            }
            item(key = "events_spacer") {
                Spacer(Modifier.height(16.dp))
            }
        }

        // "Today" section: header + swipeable carousel combining progress,
        // fasting, hadith (and any future widgets) into one card-height pager.
        item(key = "today_section") {
            Spacer(modifier = Modifier.height(8.dp))
            TodayCarousel(
                prayerTimes = homeClock.prayers,
                fastingToday = state.fastingToday,
                dailyHadith = state.dailyHadith,
                dailyHadithReference = state.dailyHadithReference,
                dailyHadithGrade = state.dailyHadithGrade,
                dailyDua = state.dailyDua,
                onHadithClick = state.dailyHadithId?.let { id -> { onOpenHadith(id) } },
            )
        }

        item("prayer_times_header") {
            Spacer(modifier = Modifier.height(24.dp))
            PrayerTimesSectionHeader(
                passedCount = homeClock.prayers.count { it.isPassed },
                upcomingCount = homeClock.prayers.count { !it.isPassed },
                onSettingsClick = onNavigateToPrayerSettings,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clickable { onNavigateToPrayerTimes() }
            )
        }

        items(homeClock.prayers, key = { it.type }) { prayer ->
            PrayerTimeCard(
                prayer = prayer,
                isActive = prayer.isNext,
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
    announcementState: AnnouncementUiState,
    onAnnouncementCta: () -> Unit,
    onAnnouncementDismiss: () -> Unit,
    updateState: UpdateState,
    updateManager: com.arshadshah.nimaz.core.util.InAppUpdateManager?,
    onNavigateToSettings: () -> Unit,
    onNavigateToPrayerSettings: () -> Unit,
    onNavigateToPrayerTracker: () -> Unit,
    onNavigateToPrayerTimes: () -> Unit,
    onOpenHadith: (hadithId: String) -> Unit,
    onOpenAnnouncementRoute: (String) -> Unit,
    onOpenWorship: (WorshipReminderType) -> Unit,
    onTogglePrayer: (PrayerType) -> Unit,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    batteryOptimizationLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    viewModel: HomeViewModel,
) {
    val gregorianDate = remember {
        java.time.LocalDate.now().format(FULL_DATE_FORMATTER)
    }
    val homeClock = rememberHomeClock(state)

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
            nextPrayer = homeClock.nextPrayer,
            nextPrayerAt = homeClock.nextPrayerAt,
            onSettingsClick = onNavigateToSettings,
        )

        // FCM engagement announcement — same banner as the compact layout.
        AnnouncementBanner(
            announcement = announcementState.announcement,
            showCta = announcementState.showCta,
            onCtaClick = onAnnouncementCta,
            onDismiss = onAnnouncementDismiss,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
        )

        // Tablet shares the same compact pill carousel — keeps both layouts
        // consistent and avoids two separate banner code paths.
        if (banners.isNotEmpty()) {
            HomeBannerCarousel(
                banners = banners,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val tabletEventCards = buildList {
            state.worshipCard?.let { w ->
                add(EventCardUi(occasion = EventOccasion.GENERIC, eyebrow = w.name, body = w.body, worship = w))
            }
            if (state.isFriday) {
                // Jumu'ah routes to JumuahCard, which sources its own strings; eyebrow/headline/body here are unused.
                add(
                    EventCardUi(
                        occasion = EventOccasion.JUMUAH,
                        eyebrow = stringResource(R.string.jumuah_mubarak),
                        body = stringResource(R.string.jumuah_hadith_quote),
                        jumuahAt = state.jumuahAt,
                    )
                )
            }
            state.celebrationCards.forEach { c ->
                add(
                    EventCardUi(
                        occasion = c.event.toOccasion(),
                        eyebrow = c.eyebrow,
                        // Direction A: compact card — name (eyebrow) + arabic + one body line + one
                        // action, matching the Jumu'ah card's height.
                        body = c.body,
                        arabic = c.arabic,
                        primaryAction = if (c.ctaLabel != null && c.route != null)
                            EventAction(c.ctaLabel) { onOpenAnnouncementRoute(c.route) } else null,
                        onDismiss = if (c.dismissable && c.announcementId != null)
                            { { viewModel.onEvent(HomeEvent.DismissAnnouncement) } } else null,
                    )
                )
            }
        }
        if (tabletEventCards.isNotEmpty()) {
            EventsCarousel(
                events = tabletEventCards,
                modifier = Modifier.padding(top = 8.dp),
                onWorshipClick = onOpenWorship,
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
                        passedCount = homeClock.prayers.count { it.isPassed },
                        upcomingCount = homeClock.prayers.count { !it.isPassed },
                        onSettingsClick = onNavigateToPrayerSettings,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .clickable { onNavigateToPrayerTimes() }
                    )
                }

                items(homeClock.prayers, key = { it.type }) { prayer ->
                    PrayerTimeCard(
                        prayer = prayer,
                        isActive = prayer.isNext,
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
                TodaysProgressCard(
                    prayerTimes = homeClock.prayers,
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
