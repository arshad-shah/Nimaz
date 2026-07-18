@file:androidx.annotation.OptIn(UnstableApi::class)

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
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for Quran audio playback.
 * Provides media-style notification with lock screen controls via Media3 MediaSession.
 */
@AndroidEntryPoint
class QuranAudioService : Service() {

    @Inject
    lateinit var audioManager: QuranAudioManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateCollectorJob: Job? = null
    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "quran_audio_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.arshadshah.nimaz.ACTION_QURAN_PLAY"
        const val ACTION_PAUSE = "com.arshadshah.nimaz.ACTION_QURAN_PAUSE"
        const val ACTION_NEXT = "com.arshadshah.nimaz.ACTION_QURAN_NEXT"
        const val ACTION_PREVIOUS = "com.arshadshah.nimaz.ACTION_QURAN_PREVIOUS"
        const val ACTION_STOP = "com.arshadshah.nimaz.ACTION_QURAN_STOP"

        // Sent on the MainActivity intent when the user taps the notification body
        // or the lock screen player. MainActivity reads the surah from the singleton
        // QuranAudioManager and navigates to the reader.
        const val ACTION_OPEN_PLAYING_SURAH = "com.arshadshah.nimaz.ACTION_OPEN_PLAYING_SURAH"

        /**
         * Start the Quran audio foreground service.
         */
        fun start(context: Context) {
            val intent = Intent(context, QuranAudioService::class.java)
            context.startForegroundService(intent)
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
        if (intent == null) {
            // Service restarted by system with null intent — nothing to do
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_PLAY -> audioManager.togglePlayPause()
            ACTION_PAUSE -> audioManager.togglePlayPause()
            ACTION_NEXT -> audioManager.skipToNext()
            ACTION_PREVIOUS -> audioManager.skipToPrevious()
            ACTION_STOP -> {
                audioManager.stop()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun getOrCreateMediaSession(): MediaSession? {
        val player = audioManager.getPlayer() ?: return null

        // If the player changed (e.g. after stop + recreate), rebuild the session
        val existing = mediaSession
        if (existing != null && existing.player === player) {
            return existing
        }

        existing?.release()
        return MediaSession.Builder(this, player)
            .setSessionActivity(buildOpenPlayingSurahPendingIntent())
            .build()
            .also { mediaSession = it }
    }

    /**
     * PendingIntent that opens MainActivity with ACTION_OPEN_PLAYING_SURAH so the
     * NavGraph can route to the surah currently being played. The surah is read at
     * the moment of the click from QuranAudioManager.audioState (the singleton),
     * not encoded in extras — so the intent stays valid as the surah changes.
     */
    private fun buildOpenPlayingSurahPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_PLAYING_SURAH
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun releaseMediaSession() {
        mediaSession?.release()
        mediaSession = null
    }

    private fun startStateObserver() {
        stateCollectorJob = serviceScope.launch {
            audioManager.audioState.collectLatest { state ->
                if (state.isActive || state.isPreparing) {
                    updateNotification(state)
                } else {
                    // Audio stopped — delay briefly before stopping the service to avoid
                    // a race condition where a quick stop-then-play kills the new session.
                    delay(500)
                    val current = audioManager.audioState.value
                    if (!current.isActive && !current.isPreparing) {
                        releaseMediaSession()
                        stopSelf()
                    }
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

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun buildNotification(state: AudioState): Notification {
        // Create pending intents for actions
        val playPauseIntent = createActionIntent(
            if (state.isPlaying) ACTION_PAUSE else ACTION_PLAY
        )
        val previousIntent = createActionIntent(ACTION_PREVIOUS)
        val nextIntent = createActionIntent(ACTION_NEXT)
        val stopIntent = createActionIntent(ACTION_STOP)

        // Tapping the notification body opens MainActivity, which reads the
        // playing surah from the singleton AudioManager and navigates the user there.
        val openAppPendingIntent = buildOpenPlayingSurahPendingIntent()

        // Build subtitle with reciter info
        val subtitle = if (state.isPreparing && state.totalToDownload > 0) {
            "Downloading ${state.downloadedCount} of ${state.totalToDownload} ayahs"
        } else {
            state.reciterName
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
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

        // Apply MediaStyle if a MediaSession is available (gives lock screen controls)
        val session = getOrCreateMediaSession()
        if (session != null) {
            builder.setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2) // prev, play/pause, next
            )
        }

        return builder.build()
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stateCollectorJob?.cancel()
        releaseMediaSession()
    }
}
