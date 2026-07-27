package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.MushafAyahTextEntity
import com.arshadshah.nimaz.data.local.database.entity.MushafLayoutLineEntity
import com.arshadshah.nimaz.domain.model.MushafScript
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MushafLayoutSeederTest {

    private val script = MushafScript.INDOPAK_16
    private val textSource = requireNotNull(script.textSource)

    private val textJson = """
        [
          {"ayah_id":1,"text":"بِسْمِ اللّٰهِ","words":["بِسْمِ","اللّٰهِ"]},
          {"ayah_id":2,"text":"الْحَمْدُ لِلّٰهِ","words":["الْحَمْدُ","لِلّٰهِ"]}
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

    /**
     * The real assets carry all 6,236 ayahs and the seeder enforces that, so the fixture is
     * padded up to the expected count rather than shrinking the guard for the test.
     */
    private val paddedTextJson: String by lazy {
        val extra = (3..MushafLayoutSeeder.EXPECTED_AYAH_COUNT).joinToString(",") {
            """{"ayah_id":$it,"text":"نَصٌّ"}"""
        }
        textJson.trimEnd().removeSuffix("]").trimEnd() + ",$extra]"
    }

    private fun seeder(
        dao: QuranDao,
        storedVersion: Int,
        text: String = paddedTextJson
    ): MushafLayoutSeeder {
        val store = object : MushafContentVersionStore {
            private val versions = mutableMapOf<String, Int>()

            init {
                if (storedVersion > 0) versions[script.name] = storedVersion
            }

            override suspend fun get(script: String) = versions[script] ?: 0
            override suspend fun set(script: String, version: Int) {
                versions[script] = version
            }
        }
        val reader = object : QuranAssetReader {
            override fun read(path: String): String =
                if (path == MushafLayoutSeeder.textAsset(textSource)) text else layoutJson
        }
        return MushafLayoutSeeder(dao = dao, versionStore = store, assetReader = reader)
    }

    private fun populatedDao(layoutRows: Int): QuranDao = mockk<QuranDao>(relaxed = true) {
        coEvery { countLayoutLines(script.name) } returns layoutRows
        coEvery { countAyahTexts(textSource) } returns MushafLayoutSeeder.EXPECTED_AYAH_COUNT
    }

    @Test
    fun seedsWhenEmpty_mapsTextAndLayout() = runTest {
        val dao = populatedDao(layoutRows = 0)
        val texts = slot<List<MushafAyahTextEntity>>()
        val rows = slot<List<MushafLayoutLineEntity>>()
        coEvery {
            dao.replaceMushafLayout(script.name, textSource, capture(texts), capture(rows))
        } returns Unit

        seeder(dao, storedVersion = 0).seedIfNeeded(script)

        // Glyph text is keyed by (text source, global ayah id).
        assertThat(texts.captured).hasSize(MushafLayoutSeeder.EXPECTED_AYAH_COUNT)
        assertThat(texts.captured[0].ayahId).isEqualTo(1)
        assertThat(texts.captured[0].text).isEqualTo("بِسْمِ اللّٰهِ")
        assertThat(texts.captured[0].textSource).isEqualTo(textSource)
        assertThat(texts.captured[1].text).isEqualTo("الْحَمْدُ لِلّٰهِ")

        // All three layout segments map through, preserving order and null-handling.
        assertThat(rows.captured).hasSize(3)
        val header = rows.captured[0]
        assertThat(header.script).isEqualTo(script.name)
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

        coVerify(exactly = 1) { dao.replaceMushafLayout(script.name, textSource, any(), any()) }
    }

    @Test
    fun skipsWhenPopulatedAndVersionCurrent() = runTest {
        val dao = populatedDao(layoutRows = 3)
        seeder(dao, storedVersion = MushafLayoutSeeder.CONTENT_VERSION).seedIfNeeded(script)
        coVerify(exactly = 0) { dao.replaceMushafLayout(any(), any(), any(), any()) }
    }

    @Test
    fun reseedsWhenStoredVersionIsOlder() = runTest {
        val dao = populatedDao(layoutRows = 3) // populated, but stale version
        seeder(dao, storedVersion = 0).seedIfNeeded(script)
        coVerify(exactly = 1) { dao.replaceMushafLayout(script.name, textSource, any(), any()) }
    }

    @Test
    fun seedsWhenTablesEmptyEvenIfVersionCurrent() = runTest {
        val dao = populatedDao(layoutRows = 0)
        seeder(dao, storedVersion = MushafLayoutSeeder.CONTENT_VERSION).seedIfNeeded(script)
        coVerify(exactly = 1) { dao.replaceMushafLayout(script.name, textSource, any(), any()) }
    }

    @Test
    fun reseedsWhenGlyphTextIsMissingEvenIfTheLayoutIsPresent() = runTest {
        // The layout and its text source are separate tables; a half-populated DB (layout
        // present, glyphs gone) must repair rather than render a page of blank words.
        val dao = mockk<QuranDao>(relaxed = true) {
            coEvery { countLayoutLines(script.name) } returns 3
            coEvery { countAyahTexts(textSource) } returns 0
        }
        seeder(dao, storedVersion = MushafLayoutSeeder.CONTENT_VERSION).seedIfNeeded(script)
        coVerify(exactly = 1) { dao.replaceMushafLayout(script.name, textSource, any(), any()) }
    }

    @Test
    fun secondCallSkipsTheDbCheckEntirely_onceConfirmedCurrent() = runTest {
        // getMushafPageLayout calls seedIfNeeded() on every page fetch (#280 review) — once a
        // process has confirmed the data is current, later calls must not retake the mutex for
        // another count/version read.
        val dao = populatedDao(layoutRows = 3)
        val instance = seeder(dao, storedVersion = MushafLayoutSeeder.CONTENT_VERSION)

        instance.seedIfNeeded(script)
        instance.seedIfNeeded(script)
        instance.seedIfNeeded(script)

        coVerify(exactly = 1) { dao.countLayoutLines(script.name) }
        coVerify(exactly = 0) { dao.replaceMushafLayout(any(), any(), any(), any()) }
    }

    @Test
    fun ayahFlowEditionsAreNeverSeeded() = runTest {
        // MADANI paginates from the ayahs table and has no stored layout at all, so asking to
        // seed it must not touch the DB — not even to count.
        val dao = mockk<QuranDao>(relaxed = true)
        seeder(dao, storedVersion = 0).seedIfNeeded(MushafScript.MADANI)
        coVerify(exactly = 0) { dao.countLayoutLines(any()) }
        coVerify(exactly = 0) { dao.replaceMushafLayout(any(), any(), any(), any()) }
    }

    @Test
    fun rejectsATextAssetWithTheWrongAyahCount() = runTest {
        // A truncated or mis-generated asset must fail loudly rather than seed a partial Quran.
        val dao = populatedDao(layoutRows = 0)
        val thrown = runCatching {
            seeder(dao, storedVersion = 0, text = textJson).seedIfNeeded(script)
        }.exceptionOrNull()
        assertThat(thrown).isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) { dao.replaceMushafLayout(any(), any(), any(), any()) }
    }
}
