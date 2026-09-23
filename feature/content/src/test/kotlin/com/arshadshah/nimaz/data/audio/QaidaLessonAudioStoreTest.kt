package com.arshadshah.nimaz.data.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.presentation.screens.qaida.qaidaLessonContent
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaLessonAudioStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val content = qaidaLessonContent()
    private val cells = content.lines.flatMap { it.cells }
    private val fingerprint = QaidaLessonAudioStore.fingerprint(cells.map { Triple(it.id, it.audioKey, it.textArabic) })
    private val root get() = File(context.filesDir, "qaida_lessons")
    private val directory get() = File(root, "1/$fingerprint")
    private lateinit var store: QaidaLessonAudioStore
    @Before fun clean() { root.deleteRecursively(); store = QaidaLessonAudioStore(context) }
    private fun cached(change: (JSONObject) -> Unit = {}) {
        directory.mkdirs()
        val clips = JSONArray()
        cells.forEach { cell ->
            val bytes = "reviewed test fixture ${cell.id}".toByteArray()
            val hash = QaidaLessonAudioStore.sha256(bytes)
            File(directory, "$hash.mp3").writeBytes(bytes)
            clips.put(JSONObject().put("audio_key", cell.audioKey).put("text_arabic", cell.textArabic)
                .put("sha256", hash).put("bytes", bytes.size).put("path", "clips/$hash.mp3"))
        }
        val manifest = JSONObject().put("version", 1).put("lesson_id", 1).put("content_sha256", fingerprint).put("clips", clips)
        change(manifest)
        File(directory, "manifest.json").writeText(manifest.toString())
    }
    @Test fun `complete cached lesson works without an endpoint including after process recreation`() = runTest {
        cached()
        assertThat(store.prepare(content)).isTrue()
        val recreated = QaidaLessonAudioStore(context)
        assertThat(recreated.prepare(content)).isTrue()
        assertThat(recreated.resolve(cells.first().audioKey)?.isFile).isTrue()
    }
    @Test fun `missing cache and unconfigured endpoint leaves practice available without fake audio`() = runTest {
        assertThat(store.prepare(content)).isFalse()
        assertThat(store.resolve(cells.first().audioKey)).isNull()
    }
    @Test fun `corruption refuses the whole lesson`() = runTest {
        cached(); directory.listFiles()!!.first { it.extension == "mp3" }.writeText("corrupt")
        assertThat(store.prepare(content)).isFalse()
        cells.forEach { assertThat(store.resolve(it.audioKey)).isNull() }
    }
    @Test fun `old text fingerprint is refused`() = runTest {
        cached { it.put("content_sha256", "0".repeat(64)) }
        assertThat(store.prepare(content)).isFalse()
    }
    @Test fun `wrong Arabic is refused even with a matching outer fingerprint`() = runTest {
        cached { it.getJSONArray("clips").getJSONObject(0).put("text_arabic", "wrong") }
        assertThat(store.prepare(content)).isFalse()
    }
    @Test fun `duplicate keys and traversal paths are refused`() = runTest {
        cached { val clips = it.getJSONArray("clips"); clips.getJSONObject(1).put("audio_key", cells.first().audioKey) }
        assertThat(store.prepare(content)).isFalse()
        cached { it.getJSONArray("clips").getJSONObject(0).put("path", "../secret") }
        assertThat(store.prepare(content)).isFalse()
    }
    @Test fun `oversized clips and incomplete manifests are refused`() = runTest {
        cached { it.getJSONArray("clips").getJSONObject(0).put("bytes", 3000000) }
        assertThat(store.prepare(content)).isFalse()
        cached { it.getJSONArray("clips").remove(0) }
        assertThat(store.prepare(content)).isFalse()
    }
    @Test fun `clearing audio removes resolved files and returns disk usage to zero`() = runTest {
        cached(); store.prepare(content)
        assertThat(store.sizeBytes()).isGreaterThan(0)
        store.clear()
        assertThat(store.sizeBytes()).isEqualTo(0)
        assertThat(store.resolve(cells.first().audioKey)).isNull()
    }
    @Test fun `fingerprint is independent of input order but sensitive to exact text`() {
        val values = cells.map { Triple(it.id, it.audioKey, it.textArabic) }
        assertThat(QaidaLessonAudioStore.fingerprint(values.reversed())).isEqualTo(fingerprint)
        assertThat(QaidaLessonAudioStore.fingerprint(values.map { Triple(it.first, it.second, it.third + "َ") })).isNotEqualTo(fingerprint)
    }
}
