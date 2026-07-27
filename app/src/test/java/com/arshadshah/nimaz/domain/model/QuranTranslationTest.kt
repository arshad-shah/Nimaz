package com.arshadshah.nimaz.domain.model

import com.arshadshah.nimaz.data.local.quran.QuranTranslationSeeder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Guards the translation catalogue's contract with the data that ships beside it.
 *
 * The ids here are written into `translations.translator_id` and into the user's
 * `quran_translator_id` preference, so they are effectively permanent — a rename strands
 * every user who had that translation selected. These tests make the catalogue, the shipped
 * assets and that stability requirement check each other.
 */
class QuranTranslationTest {

    @Test
    fun `every catalogue entry has a bundled asset declaring its own id`() {
        // Catches a catalogue entry added without running the generator, and an asset filed
        // under the wrong id. The full parse/verse-count checks live in the seeder tests; this
        // reads only the header so it stays cheap across all 15 assets.
        QuranTranslation.entries.forEach { translation ->
            val file = File("src/main/assets/${QuranTranslationSeeder.assetPath(translation)}")
            assertThat(file.exists()).isTrue()
            val header = CharArray(120).let { buf ->
                val n = file.bufferedReader().use { it.read(buf) }
                String(buf, 0, n.coerceAtLeast(0))
            }
            assertThat(header).contains("\"translationId\":\"${translation.id}\"")
        }
    }

    @Test
    fun `ids are unique and stable-looking`() {
        val ids = QuranTranslation.entries.map { it.id }
        assertThat(ids).containsNoDuplicates()
        // snake_case, no dots — dots were the upstream edition format and are not our ids.
        ids.forEach { assertThat(it).matches("[a-z0-9_]+") }
    }

    @Test
    fun `the historical default keeps its legacy id`() {
        // Every existing install has this exact string persisted; changing it would silently
        // move those users onto a translation that no longer resolves.
        assertThat(QuranTranslation.DEFAULT.id).isEqualTo("sahih_international")
        assertThat(QuranTranslation.SAHIH_INTERNATIONAL.id).isEqualTo("sahih_international")
    }

    @Test
    fun `fromId falls back to the default for unknown and null values`() {
        assertThat(QuranTranslation.fromId(null)).isEqualTo(QuranTranslation.DEFAULT)
        assertThat(QuranTranslation.fromId("")).isEqualTo(QuranTranslation.DEFAULT)
        assertThat(QuranTranslation.fromId("removed_translation"))
            .isEqualTo(QuranTranslation.DEFAULT)
        assertThat(QuranTranslation.fromId("ur_maududi")).isEqualTo(QuranTranslation.UR_MAUDUDI)
    }

    @Test
    fun `byLanguage covers every translation exactly once`() {
        val grouped = QuranTranslation.byLanguage()
        assertThat(grouped.values.flatten()).containsExactlyElementsIn(QuranTranslation.entries)
    }

    @Test
    fun `right-to-left languages are flagged so the reader can lay them out`() {
        // Urdu ships in the catalogue; if isRtl were wrong the translation would render
        // left-aligned with its punctuation on the wrong side.
        assertThat(QuranTranslation.UR_MAUDUDI.isRtl).isTrue()
        assertThat(QuranTranslation.SAHIH_INTERNATIONAL.isRtl).isFalse()
    }
}
