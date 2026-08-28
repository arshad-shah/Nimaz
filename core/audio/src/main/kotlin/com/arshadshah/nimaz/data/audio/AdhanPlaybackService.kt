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
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.common.NimazChannels
import com.arshadshah.nimaz.core.monitoring.CrashReporter
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
            context.startForegroundService(intent)
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
                notificationTitle =
                    intent.getStringExtra(EXTRA_NOTIFICATION_TITLE) ?: "$prayerName Adhan"
                notificationMessage =
                    intent.getStringExtra(EXTRA_NOTIFICATION_MESSAGE) ?: "Tap to stop"
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

    private var fallbackFile: File? = null
    private var fallbackPrayerName: String? = null

    private fun isValidAudioFile(file: File): Boolean {
        return file.exists() && file.length() > 1000 // Must be > 1KB to be a real audio file
    }

    private fun startPlayback(adhanSound: AdhanSound, isFajr: Boolean, prayerName: String) {
        // Stop any existing playback
        stopPlayback()

        val adhanDir = File(filesDir, "adhan")
        val primaryFile = File(adhanDir, adhanSound.getFileName(isFajr))

        // Fallback to beep sound — never fall back to the wrong adhan variant
        // (e.g. playing fajr adhan at Dhuhr is incorrect)
        val beepFile = File(adhanDir, AdhanSound.SIMPLE_BEEP.getFileName(false))
        fallbackFile = if (isValidAudioFile(beepFile)) beepFile else null
        fallbackPrayerName = prayerName

        if (isValidAudioFile(primaryFile)) {
            android.util.Log.d(
                "AdhanPlayback",
                "Playing primary file: ${primaryFile.name} (isFajr=$isFajr)"
            )
            playFile(primaryFile, prayerName)
            return
        }

        // Primary file missing or corrupt — fall back to beep, NOT the other variant
        android.util.Log.w(
            "AdhanPlayback",
            "Primary file invalid: ${primaryFile.name} (exists=${primaryFile.exists()}, size=${if (primaryFile.exists()) primaryFile.length() else 0})"
        )

        if (isValidAudioFile(beepFile)) {
            android.util.Log.d("AdhanPlayback", "Falling back to beep sound")
            fallbackFile = null // Already using fallback
            playFile(beepFile, prayerName)
            return
        }

        android.util.Log.w(
            "AdhanPlayback",
            "No valid adhan or beep file found for ${adhanSound.name}"
        )
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
                    // Try fallback file if primary failed (e.g. corrupted Fajr variant)
                    val fb = fallbackFile
                    val fbName = fallbackPrayerName
                    if (fb != null && fbName != null) {
                        android.util.Log.d("AdhanPlayback", "Trying fallback: ${fb.name}")
                        fallbackFile = null // Prevent infinite retry
                        stopPlayback()
                        playFile(fb, fbName)
                    } else {
                        stopPlayback()
                        stopSelf()
                    }
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
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(notifId, createPlaybackNotification(prayerName))
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
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
            CrashReporter.recordException(e)
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
            CrashReporter.recordException(e)
            e.printStackTrace()
        }
    }

    private fun requestAudioFocus() {
        audioFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .build()
        audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NimazChannels.ADHAN_PLAYBACK,
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

        // Use the adhan channel for sound-related notifications. It is created by
        // `PrayerNotificationScheduler`, not here — this service only posts on it — but the id
        // itself comes from `NimazChannels` so the reference does not cross a module boundary.
        val channelId = NimazChannels.ADHAN

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
