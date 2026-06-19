package com.arshadshah.nimaz.data.local.dua

import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DuaContentSeederTest {

    private val json = """
        { "contentVersion": 2,
          "categories": [
            { "id":1,"name_english":"Morning","name_arabic":"الصباح","icon":"🌅",
              "display_order":1,"dua_count":2 }
          ],
          "duas": [
            { "id":1,"category_id":1,"title_english":"Dua One","title_arabic":"دعاء",
              "text_arabic":"نص","transliteration":"nass","translation":"text",
              "source":"Quran","virtue":null,"repeat_count":3,"audio_file":null,
              "display_order":1 },
            { "id":2,"category_id":1,"title_english":"Dua Two","title_arabic":"دعاء",
              "text_arabic":"نص","transliteration":"nass","translation":"text",
              "source":"Hadith","repeat_count":1,"display_order":2 }
          ] }
    """.trimIndent()

    private fun seeder(dao: DuaDao, storedVersion: Int): DuaContentSeeder {
        val store = object : DuaContentVersionStore {
            var v = storedVersion
            override suspend fun get() = v
            override suspend fun set(version: Int) { v = version }
        }
        val reader = object : DuaAssetReader {
            override fun read(path: String): String = json
        }
        return DuaContentSeeder(dao = dao, versionStore = store, assetReader = reader)
    }

    @Test
    fun seedsWhenEmpty_mapsCategoriesAndDuas() = runTest {
        val dao = mockk<DuaDao>(relaxed = true)
        coEvery { dao.categoryCount() } returns 0
        val categories = slot<List<DuaCategoryEntity>>()
        val duas = slot<List<DuaEntity>>()
        coEvery { dao.replaceAllContent(capture(categories), capture(duas)) } returns Unit

        seeder(dao, storedVersion = 0).seedIfNeeded()

        assertThat(categories.captured.map { it.id }).containsExactly(1)
        assertThat(duas.captured.map { it.id }).containsExactly(1, 2)
        // Optional fields default correctly and required fields map through.
        val first = duas.captured.first { it.id == 1 }
        assertThat(first.repeatCount).isEqualTo(3)
        assertThat(first.virtue).isNull()
        assertThat(first.audioFile).isNull()
        coVerify(exactly = 1) { dao.replaceAllContent(any(), any()) }
    }

    @Test
    fun skipsWhenPopulatedAndVersionCurrent() = runTest {
        val dao = mockk<DuaDao>(relaxed = true)
        coEvery { dao.categoryCount() } returns 42
        seeder(dao, storedVersion = 2).seedIfNeeded()
        coVerify(exactly = 0) { dao.replaceAllContent(any(), any()) }
    }

    @Test
    fun reseedsWhenContentVersionIncremented() = runTest {
        val dao = mockk<DuaDao>(relaxed = true)
        coEvery { dao.categoryCount() } returns 42
        seeder(dao, storedVersion = 1).seedIfNeeded() // bundled is 2 > stored 1
        coVerify(exactly = 1) { dao.replaceAllContent(any(), any()) }
    }

    @Test
    fun reseedsWhenTablesEmptyEvenIfVersionCurrent() = runTest {
        val dao = mockk<DuaDao>(relaxed = true)
        coEvery { dao.categoryCount() } returns 0
        seeder(dao, storedVersion = 2).seedIfNeeded() // stored == bundled but empty
        coVerify(exactly = 1) { dao.replaceAllContent(any(), any()) }
    }
}
