package com.arshadshah.nimaz.repositories

import android.content.Context
import android.util.Log
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.arshadshah.nimaz.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import es.dmoral.toasty.Toasty
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SpacesFileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transferUtility: TransferUtility
) {
    companion object {
        private const val TAG = "SpacesFileRepository"
        private const val FILE_TYPE = "mp3"
        private const val BASE_PATH = "mishary"
    }

    /**
     * Downloads aya file from Digital Ocean Space
     */
    fun downloadAyaFile(
        suraNumber: Int,
        ayaNumber: Int,
        callback: (File?, Exception?, progress: Int, completed: Boolean) -> Unit,
    ) {
        try {
            val formattedSura = formatNumber(suraNumber)
            val formattedAya = formatNumber(ayaNumber)

            // Create directories if they don't exist
            val directory = File("${context.filesDir}/quran/$formattedSura").apply {
                mkdirs()
            }

            val file = File(directory, "$formattedAya.$FILE_TYPE")
            val key = buildS3Key(formattedSura, formattedAya)

            val transfer = transferUtility.download(BuildConfig.SPACE_NAME, key, file)

            transfer.setTransferListener(createTransferListener(callback, file))

        } catch (e: Exception) {
            Log.e(TAG, "Error initiating download: ${e.message}", e)
            callback(null, e, 0, false)
        }
    }

    private fun formatNumber(number: Int): String = when {
        number < 10 -> "00$number"
        number < 100 -> "0$number"
        else -> "$number"
    }

    private fun buildS3Key(sura: String, aya: String): String =
        "$BASE_PATH/quran-surah-$sura-verse-$aya.$FILE_TYPE"

    private fun createTransferListener(
        callback: (File?, Exception?, progress: Int, completed: Boolean) -> Unit,
        file: File
    ) = object : TransferListener {
        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            if (bytesTotal <= 0) return
            val progress = ((bytesCurrent.toDouble() / bytesTotal) * 100).toInt()
            Log.d(TAG, "Download progress: $progress%")
            callback(null, null, progress, false)
        }

        override fun onStateChanged(id: Int, state: TransferState?) {
            when (state) {
                TransferState.COMPLETED -> {
                    Log.d(TAG, "Download completed")
                    callback(file, null, 100, true)
                }

                TransferState.IN_PROGRESS -> {
                    Log.d(TAG, "Download in progress")
                    callback(null, null, 0, false)
                }

                TransferState.FAILED -> handleFailure("Download failed")
                TransferState.CANCELED -> handleFailure("Download canceled")
                else -> {
                    Log.d(TAG, "Transfer state: $state")
                    callback(null, null, 0, false)
                }
            }
        }

        override fun onError(id: Int, ex: Exception?) {
            handleFailure("Download error: ${ex?.message}", ex)
        }

        private fun handleFailure(message: String, exception: Exception? = null) {
            Log.e(TAG, message, exception)
            callback(null, exception ?: Exception(message), 0, false)
            showError(message)
        }

        private fun showError(message: String) {
            try {
                Toasty.error(context, message).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing toast: ${e.message}")
            }
        }
    }
}