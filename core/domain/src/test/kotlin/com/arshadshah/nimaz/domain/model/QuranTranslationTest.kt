package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Guards the translation catalogue's contract with the data that ships beside it.
 *
 * The ids here are written into `translations.translator_id` and into the user's
 * `quran_translator_id` preference, so they are effectively permanent — a rename strands
 * every user who had that translation selected.
 *
 * These ids are now also the split key for the `tr.<id>` collections in
 * arshad-shah/nimaz-data, so a rename here silently stops matching the rows the artifact
 * ships. That the 15 ids exist on both sides is asserted where the data lives (`nz import
 * --check` plus each collection's row floor); what stays here is the half only the app can
 * state — stability, uniqueness and the fallback behaviour.
 */
class QuranTranslationTest {

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
