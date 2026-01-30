package com.arshadshah.nimaz.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arshadshah.nimaz.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for Quran audio playback.
 * Provides media notification with play/pause, prev/next controls.
 */
@AndroidEntryPoint
class QuranAudioService : Service() {

    @Inject
    lateinit var audioManager: QuranAudioManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateCollectorJob: Job? = null

    companion object {
        const val CHANNEL_ID = "quran_audio_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.arshadshah.nimaz.ACTION_QURAN_PLAY"
        const val ACTION_PAUSE = "com.arshadshah.nimaz.ACTION_QURAN_PAUSE"
        const val ACTION_NEXT = "com.arshadshah.nimaz.ACTION_QURAN_NEXT"
        const val ACTION_PREVIOUS = "com.arshadshah.nimaz.ACTION_QURAN_PREVIOUS"
        const val ACTION_STOP = "com.arshadshah.nimaz.ACTION_QURAN_STOP"

        /**
         * Start the Quran audio foreground service.
         */
        fun start(context: Context) {
            val intent = Intent(context, QuranAudioService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the Quran audio foreground service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, QuranAudioService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startStateObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> audioManager.togglePlayPause()
            ACTION_PAUSE -> audioManager.togglePlayPause()
            ACTION_NEXT -> audioManager.skipToNext()
            ACTION_PREVIOUS -> audioManager.skipToPrevious()
            ACTION_STOP -> {
                audioManager.stop()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startStateObserver() {
        stateCollectorJob = serviceScope.launch {
            audioManager.audioState.collectLatest { state ->
                if (state.isActive) {
                    updateNotification(state)
                } else {
                    // Audio stopped, stop the service
                    stopSelf()
                }
            }
        }
    }

    private fun updateNotification(state: AudioState) {
        val notification = buildNotification(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(state: AudioState): Notification {
        // Create pending intents for actions
        val playPauseIntent = createActionIntent(
            if (state.isPlaying) ACTION_PAUSE else ACTION_PLAY
        )
        val previousIntent = createActionIntent(ACTION_PREVIOUS)
        val nextIntent = createActionIntent(ACTION_NEXT)
        val stopIntent = createActionIntent(ACTION_STOP)

        // Open app intent
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
        val openAppPendingIntent = openAppIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Build subtitle with progress info
        val subtitle = if (state.isPreparing && state.totalToDownload > 0) {
            "Downloading ${state.downloadedCount} of ${state.totalToDownload} ayahs"
        } else if (state.totalAyahs > 0) {
            "Ayah ${state.currentAyahIndex + 1} of ${state.totalAyahs} • ${state.reciterName}"
        } else {
            state.currentSubtitle ?: state.reciterName
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nimaz)
            .setContentTitle(state.currentTitle)
            .setContentText(subtitle)
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(stopIntent)
            .setOngoing(state.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                previousIntent
            )
            .addAction(
                if (state.isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
                if (state.isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                nextIntent
            )
            .build()
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, QuranAudioService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quran Audio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Quran audio playback controls"
                setShowBadge(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stateCollectorJob?.cancel()
    }
}
