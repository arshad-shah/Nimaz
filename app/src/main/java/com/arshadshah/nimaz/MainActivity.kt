package com.arshadshah.nimaz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.navigation.NavGraph
import com.arshadshah.nimaz.core.util.PrayerAlarmReceiver
import com.arshadshah.nimaz.core.util.InAppUpdateManager
import com.arshadshah.nimaz.data.announcement.AnnouncementPayloadMapper
import com.arshadshah.nimaz.data.audio.AdhanPlaybackService
import com.arshadshah.nimaz.domain.repository.QuranPlayback
import com.arshadshah.nimaz.data.audio.QuranAudioService
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.AnnouncementUseCases
import com.arshadshah.nimaz.presentation.components.atoms.NimazPatternBackground
import com.arshadshah.nimaz.presentation.components.atoms.ProvideNimazClock
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWidget
import com.arshadshah.nimaz.presentation.app.AppIdentity
import com.arshadshah.nimaz.presentation.app.LocalAppIdentity
import com.arshadshah.nimaz.presentation.update.LocalAppUpdateController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App identity, stated once at the composition root.
 *
 * `BuildConfig` and the launcher mipmap are both `:app`-only — a library's `BuildConfig` holds
 * only its own fields, and `nonTransitiveRClass` keeps the application's `R` off a library's
 * classpath — so a feature module cannot read either. See `presentation/app/AppIdentity.kt`.
 */
