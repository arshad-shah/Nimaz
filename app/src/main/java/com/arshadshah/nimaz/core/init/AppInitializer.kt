package com.arshadshah.nimaz.core.init

import android.content.Context
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.LocaleHelper
import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanDownloadService
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.PrayerType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: PreferencesDataStore,
    private val prayerNotificationScheduler: PrayerNotificationScheduler,
    private val adhanAudioManager: AdhanAudioManager,
) {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initialize() {
        scope.launch {
            try {
                withTimeout(5_000L) {
                    val localeTask = async { applySavedLocale() }
                    val notificationTask = async { scheduleInitialNotifications() }
                    val adhanTask = async { downloadDefaultAdhanIfNeeded() }

                    localeTask.await()
                    notificationTask.await()
                    adhanTask.await()
                }
            } catch (e: Exception) {
                // Timeout or other failure — report it but proceed to UI anyway
                CrashReporter.recordException(e)
            } finally {
                _isReady.value = true
            }
        }
    }

    private suspend fun applySavedLocale() {
        try {
            val langCode = preferencesDataStore.appLanguage.first()
            if (langCode.isNotEmpty() && langCode != "en") {
                LocaleHelper.setLocale(context, langCode)
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }

    private suspend fun scheduleInitialNotifications() {
        try {
            val prefs = preferencesDataStore.userPreferences.first()
            if (prefs.latitude != 0.0 && prefs.longitude != 0.0) {
                val enabledPrayers = buildSet {
                    if (preferencesDataStore.fajrNotificationEnabled.first()) add(PrayerType.FAJR)
                    if (preferencesDataStore.sunriseNotificationEnabled.first()) add(PrayerType.SUNRISE)
                    if (preferencesDataStore.dhuhrNotificationEnabled.first()) add(PrayerType.DHUHR)
                    if (preferencesDataStore.asrNotificationEnabled.first()) add(PrayerType.ASR)
                    if (preferencesDataStore.maghribNotificationEnabled.first()) add(PrayerType.MAGHRIB)
                    if (preferencesDataStore.ishaNotificationEnabled.first()) add(PrayerType.ISHA)
                }

                val preReminderEnabled = preferencesDataStore.showReminderBefore.first()
                val preReminderMinutes = preferencesDataStore.notificationReminderMinutes.first()

                prayerNotificationScheduler.scheduleTodaysPrayerNotifications(
                    latitude = prefs.latitude,
                    longitude = prefs.longitude,
                    notificationsEnabled = prefs.prayerNotificationsEnabled,
                    enabledPrayers = enabledPrayers,
                    preReminderEnabled = preReminderEnabled,
                    preReminderMinutes = preReminderMinutes
                )
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }

    private suspend fun downloadDefaultAdhanIfNeeded() {
        try {
            adhanAudioManager.cleanupTempFiles()
            adhanAudioManager.invalidateStaleDownloads()

            val defaultSound = AdhanSound.MISHARY
            val beepReady = adhanAudioManager.isDownloaded(AdhanSound.SIMPLE_BEEP, false)

            if (!adhanAudioManager.isFullyDownloaded(defaultSound) || !beepReady) {
                AdhanDownloadService.downloadDefault(context)
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
        }
    }
}
