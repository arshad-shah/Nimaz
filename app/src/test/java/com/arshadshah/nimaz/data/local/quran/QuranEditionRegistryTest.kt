package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.domain.model.quran.catalogue.QuranEditions
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Holds the three halves of the content registry in step (ADR-002).
 *
 * The registry is deliberately split across layers: [QuranEditions] holds pure metadata in
 * `domain`, [QuranContentAssets] holds asset paths and CDN ids in `data`, and
 * [QuranArabicFont] holds Compose `FontFamily`s in `presentation` — because domain may import
 * neither Android assets nor Compose. The cost of that split is that adding an edition means
 * touching two files, and forgetting the second one ships an edition that resolves in the
 * picker but has no bytes behind it.
 *
 * These tests are what makes the split safe: a half-added edition fails the build here rather
 * than crashing on a user's device when they select it.
 */
class QuranEditionRegistryTest {

    @Test
    fun `every line-accurate layout has an asset binding`() {
        val needsAssets = QuranEditions.mushafLayouts.filter { it.hasLineLayout }.map { it.id }
        assertThat(QuranContentAssets.mushafLayouts.keys).containsAtLeastElementsIn(needsAssets)
    }

    @Test
    fun `no asset binding exists for a flowed layout`() {
        // A flowed edition is paginated by the `ayahs.page` column that ships in the
        // prepopulated DB — an asset for it would never be read.
        val flowed = QuranEditions.mushafLayouts.filterNot { it.hasLineLayout }.map { it.id }
        flowed.forEach { assertThat(QuranContentAssets.mushafLayouts).doesNotContainKey(it) }
    }

    @Test
    fun `no asset binding refers to an unknown layout id`() {
        val known = QuranEditions.mushafLayouts.map { it.id }.toSet()
        assertThat(known).containsAtLeastElementsIn(QuranContentAssets.mushafLayouts.keys)
    }

    @Test
    fun `a layout that needs its own ayah text seeds it`() {
        // The word positions in a layout index into one specific ayah text column. If the
        // edition reads `text_indopak` — a column the prepopulated DB ships empty — the
        // binding has to carry the text asset too, or every page renders blank words.
        QuranEditions.mushafLayouts.filter { it.hasLineLayout }.forEach { layout ->
            val assets = QuranContentAssets.mushafLayouts.getValue(layout.id)
            if (layout.textSource.columnName == "text_indopak") {
                assertThat(assets.ayahText).isNotNull()
            }
        }
    }

    @Test
    fun `asset paths and content versions are well formed`() {
        val bindings = QuranContentAssets.mushafLayouts.values
            .flatMap { listOfNotNull(it.layout, it.ayahText) } +
            QuranContentAssets.translations.values
        bindings.forEach { binding ->
            assertThat(binding.assetPath).endsWith(".json")
            // 0 means "never seeded"; a shipped asset must be at least 1 or it never seeds.
            assertThat(binding.contentVersion).isAtLeast(1)
        }
    }

    @Test
    fun `every translation asset binding names a catalogue edition`() {
        val known = QuranEditions.translations.map { it.id }.toSet()
        assertThat(known).containsAtLeastElementsIn(QuranContentAssets.translations.keys)
    }

    @Test
    fun `every reciter has an audio CDN binding`() {
        // A reciter in the picker with no CDN entry would silently play the default reciter.
        val ids = QuranEditions.reciters.map { it.id }
        assertThat(QuranContentAssets.reciterAudio.keys).containsExactlyElementsIn(ids)
    }

    @Test
    fun `reciter audio resolves through legacy aliases`() {
        assertThat(QuranContentAssets.reciterAudioFor("alafasy").cdnId).isEqualTo("ar.alafasy")
        assertThat(QuranContentAssets.reciterAudioFor("muaiqly").cdnId)
            .isEqualTo("ar.mahermuaiqly")
        // Unknown ids must fall back to the default reciter rather than crash.
        assertThat(QuranContentAssets.reciterAudioFor("nobody"))
            .isEqualTo(QuranContentAssets.reciterAudio.getValue(QuranEditions.defaultReciter.id))
    }

    @Test
    fun `catalogue font ids and Compose font families are the same set`() {
        val catalogueIds = QuranEditions.arabicFonts.map { it.id }
        val composeIds = QuranArabicFont.entries.map { it.id }
        assertThat(composeIds).containsExactlyElementsIn(catalogueIds)
    }

    @Test
    fun `font display names come from the catalogue`() {
        QuranArabicFont.entries.forEach { font ->
            assertThat(font.displayName)
                .isEqualTo(QuranEditions.arabicFont(font.id).displayName)
        }
    }

    @Test
    fun `the default font matches the catalogue default`() {
        assertThat(QuranArabicFont.DEFAULT.id).isEqualTo(QuranEditions.defaultArabicFont.id)
    }
}
