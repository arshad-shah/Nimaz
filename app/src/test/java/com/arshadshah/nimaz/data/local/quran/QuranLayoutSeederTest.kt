package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.MushafLayoutEntity
import com.arshadshah.nimaz.domain.model.quran.catalogue.QuranEditions
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class QuranLayoutSeederTest {

    private val indopak16 = QuranEditions.layout("indopak16")
    private val assets = QuranContentAssets.mushafLayouts.getValue(indopak16.id)
    private val currentVersion = assets.layout.contentVersion

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

    private class FakeVersionStore(initial: Int) : ContentVersionStore {
        val versions = mutableMapOf<String, Int>()
        val readKeys = mutableListOf<String>()

        init {
            versions[QuranLayoutSeeder.contentKey("indopak16")] = initial
        }

        override suspend fun get(key: String): Int {
            readKeys += key
            return versions[key] ?: 0
        }

        override suspend fun set(key: String, version: Int) {
            versions[key] = version
        }
    }

    private fun seeder(
        dao: QuranDao,
        storedVersion: Int
    ): Pair<QuranLayoutSeeder, FakeVersionStore> {
        val store = FakeVersionStore(storedVersion)
        val reader = object : QuranAssetReader {
            override fun read(path: String): String =
                if (path == assets.ayahText?.assetPath) ayahsJson else layoutJson
        }
        return QuranLayoutSeeder(dao = dao, versionStore = store, assetReader = reader) to store
    }

    @Test
    fun seedsWhenEmpty_mapsTextAndLayoutAndStampsTheLayoutId() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countMushafLayout(indopak16.id) } returns 0
        val texts = slot<Map<Int, String>>()
        val rows = slot<List<MushafLayoutEntity>>()
        coEvery {
            dao.replaceMushafLayout(indopak16.id, capture(texts), capture(rows))
        } returns Unit

        seeder(dao, storedVersion = 0).first.seedIfNeeded(indopak16)

        // Per-ayah IndoPak text is keyed by the global ayah id.
        assertThat(texts.captured).containsExactly(1, "بِسْمِ اللّٰهِ", 2, "الْحَمْدُ لِلّٰهِ")

        // All three layout segments map through, preserving order and null-handling.
        assertThat(rows.captured).hasSize(3)
        // Every row carries the discriminator, or it would be invisible to every scoped read.
        assertThat(rows.captured.map { it.layoutId }.distinct()).containsExactly(indopak16.id)
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

        coVerify(exactly = 1) { dao.replaceMushafLayout(indopak16.id, any(), any()) }
    }

    @Test
    fun skipsWhenPopulatedAndVersionCurrent() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countMushafLayout(indopak16.id) } returns 3
        seeder(dao, storedVersion = currentVersion).first.seedIfNeeded(indopak16)
        coVerify(exactly = 0) { dao.replaceMushafLayout(any(), any(), any()) }
    }

    @Test
    fun reseedsWhenStoredVersionIsOlder() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countMushafLayout(indopak16.id) } returns 3 // populated, stale version
        seeder(dao, storedVersion = 0).first.seedIfNeeded(indopak16)
        coVerify(exactly = 1) { dao.replaceMushafLayout(indopak16.id, any(), any()) }
    }

    @Test
    fun seedsWhenTablesEmptyEvenIfVersionCurrent() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countMushafLayout(indopak16.id) } returns 0
        seeder(dao, storedVersion = currentVersion).first.seedIfNeeded(indopak16)
        coVerify(exactly = 1) { dao.replaceMushafLayout(indopak16.id, any(), any()) }
    }

    @Test
    fun secondCallSkipsTheDbCheckEntirely_onceConfirmedCurrent() = runTest {
        // getMushafPageLayout calls seedIfNeeded() on every page fetch (#280 review) — once a
        // process has confirmed the data is current, later calls must not retake the mutex for
        // another countMushafLayout()/version read.
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countMushafLayout(indopak16.id) } returns 3
        val (instance, _) = seeder(dao, storedVersion = currentVersion)

        instance.seedIfNeeded(indopak16)
        instance.seedIfNeeded(indopak16)
        instance.seedIfNeeded(indopak16)

        coVerify(exactly = 1) { dao.countMushafLayout(indopak16.id) }
        coVerify(exactly = 0) { dao.replaceMushafLayout(any(), any(), any()) }
    }

    @Test
    fun aFlowedEditionIsNeverSeeded() = runTest {
        // Madani paginates by the `ayahs.page` column and has no layout rows at all, so
        // seeding must be a no-op rather than an empty-table re-seed on every page fetch.
        val dao = mockk<QuranDao>(relaxed = true)
        val madani = QuranEditions.layout("madani")
        assertThat(madani.hasLineLayout).isFalse()

        seeder(dao, storedVersion = 0).first.seedIfNeeded(madani)

        coVerify(exactly = 0) { dao.countMushafLayout(any()) }
        coVerify(exactly = 0) { dao.replaceMushafLayout(any(), any(), any()) }
    }

    @Test
    fun theVersionKeyIsScopedToTheEdition() = runTest {
        // Two editions must not share a version key, or seeding one would mark the other
        // current and leave its rows missing.
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countMushafLayout(indopak16.id) } returns 0
        val (instance, store) = seeder(dao, storedVersion = 0)

        instance.seedIfNeeded(indopak16)

        assertThat(store.readKeys).containsExactly("mushaf_layout.indopak16")
        assertThat(store.versions["mushaf_layout.indopak16"]).isEqualTo(currentVersion)
    }
}
