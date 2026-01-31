package com.arshadshah.nimaz.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.arshadshah.nimaz.R
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

/**
 * Foreground service for playing adhan audio.
 * Uses ExoPlayer with Media3 MediaSession for lock screen controls and media-style notification.
 */
@AndroidEntryPoint
class AdhanPlaybackService : Service() {

    @Inject
    lateinit var adhanAudioManager: AdhanAudioManager

    private var exoPlayer: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Prayer notification content to merge into the service notification
    private var notificationTitle: String = ""
    private var notificationMessage: String = ""
    private var notificationColor: Int = 0
    private var currentPrayerName: String = "Prayer"

    companion object {
        const val CHANNEL_ID = "adhan_playback_channel"
        const val NOTIFICATION_ID = 6666

        const val ACTION_PLAY = "com.arshadshah.nimaz.ACTION_PLAY_ADHAN"
        const val ACTION_STOP = "com.arshadshah.nimaz.ACTION_STOP_ADHAN"

        const val EXTRA_ADHAN_SOUND = "adhan_sound"
        const val EXTRA_IS_FAJR = "is_fajr"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_PRAYER_TYPE = "prayer_type"
        const val EXTRA_PRAYER_TIME = "prayer_time"
        const val EXTRA_NOTIFICATION_TITLE = "notification_title"
        const val EXTRA_NOTIFICATION_MESSAGE = "notification_message"
        const val EXTRA_NOTIFICATION_COLOR = "notification_color"

        /**
         * Start playing adhan for a prayer notification.
         * The service notification will serve as both the prayer notification and adhan playback notification.
         */
        fun playAdhan(
            context: Context,
            adhanSound: AdhanSound,
            isFajr: Boolean,
            prayerName: String,
            prayerType: String = "",
            prayerTime: String = "",
            notificationTitle: String = "",
            notificationMessage: String = "",
            notificationColor: Int = 0
        ) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_ADHAN_SOUND, adhanSound.name)
                putExtra(EXTRA_IS_FAJR, isFajr)
                putExtra(EXTRA_PRAYER_NAME, prayerName)
                putExtra(EXTRA_PRAYER_TYPE, prayerType)
                putExtra(EXTRA_PRAYER_TIME, prayerTime)
                putExtra(EXTRA_NOTIFICATION_TITLE, notificationTitle)
                putExtra(EXTRA_NOTIFICATION_MESSAGE, notificationMessage)
                putExtra(EXTRA_NOTIFICATION_COLOR, notificationColor)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop any currently playing adhan.
         */
        fun stopAdhan(context: Context) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Acquire wake lock to keep CPU running during playback
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "nimaz:adhan_playback"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val soundName = intent.getStringExtra(EXTRA_ADHAN_SOUND) ?: AdhanSound.MISHARY.name
                val isFajr = intent.getBooleanExtra(EXTRA_IS_FAJR, false)
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"

                // Store prayer notification content for merged notification
                currentPrayerName = prayerName
                notificationTitle = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE) ?: "$prayerName Adhan"
                notificationMessage = intent.getStringExtra(EXTRA_NOTIFICATION_MESSAGE) ?: "Tap to stop"
                notificationColor = intent.getIntExtra(EXTRA_NOTIFICATION_COLOR, 0)

                val adhanSound = AdhanSound.fromName(soundName)
                startPlayback(adhanSound, isFajr, prayerName)
            }
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
            }
            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startPlayback(adhanSound: AdhanSound, isFajr: Boolean, prayerName: String) {
        // Stop any existing playback
        stopPlayback()

        val adhanDir = File(filesDir, "adhan")
        val primaryFile = File(adhanDir, adhanSound.getFileName(isFajr))

        if (primaryFile.exists()) {
            android.util.Log.d("AdhanPlayback", "Playing primary file: ${primaryFile.name} (isFajr=$isFajr)")
            playFile(primaryFile, prayerName)
            return
        }

        // Bidirectional fallback: try the other variant
        val fallbackIsFajr = !isFajr
        val fallbackFile = File(adhanDir, adhanSound.getFileName(fallbackIsFajr))
        if (fallbackFile.exists()) {
            android.util.Log.d("AdhanPlayback", "Falling back to: ${fallbackFile.name} (isFajr=$fallbackIsFajr)")
            playFile(fallbackFile, prayerName)
            return
        }

        android.util.Log.w("AdhanPlayback", "No adhan file found for ${adhanSound.name}")
        stopSelf()
    }

    private fun playFile(audioFile: File, prayerName: String) {
        try {
            // Acquire wake lock
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max

            // Request audio focus
            requestAudioFocus()

            // Create ExoPlayer with alarm audio attributes
            val player = ExoPlayer.Builder(this)
                .setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(androidx.media3.common.C.USAGE_ALARM)
                        .build(),
                    false // don't handle audio focus via ExoPlayer — we manage it ourselves
                )
                .setWakeMode(androidx.media3.common.C.WAKE_MODE_LOCAL)
                .build()

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        stopPlayback()
                        stopSelf()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("AdhanPlayback", "Playback error: ${error.message}")
                    stopPlayback()
                    stopSelf()
                }
            })

            exoPlayer = player

            // Set media item
            val mediaItem = MediaItem.Builder()
                .setUri(audioFile.toURI().toString())
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            // Use prayer name hashCode as notification ID to merge with prayer notification
            val notifId = prayerName.hashCode()

            // Start foreground with notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    notifId,
                    createPlaybackNotification(prayerName),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                )
            } else {
                startForeground(notifId, createPlaybackNotification(prayerName))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
            stopSelf()
        }
    }

    private fun stopPlayback() {
        try {
            exoPlayer?.stop()
            exoPlayer?.release()
            exoPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Release audio focus
        abandonAudioFocus()

        // Release wake lock
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .build()
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Adhan Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when adhan is playing"
                setShowBadge(false)
                setSound(null, null) // No sound for this channel
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPlaybackNotification(prayerName: String): Notification {
        // Create stop action intent
        val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create dismiss intent — stops adhan when notification is swiped away
        val dismissIntent = Intent(this, AdhanPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create open app intent
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
        val openAppPendingIntent = openAppIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Use prayer notification content if available, otherwise fallback
        val title = notificationTitle.ifEmpty { "$prayerName Adhan" }
        val message = notificationMessage.ifEmpty { "Tap to stop" }

        // Style the "Stop Adhan" action text in red
        val stopLabel = SpannableString("Stop Adhan").apply {
            setSpan(ForegroundColorSpan(0xFFE53935.toInt()), 0, length, 0)
        }

        // Use the adhan channel for sound-related notifications
        val channelId = com.arshadshah.nimaz.core.util.PrayerNotificationScheduler.CHANNEL_ID_ADHAN

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_nimaz)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setColorized(notificationColor != 0)
            .apply {
                if (notificationColor != 0) setColor(notificationColor)
            }
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                stopLabel,
                stopPendingIntent
            )

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
    }
}
