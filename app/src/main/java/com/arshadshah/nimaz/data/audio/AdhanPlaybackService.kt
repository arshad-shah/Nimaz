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
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.arshadshah.nimaz.R
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

/**
 * Foreground service for playing adhan audio.
 * This service ensures adhan plays reliably even when the app is closed.
 * It provides a notification with a stop button for user control.
 */
@AndroidEntryPoint
class AdhanPlaybackService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    @Inject
    lateinit var adhanAudioManager: AdhanAudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "adhan_playback_channel"
        const val NOTIFICATION_ID = 6666

        const val ACTION_PLAY = "com.arshadshah.nimaz.ACTION_PLAY_ADHAN"
        const val ACTION_STOP = "com.arshadshah.nimaz.ACTION_STOP_ADHAN"

        const val EXTRA_ADHAN_SOUND = "adhan_sound"
        const val EXTRA_IS_FAJR = "is_fajr"
        const val EXTRA_PRAYER_NAME = "prayer_name"

        /**
         * Start playing adhan for a prayer notification.
         */
        fun playAdhan(
            context: Context,
            adhanSound: AdhanSound,
            isFajr: Boolean,
            prayerName: String
        ) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_ADHAN_SOUND, adhanSound.name)
                putExtra(EXTRA_IS_FAJR, isFajr)
                putExtra(EXTRA_PRAYER_NAME, prayerName)
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

        // Get the file path
        val fileName = adhanSound.getFileName(isFajr)
        val adhanDir = File(filesDir, "adhan")
        val audioFile = File(adhanDir, fileName)

        if (!audioFile.exists()) {
            // Try fallback to regular adhan if Fajr not available
            if (isFajr) {
                val regularFile = File(adhanDir, adhanSound.fileName)
                if (!regularFile.exists()) {
                    stopSelf()
                    return
                }
                playFile(regularFile, prayerName)
            } else {
                stopSelf()
                return
            }
        } else {
            playFile(audioFile, prayerName)
        }
    }

    private fun playFile(audioFile: File, prayerName: String) {
        try {
            // Acquire wake lock
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max

            // Request audio focus
            requestAudioFocus()

            // Start foreground with notification
            // On Android 14+, must specify the foreground service type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    createPlaybackNotification(prayerName),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, createPlaybackNotification(prayerName))
            }

            // Create and configure MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setOnCompletionListener(this@AdhanPlaybackService)
                setOnErrorListener(this@AdhanPlaybackService)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
            stopSelf()
        }
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nimaz)
            .setContentTitle("$prayerName Adhan")
            .setContentText("Tap to stop")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        // Audio finished playing
        stopPlayback()
        stopSelf()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        // Error occurred
        stopPlayback()
        stopSelf()
        return true
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
    }
}
