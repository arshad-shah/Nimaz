package com.arshadshah.nimaz.data.local.hadith

import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HadithBackfillSeederTest {

    private val json = """
        { "contentVersion": 1, "fills": [
          { "id": 101, "reference": "bukhari:5710",
            "textArabic": "حَدَّثَنَا فُلَانٌ", "textEnglish": "Narrated X.", "narrator": "X" },
          { "id": 102, "reference": "muslim:1",
            "textArabic": "حَدَّثَنَا عَلِيٌّ", "textEnglish": "Narrated Y.", "narrator": "Y" },
          { "id": 103, "reference": "muslim:2",
            "textArabic": "", "textEnglish": "blank", "narrator": "" }
        ] }
    """.trimIndent()

    private fun seeder(dao: HadithDao, storedVersion: Int): HadithBackfillSeeder {
        val store = object : HadithBackfillVersionStore {
            var v = storedVersion
            override suspend fun get() = v
            override suspend fun set(version: Int) { v = version }
        }
        val reader = object : HadithAssetReader {
            override fun read(path: String): String = json
        }
        return HadithBackfillSeeder(dao = dao, versionStore = store, assetReader = reader)
    }

    @Test
    fun appliesFillsWhenGapsPresent_andSkipsBlankArabic() = runTest {
        val dao = mockk<HadithDao>(relaxed = true)
        coEvery { dao.emptyArabicCount() } returns 379

        seeder(dao, storedVersion = 0).seedIfNeeded()

        coVerify(exactly = 1) { dao.backfillHadith(101, any(), any(), any()) }
        coVerify(exactly = 1) { dao.backfillHadith(102, any(), any(), any()) }
        // id 103 has blank arabic -> never written
        coVerify(exactly = 0) { dao.backfillHadith(103, any(), any(), any()) }
    }

    @Test
    fun skipsWhenNoGapsAndAlreadyApplied() = runTest {
        val dao = mockk<HadithDao>(relaxed = true)
        coEvery { dao.emptyArabicCount() } returns 0

        seeder(dao, storedVersion = 1).seedIfNeeded()

        coVerify(exactly = 0) { dao.backfillHadith(any(), any(), any(), any()) }
    }

    @Test
    fun reseedsWhenContentVersionIncremented_evenWithoutGaps() = runTest {
        val dao = mockk<HadithDao>(relaxed = true)
        coEvery { dao.emptyArabicCount() } returns 0

        // stored 0 < bundled contentVersion 1 -> re-applies
        seeder(dao, storedVersion = 0).seedIfNeeded()

        coVerify(exactly = 1) { dao.backfillHadith(101, any(), any(), any()) }
    }
}
