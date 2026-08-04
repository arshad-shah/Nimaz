package com.arshadshah.nimaz.presentation.components.molecules

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Where the line-accurate reader puts a rukūʿ or sajda sign.
 *
 * The sign follows the verse it belongs to, so the renderer has to know which word ends the
 * verse. It cannot use "the last word of this ayah on this page" — an ayah can run onto the
 * next page, and the sign would then be drawn twice, or in the middle of the verse. The
 * anchor is the verse's own end glyph, which the IndoPak text closes every ayah with.
 *
 * These cases are the glyphs taken from the shipped text: `۟` alone mid-surah, and `۟۠` with
 * the ornament that follows a surah's final verse (1:7).
 */
class MushafVerseEndGlyphTest {

    @Test
    fun `the verse-end glyph is recognised on its own`() {
        // Al-Fātiḥah 1:1 — "…ٱلرَّحِیْمِ ۟"
        assertThat(endsVerse("۟")).isTrue()
    }

    @Test
    fun `the surah-closing form is recognised too`() {
        // Al-Fātiḥah 1:7 ends "۟۠" — the same zero plus the end-of-surah ornament. This is
        // the verse that now carries Al-Fātiḥah's rukūʿ sign, so missing it would put the
        // whole fix back where it started.
        assertThat(endsVerse("۟۠")).isTrue()
        assertThat(endsVerse("۠")).isTrue()
    }

    @Test
    fun `an ordinary word does not end a verse`() {
        assertThat(endsVerse("ٱلْحَمْدُ")).isFalse()
        assertThat(endsVerse("لِلَّهِ")).isFalse()
        assertThat(endsVerse("")).isFalse()
    }

    @Test
    fun `a word carrying the glyph inline still ends the verse`() {
        // Some layouts attach the glyph to the final word rather than splitting it out.
        assertThat(endsVerse("ٱلضَّآلِّینَ۟")).isTrue()
    }

    /** The renderer's own rule, not a copy of it. */
    private fun endsVerse(word: String): Boolean = word.endsOfVerse()
}