private val appIdentity = AppIdentity(
    versionName = BuildConfig.VERSION_NAME,
    versionCode = BuildConfig.VERSION_CODE,
    iconRes = R.mipmap.ic_launcher_foreground,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    /**
     * Read only for [QuranAudioService.ACTION_OPEN_PLAYING_SURAH] — which surah the notification
     * was showing when it was tapped.
     *
     * The **port**, not `QuranAudioManager`. That class is `:core:audio`'s now, and while `:app`
     * can see it, naming it here would put an `@UnstableApi` ExoPlayer type on the composition
     * root for one property read.
     */
    @Inject
    lateinit var quranPlayback: QuranPlayback

    @Inject
    lateinit var announcementUseCases: AnnouncementUseCases

    @Inject
    lateinit var announcementPayloadMapper: AnnouncementPayloadMapper

    private lateinit var inAppUpdateManager: InAppUpdateManager

    // Receives the result of the Play in-app update confirmation dialog so the
    // manager can fall back to an interactive state if the user cancels. Must be
    // registered before the activity is STARTED, hence a field initializer.
    private val updateFlowLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (::inAppUpdateManager.isInitialized) {
                inAppUpdateManager.onUpdateFlowResult(result.resultCode)
            }
        }

    // Pending surah to navigate to from a Quran audio notification tap. NavGraph
    // observes this and consumes it after navigation.
    private var pendingQuranSurah by mutableStateOf<Int?>(null)

    // Pending Islamic Calendar deep-link from the home-screen Hijri calendar
    // widget. NavGraph observes this, navigates with popUpTo(Home) so system
    // Back returns the user to the home screen rather than dropping out.
    private var pendingIslamicCalendar by mutableStateOf(false)

    // Pending announcement route from a tapped FCM tray notification (cold or
    // warm start). NavGraph resolves it against the allowlist and consumes it.
    private var pendingAnnouncementRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash visible until AppInitializer finishes (max 5s timeout).
        // Guard the cast: under instrumented tests the host Application is
        // HiltTestApplication, not NimazApp, so there is no initializer to await —
        // lift the splash immediately so the UI is visible to the test harness.
        val appInitializer = (application as? NimazApp)?.appInitializer
        splashScreen.setKeepOnScreenCondition { appInitializer?.isReady?.value == false }

        // Check if opened from prayer notification - stop adhan if so
        handleIntent(intent)

        // Initialize in-app update manager
        inAppUpdateManager = InAppUpdateManager(this)
        inAppUpdateManager.setUpdateFlowLauncher(updateFlowLauncher)
        inAppUpdateManager.checkForUpdate()

        setContent {
            val themeModeString by settingsRepository.themeMode.collectAsState(initial = "system")
            val dynamicColor by settingsRepository.dynamicColor.collectAsState(initial = false)
            val hapticEnabled by settingsRepository.hapticFeedback.collectAsState(initial = true)
            val animationsEnabled by settingsRepository.animationsEnabled.collectAsState(initial = true)
            val use24HourFormat by settingsRepository.use24HourFormat.collectAsState(initial = false)
            val useHijriPrimary by settingsRepository.useHijriPrimary.collectAsState(initial = false)
            val showIslamicPatterns by settingsRepository.showIslamicPatterns.collectAsState(
                initial = true
            )
            val patternStyleKey by settingsRepository.patternStyle.collectAsState(
                initial = "CORNER_MEDALLION"
            )
            val localeCode by settingsRepository.appLanguage.collectAsState(initial = "en")

            // NONE is "off": if the reader disabled patterns via the legacy boolean,
            // honour that regardless of the stored style so the two can't disagree.
            val patternStyle = NimazPatternStyle.fromKey(patternStyleKey)
                .takeIf { showIslamicPatterns } ?: NimazPatternStyle.NONE

            val themeMode = when (themeModeString) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }

            CompositionLocalProvider(
                LocalAppUpdateController provides inAppUpdateManager,
                LocalAppIdentity provides appIdentity,
            ) {
                NimazTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    hapticEnabled = hapticEnabled,
                    animationsEnabled = animationsEnabled,
                    use24HourFormat = use24HourFormat,
                    useHijriPrimary = useHijriPrimary,
                    showIslamicPatterns = showIslamicPatterns,
                    patternStyle = patternStyle,
                    localeCode = localeCode
                ) {
                    // One shared, lifecycle-aware ticker for the whole app — the single source
                    // countdowns and clocks read via rememberNow, replacing the per-screen 1s/30s/60s
                    // loops. Installed immediately inside the theme so every screen shares it.
                    ProvideNimazClock {
                        // The app-wide decorative background. Replaces the plain
                        // Surface: it paints the same background colour, then draws
                        // the ornament behind the whole nav graph. Screens show it
                        // through by using NimazScreenScaffold (whose container is
                        // transparent) instead of a bare Scaffold.
                        NimazPatternBackground(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            NavGraph(
                                pendingQuranSurah = pendingQuranSurah,
                                onPendingQuranSurahConsumed = { pendingQuranSurah = null },
                                pendingIslamicCalendar = pendingIslamicCalendar,
                                onPendingIslamicCalendarConsumed = {
                                    pendingIslamicCalendar = false
                                },
                                pendingAnnouncementRoute = pendingAnnouncementRoute,
                                onPendingAnnouncementRouteConsumed = {
                                    pendingAnnouncementRoute = null
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for stalled updates (download completed while app was in background)
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.checkForStalledUpdate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.cleanup()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Handle intents from external entry points:
     * - Prayer notification → stop adhan playback.
     * - Quran audio notification → deep-link to the playing surah.
     * - Hijri calendar widget → deep-link to the Islamic Calendar screen.
     * - FCM announcement notification tap → persist the announcement so the
     *   Home banner shows, and deep-link to its route if it names one.
     */
    private fun handleIntent(intent: Intent?) {
        // Backgrounded FCM notification tap: the OS copies the message's custom
        // data onto the launcher intent as string extras. The mapper returning
        // non-null is what identifies the intent as ours.
        announcementPayloadMapper.fromIntentExtras(intent?.extras)?.let { announcement ->
            AppAnalytics.logNotificationOpened(source = "announcement")
            lifecycleScope.launch { announcementUseCases.setAnnouncement(announcement) }
            // Tapping the notification implies intent to act — navigate when a
            // route is present; otherwise land on Home with the banner showing.
            if (announcement.route != null) {
                pendingAnnouncementRoute = announcement.route
            }
        }

        if (intent?.getBooleanExtra(PrayerAlarmReceiver.EXTRA_STOP_ADHAN, false) == true) {
            AdhanPlaybackService.stopAdhan(this)
            AppAnalytics.logNotificationOpened(source = "prayer_notification")
        }

        if (intent?.action == QuranAudioService.ACTION_OPEN_PLAYING_SURAH) {
            AppAnalytics.logNotificationOpened(source = "quran_audio")
            val surah = quranPlayback.audioState.value.currentSurahNumber
            if (surah > 0) {
                pendingQuranSurah = surah
            }
        }

        if (intent?.action == HijriCalendarWidget.ACTION_OPEN_ISLAMIC_CALENDAR) {
            AppAnalytics.logNotificationOpened(source = "hijri_calendar_widget")
            pendingIslamicCalendar = true
        }
    }
}
