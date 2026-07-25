package com.arshadshah.nimaz.presentation.screens.quran

import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.MushafLine
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.MushafWord
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [buildOrderedPageAyahsFromLayout] — the fix for the #280 review finding
 * where the reader's `AudioBottomBar` resolved the current page's ayahs from the
 * Madani-keyed `pageCache` even while the IndoPak 16-line layout (an unrelated pagination
 * scheme) was on screen, showing/playing the wrong ayah.
 */
class QuranReaderScreenTest {

    private fun word(ayahId: Int, ayahNumber: Int, position: Int, text: String = "w") =
        MushafWord(text = text, ayahId = ayahId, ayahNumber = ayahNumber, position = position)

    @Test
    fun `returns distinct ayahs in printed order, skipping header and basmalah lines`() {
        val layout = MushafPageLayout(
            page = 1,
            lines = listOf(
                MushafLine(page = 1, lineNumber = 1, type = MushafLineType.SURAH_HEADER, surahId = 1),
                MushafLine(page = 1, lineNumber = 2, type = MushafLineType.BASMALAH, surahId = 1),
                MushafLine(
                    page = 1, lineNumber = 3, type = MushafLineType.AYAH, surahId = 1,
                    words = listOf(word(ayahId = 1, ayahNumber = 1, position = 1))
                ),
                MushafLine(
                    page = 1, lineNumber = 4, type = MushafLineType.AYAH, surahId = 1,
                    words = listOf(
                        word(ayahId = 1, ayahNumber = 1, position = 2),
                        word(ayahId = 2, ayahNumber = 2, position = 1)
                    )
                )
            )
        )

        val ayahs = buildOrderedPageAyahsFromLayout(layout, ayahById = emptyMap())

        assertThat(ayahs.map { it.id }).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `prefers full ayah content from ayahById over the layout-derived reconstruction`() {
        val layout = MushafPageLayout(
            page = 5,
            lines = listOf(
                MushafLine(
                    page = 5, lineNumber = 1, type = MushafLineType.AYAH, surahId = 2,
                    words = listOf(word(ayahId = 10, ayahNumber = 3, position = 1))
                )
            )
        )
        val fullAyah = Ayah(
            id = 10,
            surahNumber = 2,
            ayahNumber = 3,
            textArabic = "full text",
            textSimple = "full text",
            juzNumber = 1,
            hizbNumber = 1,
            rubNumber = 1,
            pageNumber = 5,
            sajdaType = null,
            sajdaNumber = null,
        )

        val ayahs = buildOrderedPageAyahsFromLayout(layout, ayahById = mapOf(10 to fullAyah))

        assertThat(ayahs).containsExactly(fullAyah)
    }

    @Test
    fun `reconstructs a minimal ayah when the id is not yet cached`() {
        val layout = MushafPageLayout(
            page = 5,
            lines = listOf(
                MushafLine(
                    page = 5, lineNumber = 1, type = MushafLineType.AYAH, surahId = 2,
                    words = listOf(word(ayahId = 10, ayahNumber = 3, position = 1))
                )
            )
        )

        val ayahs = buildOrderedPageAyahsFromLayout(layout, ayahById = emptyMap())

        assertThat(ayahs).hasSize(1)
        val ayah = ayahs.single()
        assertThat(ayah.id).isEqualTo(10)
        assertThat(ayah.surahNumber).isEqualTo(2)
        assertThat(ayah.ayahNumber).isEqualTo(3)
        assertThat(ayah.pageNumber).isEqualTo(5)
    }

    @Test
    fun `empty layout yields no ayahs`() {
        val layout = MushafPageLayout(page = 1, lines = emptyList())

        assertThat(buildOrderedPageAyahsFromLayout(layout, ayahById = emptyMap())).isEmpty()
    }
}
