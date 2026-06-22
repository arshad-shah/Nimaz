package com.arshadshah.nimaz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.util.UnstableApi
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.navigation.NavGraph
import com.arshadshah.nimaz.core.util.BootReceiver
import com.arshadshah.nimaz.core.util.InAppUpdateManager
import com.arshadshah.nimaz.data.audio.AdhanPlaybackService
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.data.audio.QuranAudioService
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWidget
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

val LocalInAppUpdateManager = staticCompositionLocalOf<InAppUpdateManager?> { null }

@AndroidEntryPoint
@UnstableApi
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var quranAudioManager: QuranAudioManager

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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash visible until AppInitializer finishes (max 5s timeout)
        val appInitializer = (application as NimazApp).appInitializer
        splashScreen.setKeepOnScreenCondition { !appInitializer.isReady.value }

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
            val localeCode by settingsRepository.appLanguage.collectAsState(initial = "en")

            val themeMode = when (themeModeString) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }

            CompositionLocalProvider(
                LocalInAppUpdateManager provides inAppUpdateManager
            ) {
                NimazTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    hapticEnabled = hapticEnabled,
                    animationsEnabled = animationsEnabled,
                    use24HourFormat = use24HourFormat,
                    useHijriPrimary = useHijriPrimary,
                    showIslamicPatterns = showIslamicPatterns,
                    localeCode = localeCode
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph(
                            pendingQuranSurah = pendingQuranSurah,
                            onPendingQuranSurahConsumed = { pendingQuranSurah = null },
                            pendingIslamicCalendar = pendingIslamicCalendar,
                            onPendingIslamicCalendarConsumed = {
                                pendingIslamicCalendar = false
                            },
                        )
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
     */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(BootReceiver.EXTRA_STOP_ADHAN, false) == true) {
            AdhanPlaybackService.stopAdhan(this)
            AppAnalytics.logNotificationOpened(source = "prayer_notification")
        }

        if (intent?.action == QuranAudioService.ACTION_OPEN_PLAYING_SURAH) {
            AppAnalytics.logNotificationOpened(source = "quran_audio")
            val surah = quranAudioManager.audioState.value.currentSurahNumber
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
