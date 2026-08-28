package com.arshadshah.nimaz.data.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The adhan file store.
 *
 * Nothing here draws anything, so none of it looked worth testing — and all of it decides
 * whether the phone makes a sound at prayer time. The failures it guards against are all silent:
 *
 *  - **an HTML error page saved as `.mp3`.** A CDN that answers a download with a 200 and a
 *    error page leaves a file that exists, is named right, and plays nothing. `isDownloaded`
 *    reads the magic bytes for exactly this, and *deletes* what it rejects so the next attempt
 *    re-downloads rather than trusting the corpse;
 *  - **a URL that changed under a file that did not.** Mishary's regular URL was serving the
 *    *Fajr* recording, so every install that had already downloaded it kept playing the wrong
 *    adhan forever. `invalidateStaleDownloads` is the undo: a version stamp beside the files,
 *    and a bump deletes them. Getting it wrong either re-downloads 15 MB on every launch or
 *    never repairs the install;
 *  - **the generated chime.** `SIMPLE_BEEP` has no URL — it is synthesised into a WAV on the
 *    device. If the header it writes is wrong, `isDownloaded` rejects its own output and the
 *    beep can never finish downloading;
 *  - **deleting one variant.** Regular and Fajr are separate files, so removing one must not
 *    reset the pair's state while the other is still on disk.
 *
 * What is *not* here is the HTTP transfer itself: `downloadFile` opens
 * [java.net.URL.openConnection] against the URL baked into [AdhanSound], so exercising it would
 * need either a real request to a CDN or a JVM-wide `URLStreamHandlerFactory` — see
 * `docs/TESTING.md`.
 */
@RunWith(RobolectricTestRunner::class)
class AdhanAudioManagerTest {

