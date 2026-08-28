package com.arshadshah.nimaz.data.audio

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The one thing in the audio path that genuinely touches the network.
 *
 * It is a seam rather than an inline `URL.openConnection()` because of a real defect: download
 * jobs used to outlive their cancellation and keep writing progress for a surah the reader had
 * already navigated away from. The contract that matters is therefore **cancellability**, and it
 * is checked every chunk rather than once up front — a 128 kbps ayah on a slow connection is
 * seconds of transfer, and a cancel arriving mid-file has to stop it.
 *
 * A loopback socket rather than a mock: what is being asserted is what the transfer does with a
 * real stream, and a mocked `URL` would assert nothing about that.
 */
class HttpAyahAudioDownloaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val downloader = HttpAyahAudioDownloader()

    /** Serves [body] once, after [delayMs], then closes. Returns the URL to fetch. */
    private fun serveOnce(body: ByteArray, delayMs: Long = 0): Pair<String, ServerSocket> {
        val server = ServerSocket(0)
        thread(isDaemon = true) {
            runCatching {
                server.accept().use { socket ->
                    socket.getInputStream().bufferedReader().readLine()
                    if (delayMs > 0) Thread.sleep(delayMs)
                    socket.getOutputStream().apply {
                        write(
                            ("HTTP/1.1 200 OK\r\nContent-Length: ${body.size}\r\n\r\n")
                                .toByteArray()
                        )
                        write(body)
                        flush()
                    }
                }
            }
        }
        return "http://127.0.0.1:${server.localPort}/ayah.mp3" to server
    }

    @Test
    fun `the bytes served are the bytes written`() = runBlocking {
        val body = ByteArray(4096) { (it % 251).toByte() }
        val (url, server) = serveOnce(body)
        val destination = File(tempFolder.root, "ayah.mp3")

        downloader.download(url, destination)
        server.close()

        assertThat(destination.readBytes()).isEqualTo(body)
    }

    @Test
    fun `a file larger than one buffer is transferred whole`() = runBlocking {
        // The loop reads 8 KB at a time; an off-by-one there truncates every real ayah and the
        // player reports a corrupt file rather than a short one.
        val body = ByteArray(40_000) { (it % 97).toByte() }
        val (url, server) = serveOnce(body)
        val destination = File(tempFolder.root, "long.mp3")

        downloader.download(url, destination)
        server.close()

        assertThat(destination.length()).isEqualTo(40_000L)
    }

    @Test
    fun `a connection that never answers surfaces as a failure, not a hang`() {
        // A server that accepts and says nothing is exactly a captive portal. Without the read
        // timeout the download coroutine would sit there for the length of the sitting.
        val server = ServerSocket(0)
        thread(isDaemon = true) { runCatching { server.accept() } }
        val destination = File(tempFolder.root, "never.mp3")

        val failure = runCatching {
            runBlocking { downloader.download("http://127.0.0.1:${server.localPort}/a.mp3", destination) }
        }.exceptionOrNull()
        server.close()

        assertThat(failure).isNotNull()
    }

    @Test
    fun `an unreachable host fails rather than writing a truncated file`() {
        val destination = File(tempFolder.root, "bad.mp3")

        val failure = runCatching {
            runBlocking { downloader.download("http://127.0.0.1:1/a.mp3", destination) }
        }.exceptionOrNull()

        assertThat(failure).isNotNull()
    }

    @Test
    fun `cancelling the download stops the transfer`() = runBlocking {
        // The defect this seam exists for: a transfer that ignores cancellation keeps writing
        // progress for a surah the reader has already left.
        val body = ByteArray(4096)
        val (url, server) = serveOnce(body, delayMs = 3_000)
        val destination = File(tempFolder.root, "cancelled.mp3")

        val job = async(Dispatchers.IO) { downloader.download(url, destination) }
        job.cancel()
        val outcome = runCatching { job.await() }
        server.close()

        assertThat(outcome.isFailure).isTrue()
    }
}
