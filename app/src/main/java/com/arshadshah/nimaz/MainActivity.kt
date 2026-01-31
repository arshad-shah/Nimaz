package com.arshadshah.nimaz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.arshadshah.nimaz.core.navigation.NavGraph
import com.arshadshah.nimaz.core.util.BootReceiver
import com.arshadshah.nimaz.core.util.InAppUpdateManager
import com.arshadshah.nimaz.data.audio.AdhanPlaybackService
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

val LocalInAppUpdateManager = staticCompositionLocalOf<InAppUpdateManager?> { null }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    private lateinit var inAppUpdateManager: InAppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                    showIslamicPatterns = showIslamicPatterns
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph()
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
     * Handle intents - specifically for stopping adhan when opened from notification.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(BootReceiver.EXTRA_STOP_ADHAN, false) == true) {
            // Stop the adhan playback service
            AdhanPlaybackService.stopAdhan(this)
        }
    }
}
