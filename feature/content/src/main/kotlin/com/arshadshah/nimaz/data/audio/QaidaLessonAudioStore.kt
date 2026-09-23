package com.arshadshah.nimaz.data.audio

import android.content.Context
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.feature.content.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Only verified complete lessons become playable. No audio ships in the APK. */
@Singleton
class QaidaLessonAudioStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val lock = Mutex()
    private val resolved = ConcurrentHashMap<String, File>()
    private val root get() = File(context.filesDir, "qaida_lessons")
    fun resolve(key: String): File? = resolved[key]?.takeIf { it.isFile }

    suspend fun prepare(
        content: QaidaLessonContent,
        refresh: Boolean = false,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean = withContext(Dispatchers.IO) {
        lock.withLock {
            val cells = content.lines.flatMap { it.cells }.sortedBy { it.id }
            cells.forEach { resolved.remove(it.audioKey) }
            if (cells.isEmpty()) throw IOException("empty_content")
            val fingerprint = fingerprint(cells.map { Triple(it.id, it.audioKey, it.textArabic) })
            val directory = File(root, "${content.lesson.id}/$fingerprint")
            val manifestFile = File(directory, "manifest.json")
            fun validate(manifest: JSONObject): List<AudioClip> {
                require(manifest.getInt("version") == 1)
                require(manifest.getInt("lesson_id") == content.lesson.id)
                require(manifest.getString("content_sha256") == fingerprint)
                val array = manifest.getJSONArray("clips")
                require(array.length() == cells.size)
                val expected = cells.associateBy { it.audioKey }
                val seen = mutableSetOf<String>()
                return (0 until array.length()).map { index ->
                    val entry = array.getJSONObject(index)
                    val key = entry.getString("audio_key")
                    val sha = entry.getString("sha256")
                    val bytes = entry.getLong("bytes")
                    require(seen.add(key) && expected[key]?.textArabic == entry.getString("text_arabic"))
                    require(SHA.matches(sha) && bytes in 1..MAX_CLIP_BYTES)
                    require(entry.getString("path") == "clips/$sha.mp3")
                    AudioClip(key, sha, bytes)
                }.also { require(it.sumOf { c -> c.bytes } <= MAX_LESSON_BYTES) }
            }
            fun useCached(): Boolean = runCatching {
                val clips = validate(JSONObject(manifestFile.readText()))
                require(clips.all { clip -> verified(File(directory, "${clip.sha}.mp3"), clip) })
                clips.forEach { resolved[it.key] = File(directory, "${it.sha}.mp3") }
                true
            }.getOrDefault(false)
            if (!refresh && useCached()) return@withLock true
            val configured = context.getString(R.string.qaida_audio_base_url).trimEnd('/')
            if (configured.isBlank()) return@withLock useCached()
            val uri = URI(configured)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null)
            val manifestBytes = fetch("$configured/lessons/${content.lesson.id}/manifest.json", MAX_MANIFEST_BYTES)
            val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
            val clips = validate(manifest)
            root.mkdirs()
            val stage = File(root, ".stage-${content.lesson.id}-${System.nanoTime()}").apply { mkdirs() }
            try {
                clips.forEachIndexed { index, clip ->
                    currentCoroutineContext().ensureActive()
                    onProgress(index, clips.size)
                    val destination = File(stage, "${clip.sha}.mp3")
                    val cached = File(directory, destination.name)
                    if (verified(destination, clip)) Unit
                    else if (verified(cached, clip)) cached.copyTo(destination)
                    else {
                        val bytes = fetch("$configured/clips/${clip.sha}.mp3", clip.bytes)
                        require(bytes.size.toLong() == clip.bytes && sha256(bytes) == clip.sha)
                        destination.writeBytes(bytes)
                    }
                }
                File(stage, "manifest.json").writeBytes(manifestBytes)
                // The mutex excludes reads/clears during the replacement. Keep old data until
                // all new bytes verify; a failed download never destroys a usable lesson.
                val backup = File(root, ".backup-${content.lesson.id}-${System.nanoTime()}")
                directory.parentFile?.mkdirs()
                if (directory.exists()) check(directory.renameTo(backup))
                if (!stage.renameTo(directory)) {
                    backup.renameTo(directory)
                    throw IOException("cache_commit_failed")
                }
                backup.deleteRecursively()
                clips.forEach { resolved[it.key] = File(directory, "${it.sha}.mp3") }
                // Retire older content editions only after the replacement is complete.
                directory.parentFile?.listFiles()?.filter { it != directory }?.forEach { it.deleteRecursively() }
                onProgress(clips.size, clips.size)
                true
            } finally { stage.deleteRecursively() }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        lock.withLock { resolved.clear(); root.deleteRecursively(); Unit }
    }
    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        lock.withLock { if (root.exists()) root.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L }
    }

    private suspend fun fetch(address: String, limit: Long): ByteArray {
        val connection = URI(address).toURL().openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json, audio/mpeg")
            if (connection.responseCode != 200) throw IOException("http_${connection.responseCode}")
            if (connection.contentLengthLong > limit) throw IOException("too_large")
            return connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size().toLong() + count > limit) throw IOException("too_large")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally { connection.disconnect() }
    }

    private fun verified(file: File, clip: AudioClip): Boolean =
        file.isFile && file.length() == clip.bytes && sha256(file.readBytes()) == clip.sha

    private data class AudioClip(val key: String, val sha: String, val bytes: Long)
    companion object {
        private val SHA = Regex("[a-f0-9]{64}")
        private const val MAX_CLIP_BYTES = 2L * 1024 * 1024
        private const val MAX_LESSON_BYTES = 16L * 1024 * 1024
        private const val MAX_MANIFEST_BYTES = 512L * 1024
        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        fun fingerprint(cells: List<Triple<Int, String, String>>): String = sha256(
            cells.sortedBy { it.first }.joinToString("") { "${it.first}\n${it.second}\n${it.third}\n" }.toByteArray(Charsets.UTF_8)
        )
    }
}
