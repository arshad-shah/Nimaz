package com.arshadshah.nimaz.data.audio

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.R as AppR
import com.arshadshah.nimaz.core.common.NimazChannels
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for downloading adhan audio files.
 * Shows real-time progress in the notification with step indicators.
 */
@AndroidEntryPoint
class AdhanDownloadService : Service() {

    @Inject
    lateinit var adhanAudioManager: AdhanAudioManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var notificationManager: NotificationManager? = null

    companion object {
        private const val TAG = "AdhanDownloadService"
        const val NOTIFICATION_ID = 7777
        const val EXTRA_ADHAN_SOUND = "adhan_sound"
        const val ACTION_DOWNLOAD_DEFAULT = "com.arshadshah.nimaz.DOWNLOAD_DEFAULT_ADHAN"
        const val ACTION_DOWNLOAD_SELECTED = "com.arshadshah.nimaz.DOWNLOAD_SELECTED_ADHAN"

        fun downloadDefault(context: Context) {
            // Automatic, non-interactive maintenance download triggered during
            // app initialization. No user is watching for progress, and on a
            // cold start the main thread can be too busy to deliver
            // onStartCommand() — and therefore call startForeground() — within
            // the system's foreground-service start deadline, crashing with
            // ForegroundServiceDidNotStartInTimeException. That exception is
            // delivered asynchronously and cannot be caught at the call site, so
            // we skip the foreground service entirely and use WorkManager, the
            // supported mechanism for background work.
            AdhanDownloadWorker.enqueue(context, null)
        }

        fun downloadSelected(context: Context, adhanSound: AdhanSound) {
            val intent = Intent(context, AdhanDownloadService::class.java).apply {
                action = ACTION_DOWNLOAD_SELECTED
                putExtra(EXTRA_ADHAN_SOUND, adhanSound.name)
            }
            startServiceWithFallback(
                canStartForeground = isAppInForeground(),
                start = { startForegroundCompat(context, intent) },
                fallback = { AdhanDownloadWorker.enqueue(context, adhanSound) }
            )
        }

        private fun startForegroundCompat(context: Context, intent: Intent) {
            context.startForegroundService(intent)
        }

        /**
         * Whether the app process is currently in the foreground and may
         * therefore reliably start a foreground service.
         *
         * Foreground starts from the background are the source of two distinct
         * Android 12+ crashes: [android.app.ForegroundServiceStartNotAllowedException]
         * (thrown synchronously at the call site) and
         * [android.app.ForegroundServiceDidNotStartInTimeException] (thrown
         * asynchronously on the main thread when startForeground() is not reached
         * within the system deadline, and therefore impossible to catch). Gating
         * the start on foreground importance avoids both, sending background
         * triggers (e.g. boot) to the WorkManager fallback instead.
         */
        private fun isAppInForeground(): Boolean {
            val state = ActivityManager.RunningAppProcessInfo()
            ActivityManager.getMyMemoryState(state)
            return state.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }

        /**
         * Launches the foreground download service via [start] when
         * [canStartForeground] is true, degrading to [fallback] (a background
         * WorkManager job) when the app is not in the foreground or when the
         * foreground start is rejected at the call site.
         *
         * Visible for testing.
         */
        internal fun startServiceWithFallback(
            canStartForeground: Boolean,
            start: () -> Unit,
            fallback: () -> Unit
        ) {
            if (!canStartForeground) {
                Log.d(TAG, "App not in foreground; using background download")
                fallback()
                return
            }
            try {
                start()
            } catch (e: Exception) {
                Log.w(TAG, "Foreground service start not allowed; using background download", e)
                fallback()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotification = buildNotification(
            title = getString(R.string.adhan_download_preparing),
            subtitle = null,
            progress = -1 // indeterminate
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        when (intent?.action) {
            ACTION_DOWNLOAD_DEFAULT -> downloadDefaultAdhan()
            ACTION_DOWNLOAD_SELECTED -> {
                val soundName = intent.getStringExtra(EXTRA_ADHAN_SOUND)
                if (soundName != null) {
                    downloadSelectedAdhan(AdhanSound.fromName(soundName))
                } else {
                    stopSelf()
                }
            }

            else -> stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun downloadDefaultAdhan() {
        serviceScope.launch {
            try {
                adhanAudioManager.cleanupTempFiles()
                val sound = AdhanSound.MISHARY
                val results = downloadBothVariants(sound)
                ensureBeepExists()
                showCompletionNotification(sound.displayName, results)
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                Log.e(TAG, "Download service error", e)
                showErrorNotification()
            } finally {
                stopSelf()
            }
        }
    }

    private fun downloadSelectedAdhan(sound: AdhanSound) {
        serviceScope.launch {
            try {
                adhanAudioManager.cleanupTempFiles()
                val results = downloadBothVariants(sound)
                ensureBeepExists()
                showCompletionNotification(sound.displayName, results)
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                Log.e(TAG, "Download service error", e)
                showErrorNotification()
            } finally {
                stopSelf()
            }
        }
    }

    private data class DownloadResults(
        val regularSuccess: Boolean,
        val fajrSuccess: Boolean
    )

    private suspend fun downloadBothVariants(sound: AdhanSound): DownloadResults {
        val totalSteps = 2
        var regularSuccess = adhanAudioManager.isDownloaded(sound, false)
        var fajrSuccess = adhanAudioManager.isDownloaded(sound, true)

        // Step 1: Regular variant
        if (!regularSuccess) {
            updateProgressNotification(
                muezzinName = sound.displayName,
                step = 1,
                totalSteps = totalSteps,
                variantLabel = getString(R.string.adhan_variant_regular),
                progress = 0
            )

            regularSuccess = adhanAudioManager.downloadAdhan(sound, false) { progress ->
                updateProgressNotification(
                    muezzinName = sound.displayName,
                    step = 1,
                    totalSteps = totalSteps,
                    variantLabel = getString(R.string.adhan_variant_regular),
                    progress = progress
                )
            }

            if (regularSuccess && !adhanAudioManager.isDownloaded(sound, false)) {
                Log.e(TAG, "Regular variant reported success but file is invalid")
                regularSuccess = false
            }
        }

        // Step 2: Fajr variant
        if (!fajrSuccess) {
            updateProgressNotification(
                muezzinName = sound.displayName,
                step = 2,
                totalSteps = totalSteps,
                variantLabel = getString(R.string.adhan_variant_fajr),
                progress = 0
            )

            fajrSuccess = adhanAudioManager.downloadAdhan(sound, true) { progress ->
                updateProgressNotification(
                    muezzinName = sound.displayName,
                    step = 2,
                    totalSteps = totalSteps,
                    variantLabel = getString(R.string.adhan_variant_fajr),
                    progress = progress
                )
            }

            if (fajrSuccess && !adhanAudioManager.isDownloaded(sound, true)) {
                Log.e(TAG, "Fajr variant reported success but file is invalid")
                fajrSuccess = false
            }
        }

        return DownloadResults(regularSuccess, fajrSuccess)
    }

    private suspend fun ensureBeepExists() {
        if (!adhanAudioManager.isDownloaded(AdhanSound.SIMPLE_BEEP, false)) {
            val success = adhanAudioManager.downloadAdhan(AdhanSound.SIMPLE_BEEP, false)
            if (!success) Log.e(TAG, "Failed to generate beep sound")
        }
    }

    // --- Notification builders ---

    private fun updateProgressNotification(
        muezzinName: String,
        step: Int,
        totalSteps: Int,
        variantLabel: String,
        progress: Int
    ) {
        val title = getString(R.string.adhan_download_title, muezzinName)
        val subtitle = getString(R.string.adhan_download_step, step, totalSteps, variantLabel)
        val notification = buildNotification(title, subtitle, progress)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(muezzinName: String, results: DownloadResults) {
        val (title, subtitle) = when {
            results.regularSuccess && results.fajrSuccess -> Pair(
                getString(R.string.adhan_download_complete),
                getString(R.string.adhan_download_complete_subtitle, muezzinName)
            )

            results.regularSuccess || results.fajrSuccess -> Pair(
                getString(R.string.adhan_download_partial),
                getString(R.string.adhan_download_partial_subtitle, muezzinName)
            )

            else -> Pair(
                getString(R.string.adhan_download_failed),
                getString(R.string.adhan_download_failed_subtitle)
            )
        }

        val notification = buildNotification(title, subtitle, progress = 100, ongoing = false)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification() {
        val notification = buildNotification(
            title = getString(R.string.adhan_download_failed),
            subtitle = getString(R.string.adhan_download_failed_subtitle),
            progress = 100,
            ongoing = false
        )
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Builds a download notification.
     * @param progress 0-100 for determinate, -1 for indeterminate, 100 for complete (no bar).
     * @param ongoing true while downloading, false for completion/error.
     */
    private fun buildNotification(
        title: String,
        subtitle: String?,
        progress: Int,
        ongoing: Boolean = true
    ): Notification {
        val builder = NotificationCompat.Builder(this, NimazChannels.ADHAN_DOWNLOAD)
            .setSmallIcon(AppR.drawable.ic_stat_nimaz)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (subtitle != null) {
            builder.setContentText(subtitle)
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
        }

        when {
            progress == -1 -> {
                // Indeterminate
                builder.setProgress(100, 0, true)
            }

            progress < 100 -> {
                // Determinate progress bar
                builder.setProgress(100, progress, false)
            }

            else -> {
                // Complete — no progress bar
                builder.setProgress(0, 0, false)
                builder.setAutoCancel(true)
            }
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NimazChannels.ADHAN_DOWNLOAD,
            getString(R.string.adhan_download_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.adhan_download_channel_description)
            setShowBadge(false)
            setSound(null, null)
        }
        notificationManager?.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
