package com.arshadshah.nimaz

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanDownloadService
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NimazApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var prayerNotificationScheduler: PrayerNotificationScheduler

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    @Inject
    lateinit var adhanAudioManager: AdhanAudioManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleInitialNotifications()
        downloadDefaultAdhanIfNeeded()
    }

    private fun scheduleInitialNotifications() {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val prefs = preferencesDataStore.userPreferences.first()
                if (prefs.latitude != 0.0 && prefs.longitude != 0.0) {
                    prayerNotificationScheduler.scheduleTodaysPrayerNotifications(
                        latitude = prefs.latitude,
                        longitude = prefs.longitude,
                        notificationsEnabled = prefs.prayerNotificationsEnabled
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Downloads the default adhan (Mishary) on first launch if not already downloaded.
     * This ensures adhans are ready for notifications.
     */
    private fun downloadDefaultAdhanIfNeeded() {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                // Check if default adhan is already downloaded
                val defaultSound = AdhanSound.MISHARY
                if (!adhanAudioManager.isFullyDownloaded(defaultSound)) {
                    // Start the download service
                    AdhanDownloadService.downloadDefault(this@NimazApp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
