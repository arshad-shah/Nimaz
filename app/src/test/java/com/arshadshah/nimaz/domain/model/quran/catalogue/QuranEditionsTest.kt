package com.arshadshah.nimaz.domain.model.quran.catalogue

import com.arshadshah.nimaz.domain.model.MushafLineType
import com.arshadshah.nimaz.domain.model.ReadingProgressCalculator
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Pins the content registry's resolution contract — the part an upgrade can silently break.
 *
 * The stakes: layout and translator ids are persisted in DataStore, and values written before
 * the registry existed were `MushafScript` enum *names*. If resolution stopped honouring
 * those, every existing user's reader would quietly reset to the default edition on update.
 * ADR-003 says resolve the stored value rather than migrate it; these tests hold that.
 */
class QuranEditionsTest {

    @Test
    fun `page counts match each shipped edition`() {
        assertThat(QuranEditions.layout("madani").totalPages).isEqualTo(604)
        assertThat(QuranEditions.layout("indopak16").totalPages).isEqualTo(548)
    }

    @Test
    fun `legacy total pages stays in sync with the default layout`() {
        assertThat(ReadingProgressCalculator.TOTAL_QURAN_PAGES)
            .isEqualTo(QuranEditions.defaultLayout.totalPages)
    }

    @Test
    fun `layout ids resolve to their edition`() {
        assertThat(QuranEditions.layout("madani").id).isEqualTo("madani")
        assertThat(QuranEditions.layout("indopak16").id).isEqualTo("indopak16")
    }

    @Test
    fun `pre-registry MushafScript enum names still resolve to the same edition`() {
        // The exact strings PreferencesDataStore persisted before the registry landed.
        assertThat(QuranEditions.layout("MADANI").id).isEqualTo("madani")
        assertThat(QuranEditions.layout("INDOPAK_16").id).isEqualTo("indopak16")
    }

    @Test
    fun `unknown and absent layout values fall back to the default`() {
        // Null / blank / unknown must never break the reader.
        assertThat(QuranEditions.layout(null)).isEqualTo(QuranEditions.defaultLayout)
        assertThat(QuranEditions.layout("")).isEqualTo(QuranEditions.defaultLayout)
        assertThat(QuranEditions.layout("GALAXY")).isEqualTo(QuranEditions.defaultLayout)
    }

    @Test
    fun `max total pages is the largest edition`() {
        assertThat(QuranEditions.maxTotalPages).isEqualTo(604)
    }

    @Test
    fun `flowed and line-accurate editions are distinguishable`() {
        // The reader picks its renderer and its page-range source off this flag, so it has to
        // track linesPerPage exactly.
        assertThat(QuranEditions.layout("madani").hasLineLayout).isFalse()
        assertThat(QuranEditions.layout("indopak16").hasLineLayout).isTrue()
        assertThat(QuranEditions.layout("indopak16").linesPerPage).isEqualTo(16)
    }

    @Test
    fun `tajweed support tracks the ayah text source`() {
        // Per-letter tajweed spans only exist for the Uthmani text.
        QuranEditions.mushafLayouts.forEach { layout ->
            if (layout.supportsTajweed) {
                assertThat(layout.textSource).isEqualTo(AyahTextSource.UTHMANI)
            }
        }
    }

    @Test
    fun `translation ids resolve and fall back to the default`() {
        assertThat(QuranEditions.translation("sahih_international").id)
            .isEqualTo("sahih_international")
        assertThat(QuranEditions.translation(null)).isEqualTo(QuranEditions.defaultTranslation)
        assertThat(QuranEditions.translation("no_such_edition"))
            .isEqualTo(QuranEditions.defaultTranslation)
    }

    @Test
    fun `tafseer ids resolve and fall back to the default`() {
        assertThat(QuranEditions.tafseer("ibn_kathir_en").id).isEqualTo("ibn_kathir_en")
        assertThat(QuranEditions.tafseer("maariful_quran_en").id).isEqualTo("maariful_quran_en")
        assertThat(QuranEditions.tafseer("nope")).isEqualTo(QuranEditions.defaultTafseer)
    }

    @Test
    fun `reciter aliases from the old audio layer still resolve`() {
        // QuranAudioManager accepted both spellings for these two reciters; a user who had
        // one persisted must keep the reciter they chose.
        assertThat(QuranEditions.reciter("alafasy").id).isEqualTo("mishary")
        assertThat(QuranEditions.reciter("muaiqly").id).isEqualTo("maher")
        assertThat(QuranEditions.reciter("mishary").id).isEqualTo("mishary")
        assertThat(QuranEditions.reciter(null)).isEqualTo(QuranEditions.defaultReciter)
    }

    @Test
    fun `arabic font ids resolve and fall back to the default`() {
        assertThat(QuranEditions.arabicFont("indopak").id).isEqualTo("indopak")
        assertThat(QuranEditions.arabicFont("nope")).isEqualTo(QuranEditions.defaultArabicFont)
    }

    @Test
    fun `ids are unique within every axis`() {
        // A duplicate id would make resolution order-dependent and silently shadow an edition.
        listOf(
            "translations" to QuranEditions.translations,
            "mushafLayouts" to QuranEditions.mushafLayouts,
            "tafseers" to QuranEditions.tafseers,
            "reciters" to QuranEditions.reciters,
            "arabicFonts" to QuranEditions.arabicFonts
        ).forEach { (axis, editions) ->
            val ids = editions.map { it.id }
            assertWithMessage("$axis ids").that(ids).containsNoDuplicates()
        }
    }

    @Test
    fun `no legacy key collides with a real id on the same axis`() {
        // An id always wins over an alias, so a collision would make an edition unreachable.
        listOf(
            QuranEditions.translations,
            QuranEditions.mushafLayouts,
            QuranEditions.tafseers,
            QuranEditions.reciters,
            QuranEditions.arabicFonts
        ).forEach { editions ->
            val ids = editions.map { it.id }.toSet()
            editions.forEach { edition ->
                edition.legacyKeys.forEach { key ->
                    assertThat(ids).doesNotContain(key)
                }
            }
        }
    }

    @Test
    fun `every layout declares a font that the catalogue ships`() {
        val fontIds = QuranEditions.arabicFonts.map { it.id }.toSet()
        QuranEditions.mushafLayouts.forEach { layout ->
            assertThat(fontIds).contains(layout.fontId)
        }
        QuranEditions.translations.mapNotNull { it.fontId }.forEach { fontId ->
            assertThat(fontIds).contains(fontId)
        }
    }

    @Test
    fun `line type parsing is tolerant of casing and unknown values`() {
        assertThat(MushafLineType.fromString("surah_header")).isEqualTo(MushafLineType.SURAH_HEADER)
        assertThat(MushafLineType.fromString("BASMALAH")).isEqualTo(MushafLineType.BASMALAH)
        assertThat(MushafLineType.fromString("ayah")).isEqualTo(MushafLineType.AYAH)
        assertThat(MushafLineType.fromString("something-else")).isEqualTo(MushafLineType.AYAH)
    }
}
