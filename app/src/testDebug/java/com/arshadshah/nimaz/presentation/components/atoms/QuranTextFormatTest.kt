package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranTextFormatTest {

    private fun ayah(
        surahNumber: Int,
        ayahNumber: Int,
        textArabic: String,
    ): Ayah = Ayah(
        id = 1,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        textArabic = textArabic,
        textSimple = textArabic,
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
    )

    @Test
    fun `formatAyahEndMarker wraps arabic number in ornamental brackets`() {
        assertThat(formatAyahEndMarker(2)).isEqualTo("﴿٢﴾")
    }

    @Test
    fun `formatAyahWithEndMarker appends marker after a space`() {
        val result = formatAyahWithEndMarker("نص", 3)
        assertThat(result).isEqualTo("نص ﴿٣﴾")
    }

    @Test
    fun `annotatedAyahEndMarker pins every span to Amiri so the number never inherits a font without the bracket glyphs`() {
        // Regression guard for the IndoPak (Nastaʿlīq) font on the Madani layout:
        // the ornamental brackets ﴿﴾ only exist in the naskh fonts, so the marker
        // must render in its own font regardless of the selected verse font.
        val marker = annotatedAyahEndMarker(
            ayahNumber = 5,
            bracketColor = Color.Yellow,
            numberColor = Color.Cyan,
        )
        assertThat(marker.text).isEqualTo("﴿٥﴾")
        assertThat(marker.spanStyles).isNotEmpty()
        assertThat(marker.spanStyles.all { it.item.fontFamily == AmiriFontFamily }).isTrue()
    }

    @Test
    fun `getDisplayArabicText strips bismillah from first ayah of normal surah`() {
        val a = ayah(surahNumber = 2, ayahNumber = 1, textArabic = "$BISMILLAH_TEXT الم")
        assertThat(a.getDisplayArabicText()).isEqualTo("الم")
    }

    @Test
    fun `getDisplayArabicText keeps bismillah for Al-Fatiha`() {
        val a = ayah(surahNumber = 1, ayahNumber = 1, textArabic = BISMILLAH_TEXT)
        assertThat(a.getDisplayArabicText()).isEqualTo(BISMILLAH_TEXT)
    }

    @Test
    fun `getDisplayArabicText keeps text for At-Tawbah`() {
        val text = "براءة من الله"
        val a = ayah(surahNumber = 9, ayahNumber = 1, textArabic = text)
        assertThat(a.getDisplayArabicText()).isEqualTo(text)
    }

    @Test
    fun `getDisplayArabicText leaves non-first ayahs untouched`() {
        val text = "$BISMILLAH_TEXT شيء"
        val a = ayah(surahNumber = 2, ayahNumber = 5, textArabic = text)
        assertThat(a.getDisplayArabicText()).isEqualTo(text)
    }
}
