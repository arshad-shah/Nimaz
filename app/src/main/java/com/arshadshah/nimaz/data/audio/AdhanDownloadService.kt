package com.arshadshah.nimaz.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for downloading adhan audio files.
 * Used for initial download on first launch and when user selects a new muezzin.
 */
@AndroidEntryPoint
class AdhanDownloadService : Service() {

    @Inject
    lateinit var adhanAudioManager: AdhanAudioManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "adhan_download_channel"
        const val NOTIFICATION_ID = 7777
        const val EXTRA_ADHAN_SOUND = "adhan_sound"
        const val ACTION_DOWNLOAD_DEFAULT = "com.arshadshah.nimaz.DOWNLOAD_DEFAULT_ADHAN"
        const val ACTION_DOWNLOAD_SELECTED = "com.arshadshah.nimaz.DOWNLOAD_SELECTED_ADHAN"

        fun downloadDefault(context: Context) {
            val intent = Intent(context, AdhanDownloadService::class.java).apply {
                action = ACTION_DOWNLOAD_DEFAULT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun downloadSelected(context: Context, adhanSound: AdhanSound) {
            val intent = Intent(context, AdhanDownloadService::class.java).apply {
                action = ACTION_DOWNLOAD_SELECTED
                putExtra(EXTRA_ADHAN_SOUND, adhanSound.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Preparing download..."))

        when (intent?.action) {
            ACTION_DOWNLOAD_DEFAULT -> downloadDefaultAdhan()
            ACTION_DOWNLOAD_SELECTED -> {
                val soundName = intent.getStringExtra(EXTRA_ADHAN_SOUND)
                if (soundName != null) {
                    val sound = AdhanSound.fromName(soundName)
                    downloadSelectedAdhan(sound)
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
                val defaultSound = AdhanSound.MISHARY

                // Check if already downloaded
                if (adhanAudioManager.isFullyDownloaded(defaultSound)) {
                    stopSelf()
                    return@launch
                }

                updateNotification("Downloading ${defaultSound.displayName}...")

                // Download regular adhan
                if (!adhanAudioManager.isDownloaded(defaultSound, false)) {
                    updateNotification("Downloading ${defaultSound.displayName} (Regular)...")
                    adhanAudioManager.downloadAdhan(defaultSound, false)
                }

                // Download Fajr adhan
                if (!adhanAudioManager.isDownloaded(defaultSound, true)) {
                    updateNotification("Downloading ${defaultSound.displayName} (Fajr)...")
                    adhanAudioManager.downloadAdhan(defaultSound, true)
                }

                // Also generate beep sound
                if (!adhanAudioManager.isDownloaded(AdhanSound.SIMPLE_BEEP, false)) {
                    updateNotification("Generating beep sound...")
                    adhanAudioManager.downloadAdhan(AdhanSound.SIMPLE_BEEP, false)
                }

                updateNotification("Download complete!")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopSelf()
            }
        }
    }

    private fun downloadSelectedAdhan(sound: AdhanSound) {
        serviceScope.launch {
            try {
                // Check if already downloaded
                if (adhanAudioManager.isFullyDownloaded(sound)) {
                    stopSelf()
                    return@launch
                }

                updateNotification("Downloading ${sound.displayName}...")

                // Download regular adhan
                if (!adhanAudioManager.isDownloaded(sound, false)) {
                    updateNotification("Downloading ${sound.displayName} (Regular)...")
                    adhanAudioManager.downloadAdhan(sound, false)
                }

                // Download Fajr adhan
                if (!adhanAudioManager.isDownloaded(sound, true)) {
                    updateNotification("Downloading ${sound.displayName} (Fajr)...")
                    adhanAudioManager.downloadAdhan(sound, true)
                }

                updateNotification("Download complete!")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Adhan Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress when downloading adhan audio files"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading Adhan")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
