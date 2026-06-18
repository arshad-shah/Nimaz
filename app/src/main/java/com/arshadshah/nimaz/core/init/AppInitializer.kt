package com.arshadshah.nimaz.core.init

import android.content.Context
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.monitoring.PerfMonitor
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
import kotlin.time.Duration.Companion.milliseconds

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
            val startMs = System.currentTimeMillis()
            var timedOut = false
            val perfTrace = PerfMonitor.newTrace(PerfMonitor.Traces.APP_INITIALIZE)
            try {
                withTimeout(5_000L.milliseconds) {
                    val localeTask = async { applySavedLocale() }
                    val notificationTask = async { scheduleInitialNotifications() }
                    val adhanTask = async { downloadDefaultAdhanIfNeeded() }

                    localeTask.await()
                    notificationTask.await()
                    adhanTask.await()
                }
            } catch (e: Exception) {
                // Timeout or other failure — report it but proceed to UI anyway
                timedOut = e is kotlinx.coroutines.TimeoutCancellationException
                CrashReporter.recordException(e)
                AppAnalytics.logError(
                    domain = "app_init",
                    type = e.javaClass.simpleName,
                    message = e.message,
                )
            } finally {
                _isReady.value = true
                PerfMonitor.stop(
                    perfTrace,
                    attributes = mapOf("timed_out" to timedOut.toString()),
                )
                AppAnalytics.logAppInit(
                    durationMs = System.currentTimeMillis() - startMs,
                    timedOut = timedOut,
                )
                // Capture the current notification-delivery prerequisites so the
                // population-level "do notifications work?" question is answerable.
                AppAnalytics.logDiagnostics(context)
            }
        }
    }

    private suspend fun applySavedLocale() {
        try {
            val langCode = preferencesDataStore.appLanguage.first()
            AppAnalytics.setUserProperty(AppAnalytics.UserProperty.APP_LANGUAGE, langCode.ifEmpty { "en" })
            if (langCode.isNotEmpty() && langCode != "en") {
                LocaleHelper.setLocale(context, langCode)
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            AppAnalytics.logError("app_init", e.javaClass.simpleName, e.message)
        }
    }

    private suspend fun scheduleInitialNotifications() {
        try {
            val prefs = preferencesDataStore.userPreferences.first()
            AppAnalytics.setUserProperty(
                AppAnalytics.UserProperty.NOTIFICATIONS_ENABLED,
                prefs.prayerNotificationsEnabled.toString(),
            )
            AppAnalytics.setUserProperty(
                AppAnalytics.UserProperty.LOCATION_SET,
                (prefs.latitude != 0.0 || prefs.longitude != 0.0).toString(),
            )
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
            AppAnalytics.logError("notification_scheduling", e.javaClass.simpleName, e.message)
        }
    }

    private fun downloadDefaultAdhanIfNeeded() {
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
            AppAnalytics.logError("adhan_download", e.javaClass.simpleName, e.message)
        }
    }
}