    private lateinit var context: Context
    private lateinit var manager: AdhanAudioManager
    private lateinit var adhanDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        adhanDir = File(context.filesDir, "adhan")
        adhanDir.deleteRecursively()
        manager = AdhanAudioManager(context)
    }

    // ── what counts as downloaded ─────────────────────────────────────────────

    @Test
    fun `a sound with no file is not downloaded`() {
        assertThat(manager.isDownloaded(AdhanSound.MAKKAH)).isFalse()
        assertThat(manager.getAdhanUri(AdhanSound.MAKKAH)).isNull()
        assertThat(manager.getDownloadedSize(AdhanSound.MAKKAH)).isNull()
    }

    @Test
    fun `an mp3 with an ID3 tag counts as downloaded`() {
        writeAudio(AdhanSound.MAKKAH, isFajr = false, header = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 0x04))

        assertThat(manager.isDownloaded(AdhanSound.MAKKAH)).isTrue()
        assertThat(manager.getAdhanUri(AdhanSound.MAKKAH)).isNotNull()
    }

    @Test
    fun `an mp3 that starts with a bare MPEG sync word counts as downloaded`() {
        // Plenty of adhan files have no ID3 tag at all; rejecting them would re-download forever.
        writeAudio(AdhanSound.MAKKAH, isFajr = false, header = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x00))

        assertThat(manager.isDownloaded(AdhanSound.MAKKAH)).isTrue()
    }

    @Test
    fun `a wav counts as downloaded`() {
        writeAudio(AdhanSound.MAKKAH, isFajr = false, header = "RIFF".toByteArray())

        assertThat(manager.isDownloaded(AdhanSound.MAKKAH)).isTrue()
    }

    @Test
    fun `an HTML error page saved as an mp3 is rejected and deleted`() {
        val file = writeAudio(AdhanSound.MAKKAH, isFajr = false, header = "<htm".toByteArray())

        assertThat(manager.isDownloaded(AdhanSound.MAKKAH)).isFalse()
        // Deleted, not just reported: otherwise the next attempt short-circuits on a file that
        // exists and the install never repairs itself.
        assertThat(file.exists()).isFalse()
    }

    @Test
    fun `a truncated file is rejected and deleted`() {
        val file = File(adhanDir.also { it.mkdirs() }, AdhanSound.MAKKAH.fileName)
        file.writeBytes("ID3".toByteArray() + ByteArray(20))

        assertThat(manager.isDownloaded(AdhanSound.MAKKAH)).isFalse()
        assertThat(file.exists()).isFalse()
    }

    @Test
    fun `a file too short to hold a header at all is rejected`() {
        val file = File(adhanDir.also { it.mkdirs() }, AdhanSound.MAKKAH.fileName)
        file.writeBytes(byteArrayOf(1, 2))

        assertThat(manager.isDownloaded(AdhanSound.MAKKAH)).isFalse()
    }

    @Test
    fun `a sound is fully downloaded only when both variants are present`() {
        writeAudio(AdhanSound.MAKKAH, isFajr = false)

        assertThat(manager.isDownloaded(AdhanSound.MAKKAH, isFajr = false)).isTrue()
        assertThat(manager.isDownloaded(AdhanSound.MAKKAH, isFajr = true)).isFalse()
        assertThat(manager.isFullyDownloaded(AdhanSound.MAKKAH)).isFalse()

        writeAudio(AdhanSound.MAKKAH, isFajr = true)

        assertThat(manager.isFullyDownloaded(AdhanSound.MAKKAH)).isTrue()
    }

    @Test
    fun `the reported size is the pair's, not one variant's`() {
        writeAudio(AdhanSound.MAKKAH, isFajr = false, size = 20_000)
        writeAudio(AdhanSound.MAKKAH, isFajr = true, size = 30_000)

        assertThat(manager.getDownloadedSize(AdhanSound.MAKKAH, isFajr = false)).isEqualTo(20_000)
        assertThat(manager.getTotalDownloadedSize(AdhanSound.MAKKAH)).isEqualTo(50_000)
    }

    @Test
    fun `a sound with nothing on disk reports no total size rather than failing`() {
        assertThat(manager.getTotalDownloadedSize(AdhanSound.ABDUL_BASIT)).isEqualTo(0L)
    }

    // ── repairing an install whose URLs changed ───────────────────────────────

    @Test
    fun `an install that predates the version stamp has its recordings deleted`() {
        val makkah = writeAudio(AdhanSound.MAKKAH, isFajr = false)
        val mishary = writeAudio(AdhanSound.MISHARY, isFajr = true)

        manager.invalidateStaleDownloads()

        // This is the undo for "the regular URL was serving the Fajr recording".
        assertThat(makkah.exists()).isFalse()
        assertThat(mishary.exists()).isFalse()
    }

    @Test
    fun `the generated beep survives an invalidation`() {
        val beep = writeAudio(AdhanSound.SIMPLE_BEEP, isFajr = false)

        manager.invalidateStaleDownloads()

        // It has no URL, so a URL change cannot have staled it — deleting it would be 45s of
        // regeneration for nothing.
        assertThat(beep.exists()).isTrue()
    }

    @Test
    fun `an install already at the current version keeps its recordings`() {
        writeAudio(AdhanSound.MAKKAH, isFajr = false)
        manager.invalidateStaleDownloads()
        val kept = writeAudio(AdhanSound.MAKKAH, isFajr = false)

        manager.invalidateStaleDownloads()

        // Re-running must be a no-op, or every launch re-downloads 15 MB.
        assertThat(kept.exists()).isTrue()
    }

    @Test
    fun `a corrupted version stamp is treated as no stamp at all`() {
        adhanDir.mkdirs()
        File(adhanDir, ".adhan_url_version").writeText("not a number")
        val makkah = writeAudio(AdhanSound.MAKKAH, isFajr = false)

        manager.invalidateStaleDownloads()

        assertThat(makkah.exists()).isFalse()
        assertThat(File(adhanDir, ".adhan_url_version").readText().trim().toInt()).isAtLeast(2)
    }

    @Test
    fun `the version stamp is not itself deleted by the invalidation it triggers`() {
        writeAudio(AdhanSound.MAKKAH, isFajr = false)

        manager.invalidateStaleDownloads()

        // If the sweep took the stamp with it, every launch would invalidate again.
        assertThat(File(adhanDir, ".adhan_url_version").exists()).isTrue()
    }

    // ── interrupted downloads ─────────────────────────────────────────────────

    @Test
    fun `stale temp files are cleaned up and finished files are not`() {
        adhanDir.mkdirs()
        val temp = File(adhanDir, "${AdhanSound.MAKKAH.fileName}.tmp").apply { writeBytes(ByteArray(50)) }
        val done = writeAudio(AdhanSound.MISHARY, isFajr = false)

        manager.cleanupTempFiles()

        assertThat(temp.exists()).isFalse()
        assertThat(done.exists()).isTrue()
    }

    @Test
    fun `cleaning up on a device that has never downloaded anything does nothing`() {
        manager.cleanupTempFiles()
        manager.invalidateStaleDownloads()

        assertThat(manager.downloadState.value).isEmpty()
    }

    // ── the generated chime ───────────────────────────────────────────────────

    @Test
    fun `the beep generates a file the manager itself accepts as audio`() = runTest {
        val generated = manager.downloadAdhan(AdhanSound.SIMPLE_BEEP)

        assertThat(generated).isTrue()
        // The round trip is the point: a wrong WAV header means `isDownloaded` rejects the
        // manager's own output and the beep can never finish downloading.
        assertThat(manager.isDownloaded(AdhanSound.SIMPLE_BEEP)).isTrue()
        assertThat(manager.downloadState.value[AdhanSound.SIMPLE_BEEP])
            .isEqualTo(DownloadState.Completed)
    }

    @Test
    fun `the generated chime is a real RIFF WAV of the length its header claims`() = runTest {
        manager.downloadAdhan(AdhanSound.SIMPLE_BEEP)

        val bytes = File(adhanDir, AdhanSound.SIMPLE_BEEP.fileName).readBytes()

        assertThat(String(bytes.copyOfRange(0, 4))).isEqualTo("RIFF")
        assertThat(String(bytes.copyOfRange(8, 12))).isEqualTo("WAVE")
        assertThat(String(bytes.copyOfRange(36, 40))).isEqualTo("data")
        // RIFF size counts everything after the first eight bytes.
        assertThat(littleEndianInt(bytes, 4)).isEqualTo(bytes.size - 8)
        assertThat(littleEndianInt(bytes, 40)).isEqualTo(bytes.size - 44)
        // 44,100 Hz, mono, 16-bit — and a chime that actually lasts about 1.5 seconds.
        assertThat(littleEndianInt(bytes, 24)).isEqualTo(44_100)
        assertThat(bytes.size).isGreaterThan(44 + 44_100)
    }

    @Test
    fun `the generated chime is not silence`() = runTest {
        manager.downloadAdhan(AdhanSound.SIMPLE_BEEP)

        val samples = File(adhanDir, AdhanSound.SIMPLE_BEEP.fileName).readBytes().drop(44)

        // Three overlapping notes with a fade envelope: a synthesiser that emits a flat zero
        // buffer produces a file of exactly the right size that plays nothing at all.
        assertThat(samples.any { it != 0.toByte() }).isTrue()
    }

    @Test
    fun `a beep that is already generated is not generated again`() = runTest {
        manager.downloadAdhan(AdhanSound.SIMPLE_BEEP)
        val first = File(adhanDir, AdhanSound.SIMPLE_BEEP.fileName).lastModified()

        assertThat(manager.downloadAdhan(AdhanSound.SIMPLE_BEEP)).isTrue()

        assertThat(File(adhanDir, AdhanSound.SIMPLE_BEEP.fileName).lastModified()).isEqualTo(first)
        assertThat(manager.downloadState.value[AdhanSound.SIMPLE_BEEP])
            .isEqualTo(DownloadState.Completed)
    }

    @Test
    fun `an interrupted attempt's temp file is cleared before the next one starts`() = runTest {
        adhanDir.mkdirs()
        val temp = File(adhanDir, "${AdhanSound.SIMPLE_BEEP.fileName}.tmp")
            .apply { writeBytes(ByteArray(10)) }

        manager.downloadAdhan(AdhanSound.SIMPLE_BEEP)

        assertThat(temp.exists()).isFalse()
    }

    @Test
    fun `a corrupt file on disk is replaced rather than trusted`() = runTest {
        val corrupt = File(adhanDir.also { it.mkdirs() }, AdhanSound.SIMPLE_BEEP.fileName)
        corrupt.writeBytes("<html>404</html>".toByteArray() + ByteArray(20_000))

        assertThat(manager.downloadAdhan(AdhanSound.SIMPLE_BEEP)).isTrue()

        assertThat(String(corrupt.readBytes().copyOfRange(0, 4))).isEqualTo("RIFF")
    }

    @Test
    fun `downloading both variants of the beep writes the one file they share`() = runTest {
        assertThat(manager.downloadAdhanWithFajr(AdhanSound.SIMPLE_BEEP)).isTrue()

        assertThat(manager.isFullyDownloaded(AdhanSound.SIMPLE_BEEP)).isTrue()
        assertThat(adhanDir.listFiles()!!.filter { it.name.startsWith("adhan_beep") }).hasSize(1)
    }

    // ── deleting ──────────────────────────────────────────────────────────────

    @Test
    fun `deleting one variant leaves the pair's state alone while the other survives`() {
        writeAudio(AdhanSound.MAKKAH, isFajr = false)
        writeAudio(AdhanSound.MAKKAH, isFajr = true)

        assertThat(manager.deleteAdhan(AdhanSound.MAKKAH, isFajr = true)).isTrue()

        assertThat(manager.isDownloaded(AdhanSound.MAKKAH, isFajr = false)).isTrue()
        // The pair is not Idle: half of it is still on disk.
        assertThat(manager.downloadState.value[AdhanSound.MAKKAH]).isNull()
    }

    @Test
    fun `deleting the last variant resets the sound to idle`() {
        writeAudio(AdhanSound.MAKKAH, isFajr = false)

        assertThat(manager.deleteAdhan(AdhanSound.MAKKAH, isFajr = false)).isTrue()

        assertThat(manager.downloadState.value[AdhanSound.MAKKAH]).isEqualTo(DownloadState.Idle)
    }

    @Test
    fun `deleting a sound that is not there reports nothing was deleted`() {
        assertThat(manager.deleteAdhan(AdhanSound.MAKKAH)).isFalse()
    }

    @Test
    fun `deleting fully removes both variants and resets the state`() {
        writeAudio(AdhanSound.MAKKAH, isFajr = false)
        writeAudio(AdhanSound.MAKKAH, isFajr = true)

        assertThat(manager.deleteAdhanFully(AdhanSound.MAKKAH)).isTrue()

        assertThat(manager.isDownloaded(AdhanSound.MAKKAH, isFajr = false)).isFalse()
        assertThat(manager.isDownloaded(AdhanSound.MAKKAH, isFajr = true)).isFalse()
        assertThat(manager.downloadState.value[AdhanSound.MAKKAH]).isEqualTo(DownloadState.Idle)
    }

    @Test
    fun `deleting fully still reports success when only one variant was present`() {
        writeAudio(AdhanSound.MAKKAH, isFajr = true)

        assertThat(manager.deleteAdhanFully(AdhanSound.MAKKAH)).isTrue()
    }

    @Test
    fun `deleting fully a sound that was never downloaded reports nothing removed`() {
        assertThat(manager.deleteAdhanFully(AdhanSound.MAKKAH)).isFalse()
        assertThat(manager.downloadState.value[AdhanSound.MAKKAH]).isEqualTo(DownloadState.Idle)
    }

    // ── preview ───────────────────────────────────────────────────────────────

    @Test
    fun `previewing a sound that is not downloaded plays nothing and says so`() {
        manager.preview(AdhanSound.MAKKAH)

        assertThat(manager.isPlaying.value).isFalse()
        assertThat(manager.currentlyPlaying.value).isNull()
    }

    @Test
    fun `stopping a preview that was never started is safe`() {
        manager.stopPreview()

        assertThat(manager.isPlaying.value).isFalse()
        assertThat(manager.currentlyPlaying.value).isNull()
    }

    @Test
    fun `a preview of an unplayable file leaves nothing playing rather than throwing`() {
        // A file that exists but is not decodable — the state has to end up clean either way,
        // or the settings screen shows a stop button for a sound that never started.
        writeAudio(AdhanSound.MAKKAH, isFajr = false)

        manager.preview(AdhanSound.MAKKAH)
        manager.stopPreview()

        assertThat(manager.isPlaying.value).isFalse()
        assertThat(manager.currentlyPlaying.value).isNull()
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** A file the manager will accept: audio magic bytes and comfortably over the size floor. */
    private fun writeAudio(
        sound: AdhanSound,
        isFajr: Boolean,
        header: ByteArray = "ID3".toByteArray() + byteArrayOf(0x04),
        size: Int = 20_000,
    ): File {
        adhanDir.mkdirs()
        return File(adhanDir, sound.getFileName(isFajr)).apply {
            writeBytes(header + ByteArray(size - header.size))
        }
    }

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
}
