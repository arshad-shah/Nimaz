package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.MushafLayoutIndopak16Entity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class QuranIndopakSeederTest {

    private val ayahsJson = """
        [
          {"ayah_id":1,"text_indopak":"بِسْمِ اللّٰهِ","words":["بِسْمِ","اللّٰهِ"]},
          {"ayah_id":2,"text_indopak":"الْحَمْدُ لِلّٰهِ","words":["الْحَمْدُ","لِلّٰهِ"]}
        ]
    """.trimIndent()

    private val layoutJson = """
        [
          {"page_number":1,"line_number":1,"line_type":"surah_header","surah_id":1,
           "ayah_id":null,"first_word_position":null,"last_word_position":null},
          {"page_number":1,"line_number":2,"line_type":"ayah","surah_id":1,
           "ayah_id":1,"first_word_position":1,"last_word_position":2},
          {"page_number":1,"line_number":3,"line_type":"ayah","surah_id":1,
           "ayah_id":2,"first_word_position":1,"last_word_position":2}
        ]
    """.trimIndent()

    private fun seeder(dao: QuranDao, storedVersion: Int): QuranIndopakSeeder {
        val store = object : IndopakContentVersionStore {
            var v = storedVersion
            override suspend fun get() = v
            override suspend fun set(version: Int) { v = version }
        }
        val reader = object : QuranAssetReader {
            override fun read(path: String): String =
                if (path == QuranIndopakSeeder.AYAHS_ASSET) ayahsJson else layoutJson
        }
        return QuranIndopakSeeder(dao = dao, versionStore = store, assetReader = reader)
    }

    @Test
    fun seedsWhenEmpty_mapsTextAndLayout() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countMushafLayoutIndopak16() } returns 0
        val texts = slot<Map<Int, String>>()
        val rows = slot<List<MushafLayoutIndopak16Entity>>()
        coEvery { dao.replaceMushafIndopak16(capture(texts), capture(rows)) } returns Unit

        seeder(dao, storedVersion = 0).seedIfNeeded()

        // Per-ayah IndoPak text is keyed by the global ayah id.
        assertThat(texts.captured).containsExactly(1, "بِسْمِ اللّٰهِ", 2, "الْحَمْدُ لِلّٰهِ")

        // All three layout segments map through, preserving order and null-handling.
        assertThat(rows.captured).hasSize(3)
        val header = rows.captured[0]
        assertThat(header.lineType).isEqualTo("surah_header")
        assertThat(header.ayahId).isNull()
        assertThat(header.firstWordPosition).isNull()
        val ayahLine = rows.captured[1]
        assertThat(ayahLine.lineType).isEqualTo("ayah")
        assertThat(ayahLine.page).isEqualTo(1)
        assertThat(ayahLine.line).isEqualTo(2)
        assertThat(ayahLine.ayahId).isEqualTo(1)
        assertThat(ayahLine.firstWordPosition).isEqualTo(1)
        assertThat(ayahLine.lastWordPosition).isEqualTo(2)

        coVerify(exactly = 1) { dao.replaceMushafIndopak16(any(), any()) }
    }

    @Test
    fun skipsWhenPopulatedAndVersionCurrent() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countMushafLayoutIndopak16() } returns 3
        seeder(dao, storedVersion = QuranIndopakSeeder.INDOPAK_CONTENT_VERSION).seedIfNeeded()
        coVerify(exactly = 0) { dao.replaceMushafIndopak16(any(), any()) }
    }

    @Test
    fun reseedsWhenStoredVersionIsOlder() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countMushafLayoutIndopak16() } returns 3 // populated, but stale version
        seeder(dao, storedVersion = 0).seedIfNeeded()
        coVerify(exactly = 1) { dao.replaceMushafIndopak16(any(), any()) }
    }

    @Test
    fun seedsWhenTablesEmptyEvenIfVersionCurrent() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countMushafLayoutIndopak16() } returns 0
        seeder(dao, storedVersion = QuranIndopakSeeder.INDOPAK_CONTENT_VERSION).seedIfNeeded()
        coVerify(exactly = 1) { dao.replaceMushafIndopak16(any(), any()) }
    }

    @Test
    fun secondCallSkipsTheDbCheckEntirely_onceConfirmedCurrent() = runTest {
        // getMushafPageLayout calls seedIfNeeded() on every page fetch (#280 review) — once a
        // process has confirmed the data is current, later calls must not retake the mutex for
        // another countMushafLayoutIndopak16()/version read.
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countMushafLayoutIndopak16() } returns 3
        val instance = seeder(dao, storedVersion = QuranIndopakSeeder.INDOPAK_CONTENT_VERSION)

        instance.seedIfNeeded()
        instance.seedIfNeeded()
        instance.seedIfNeeded()

        coVerify(exactly = 1) { dao.countMushafLayoutIndopak16() }
        coVerify(exactly = 0) { dao.replaceMushafIndopak16(any(), any()) }
    }
}
