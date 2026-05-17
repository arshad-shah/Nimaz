package com.arshadshah.nimaz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import androidx.media3.common.util.UnstableApi
import com.arshadshah.nimaz.core.navigation.NavGraph
import com.arshadshah.nimaz.core.util.BootReceiver
import com.arshadshah.nimaz.core.util.InAppUpdateManager
import com.arshadshah.nimaz.data.audio.AdhanPlaybackService
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.data.audio.QuranAudioService
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
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
    lateinit var preferencesDataStore: PreferencesDataStore

    @Inject
    lateinit var quranAudioManager: QuranAudioManager

    private lateinit var inAppUpdateManager: InAppUpdateManager

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
        inAppUpdateManager.checkForUpdate()

        setContent {
            val themeModeString by preferencesDataStore.themeMode.collectAsState(initial = "system")
            val dynamicColor by preferencesDataStore.dynamicColor.collectAsState(initial = false)
            val hapticEnabled by preferencesDataStore.hapticFeedback.collectAsState(initial = true)
            val animationsEnabled by preferencesDataStore.animationsEnabled.collectAsState(initial = true)
            val use24HourFormat by preferencesDataStore.use24HourFormat.collectAsState(initial = false)
            val useHijriPrimary by preferencesDataStore.useHijriPrimary.collectAsState(initial = false)
            val showIslamicPatterns by preferencesDataStore.showIslamicPatterns.collectAsState(initial = true)
            val localeCode by preferencesDataStore.appLanguage.collectAsState(initial = "en")

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
        }

        if (intent?.action == QuranAudioService.ACTION_OPEN_PLAYING_SURAH) {
            val surah = quranAudioManager.audioState.value.currentSurahNumber
            if (surah > 0) {
                pendingQuranSurah = surah
            }
        }

        if (intent?.action == HijriCalendarWidget.ACTION_OPEN_ISLAMIC_CALENDAR) {
            pendingIslamicCalendar = true
        }
    }
}
