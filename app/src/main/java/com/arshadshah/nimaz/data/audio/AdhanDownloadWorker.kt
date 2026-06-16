package com.arshadshah.nimaz.data.audio

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background fallback for downloading adhan audio when a foreground service
 * cannot be started.
 *
 * [AdhanDownloadService] runs as a foreground service so it can show download
 * progress while the app is open. But it is triggered during app
 * initialization and from prayer-notification broadcasts, both of which can run
 * while the app is in the background. On Android 12+ starting a foreground
 * service from the background throws
 * [android.app.ForegroundServiceStartNotAllowedException]. WorkManager is the
 * supported way to run such work in the background, so the service start helpers
 * fall back to enqueuing this worker when the foreground start is rejected.
 */
@HiltWorker
class AdhanDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val adhanAudioManager: AdhanAudioManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            adhanAudioManager.cleanupTempFiles()

            val sound = inputData.getString(KEY_ADHAN_SOUND)
                ?.let { AdhanSound.fromName(it) }
                ?: AdhanSound.MISHARY

            // Download both variants; failures are tolerated per-variant so a
            // single bad URL doesn't abort the whole job.
            adhanAudioManager.downloadAdhan(sound, isFajr = false)
            adhanAudioManager.downloadAdhan(sound, isFajr = true)

            // Always ensure the beep fallback exists (generated locally).
            if (!adhanAudioManager.isDownloaded(AdhanSound.SIMPLE_BEEP, false)) {
                adhanAudioManager.downloadAdhan(AdhanSound.SIMPLE_BEEP, false)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background adhan download failed", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "AdhanDownloadWorker"
        private const val MAX_RETRIES = 3
        const val KEY_ADHAN_SOUND = "adhan_sound"
        const val WORK_NAME = "adhan_download_work"

        /**
         * Enqueues a background download. A null [sound] downloads the default
         * adhan. Existing work is kept so repeated triggers don't pile up.
         */
        fun enqueue(context: Context, sound: AdhanSound?) {
            val data = Data.Builder().apply {
                if (sound != null) putString(KEY_ADHAN_SOUND, sound.name)
            }.build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<AdhanDownloadWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
