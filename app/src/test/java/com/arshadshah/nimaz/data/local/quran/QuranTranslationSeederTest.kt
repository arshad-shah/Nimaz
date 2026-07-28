package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.TranslationEntity
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class QuranTranslationSeederTest {

    private val translation = QuranTranslation.EN_PICKTHALL
    private val other = QuranTranslation.UR_MAUDUDI

    /** A well-formed asset: exactly 6,236 positional verses, index i -> ayah id i+1. */
    private fun assetJson(
        id: String,
        version: Int = 1,
        count: Int = QuranTranslationSeeder.EXPECTED_AYAH_COUNT
    ): String {
        val texts = (1..count).joinToString(",") { "\"verse $it\"" }
        return """{"translationId":"$id","contentVersion":$version,"source":"t","texts":[$texts]}"""
    }

    private fun seeder(
        dao: QuranDao,
        stored: Map<String, Int> = emptyMap(),
        assets: Map<String, String> = mapOf(
            QuranTranslationSeeder.assetPath(translation) to assetJson(translation.id),
            QuranTranslationSeeder.assetPath(other) to assetJson(other.id)
        )
    ): QuranTranslationSeeder {
        val store = object : TranslationContentVersionStore {
            val versions = stored.toMutableMap()
            override suspend fun get(translationId: String) = versions[translationId] ?: 0
            override suspend fun set(translationId: String, version: Int) {
                versions[translationId] = version
            }
        }
        val reader = object : QuranAssetReader {
            override fun read(path: String): String =
                assets[path] ?: error("unexpected asset $path")
        }
        return QuranTranslationSeeder(dao = dao, versionStore = store, assetReader = reader)
    }

    @Test
    fun seedsWhenAbsent_mapsPositionalTextsOntoGlobalAyahIds() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countTranslationsFor(translation.id) } returns 0
        val rows = slot<List<TranslationEntity>>()
        coEvery { dao.replaceTranslation(translation.id, capture(rows)) } returns Unit

        seeder(dao).seedIfNeeded(translation)

        assertThat(rows.captured).hasSize(QuranTranslationSeeder.EXPECTED_AYAH_COUNT)
        // Index 0 of the asset is global ayah id 1 — the positional contract the whole format
        // rests on. Getting this off by one would silently shift every verse.
        assertThat(rows.captured.first().ayahId).isEqualTo(1)
        assertThat(rows.captured.first().text).isEqualTo("verse 1")
        assertThat(rows.captured.last().ayahId)
            .isEqualTo(QuranTranslationSeeder.EXPECTED_AYAH_COUNT)
        assertThat(rows.captured.all { it.translatorId == translation.id }).isTrue()
    }

    @Test
    fun skipsWhenFullyPopulatedAndVersionCurrent() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countTranslationsFor(translation.id) } returns
                QuranTranslationSeeder.EXPECTED_AYAH_COUNT

        seeder(dao, stored = mapOf(translation.id to 1)).seedIfNeeded(translation)

        coVerify(exactly = 0) { dao.replaceTranslation(any(), any()) }
    }

    @Test
    fun reseedsWhenPartiallyPopulated() = runTest {
        // An interrupted seed leaves a partial translation; a plain "> 0" check would call that
        // done and leave the reader with holes.
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countTranslationsFor(translation.id) } returns 12

        seeder(dao, stored = mapOf(translation.id to 1)).seedIfNeeded(translation)

        coVerify(exactly = 1) { dao.replaceTranslation(translation.id, any()) }
    }

    @Test
    fun reseedsWhenAssetVersionIsNewerThanStored() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countTranslationsFor(translation.id) } returns
                QuranTranslationSeeder.EXPECTED_AYAH_COUNT

        seeder(
            dao,
            stored = mapOf(translation.id to 1),
            assets = mapOf(
                QuranTranslationSeeder.assetPath(translation) to
                        assetJson(translation.id, version = 2)
            )
        ).seedIfNeeded(translation)

        coVerify(exactly = 1) { dao.replaceTranslation(translation.id, any()) }
    }

    @Test
    fun seedingOneTranslationLeavesOthersUntouched() = runTest {
        // The whole point of scoping the replace by translator_id: selecting Urdu must not
        // disturb the English verses a user already has.
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countTranslationsFor(any()) } returns 0

        seeder(dao).seedIfNeeded(other)

        coVerify(exactly = 1) { dao.replaceTranslation(other.id, any()) }
        coVerify(exactly = 0) { dao.replaceTranslation(translation.id, any()) }
    }

    @Test
    fun secondCallSkipsTheDbCheckEntirely_onceConfirmedCurrent() = runTest {
        // Every page/juz/surah read calls seedIfNeeded, so the confirmed case must not go back
        // to the DB.
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countTranslationsFor(translation.id) } returns
                QuranTranslationSeeder.EXPECTED_AYAH_COUNT
        val instance = seeder(dao, stored = mapOf(translation.id to 1))

        instance.seedIfNeeded(translation)
        instance.seedIfNeeded(translation)
        instance.seedIfNeeded(translation)

        coVerify(exactly = 1) { dao.countTranslationsFor(translation.id) }
    }

    @Test
    fun rejectsAnAssetWithTheWrongVerseCount() = runTest {
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countTranslationsFor(translation.id) } returns 0

        val thrown = runCatching {
            seeder(
                dao,
                assets = mapOf(
                    QuranTranslationSeeder.assetPath(translation) to
                            assetJson(translation.id, count = 10)
                )
            ).seedIfNeeded(translation)
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) { dao.replaceTranslation(any(), any()) }
    }

    @Test
    fun rejectsAnAssetDeclaringADifferentTranslationId() = runTest {
        // Guards against a copy-paste in the generator silently filing Pickthall's verses
        // under Yusuf Ali's id.
        val dao = mockk<QuranDao>(relaxed = true)
        coEvery { dao.countTranslationsFor(translation.id) } returns 0

        val thrown = runCatching {
            seeder(
                dao,
                assets = mapOf(
                    QuranTranslationSeeder.assetPath(translation) to assetJson("en_yusuf_ali")
                )
            ).seedIfNeeded(translation)
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) { dao.replaceTranslation(any(), any()) }
    }
}
