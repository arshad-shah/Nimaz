package com.arshadshah.nimaz.data.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Fetches one ayah recitation to a file.
 *
 * A seam, and a deliberately small one: everything interesting about downloading a playlist —
 * de-duplication, concurrency, retry policy, progress reporting — stays in [QuranAudioManager].
 * This is only the byte transfer, which is the single thing that cannot run in a unit test.
 *
 * It exists because of a real defect. Download jobs used to be launched on the manager's own
 * scope rather than as children of the download job, so cancelling a download cancelled the
 * waiting and left the transfers running, writing progress for a surah the user had already
 * navigated away from. Nothing could catch that: `QuranAudioManager` had no tests, and it could
 * not have any while the download path reached straight for `URL.openConnection()`.
 */
interface AyahAudioDownloader {
    /**
     * Fetch [url] into [destination].
     *
     * Must be cancellable — the caller relies on cancellation propagating, and a transfer that
     * ignores it reintroduces exactly the bug this seam exists to test for.
     */
    suspend fun download(url: String, destination: File)
}

@Singleton
class HttpAyahAudioDownloader @Inject constructor() : AyahAudioDownloader {

    override suspend fun download(url: String, destination: File) {
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection()
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS

            connection.getInputStream().use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    while (true) {
                        // Checked every chunk, not once up front: a 128 kbps ayah over a slow
                        // connection is seconds of transfer, and a cancel arriving mid-file has
                        // to stop it rather than be noticed after the last byte.
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                    }
                }
            }
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val BUFFER_BYTES = 8 * 1024
    }
}
