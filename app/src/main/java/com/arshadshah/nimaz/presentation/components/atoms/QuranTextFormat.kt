package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily

/**
 * Shared, non-Compose helpers for formatting Quran Arabic text.
 *
 * These were previously copy-pasted (byte-for-byte) across [ArabicText],
 * `MushafContinuousText` and `QuranAyahItem`. Keep them here as the single
 * source of truth so the ayah end-marker and bismillah handling stay
 * consistent everywhere they are rendered.
 */

/** The Bismillah as stored in the ayah database (uses the wasla alif `ٱ`). */
const val BISMILLAH_TEXT = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

// Ornamental "end of ayah" brackets (Quranic typography).
private const val AYAH_END_OPEN = "﴿" // ﴿
private const val AYAH_END_CLOSE = "﴾" // ﴾

/** Wraps an ayah number in the ornamental end-of-ayah brackets, e.g. ﴿٢﴾. */
fun formatAyahEndMarker(ayahNumber: Int): String {
    return "$AYAH_END_OPEN${toArabicNumber(ayahNumber)}$AYAH_END_CLOSE"
}

/** Appends the ornamental ayah end-marker to the supplied Arabic text. */
fun formatAyahWithEndMarker(arabicText: String, ayahNumber: Int): String {
    return "$arabicText ${formatAyahEndMarker(ayahNumber)}"
}

/**
 * Appends the ornamental end-marker with the brackets and the number tinted
 * separately — gold brackets + a teal number, matching the Quran ornament
 * language. Use inside a [buildAnnotatedString] block.
 *
 * The marker is pinned to [markerFontFamily] (Amiri by default) rather than
 * inheriting the surrounding verse font: the ornamental brackets `﴿﴾`
 * (U+FD3F / U+FD3E) only exist in the naskh fonts. The IndoPak Nastaʿlīq font
 * has no glyphs for them — its ayah numbers live in its Private Use Area and
 * are only emitted by the 16-line IndoPak source text — so a Madani-layout
 * verse drawn in that font would otherwise render the end-marker as nothing
 * (the ayah number vanishes). Pinning a font that owns those glyphs keeps the
 * number visible for every font × layout combination.
 */
fun AnnotatedString.Builder.appendAyahEndMarker(
    ayahNumber: Int,
    bracketColor: Color,
    numberColor: Color,
    markerFontFamily: FontFamily = AmiriFontFamily,
) {
    withStyle(SpanStyle(color = bracketColor, fontFamily = markerFontFamily)) { append(AYAH_END_OPEN) }
    withStyle(SpanStyle(color = numberColor, fontFamily = markerFontFamily)) { append(toArabicNumber(ayahNumber)) }
    withStyle(SpanStyle(color = bracketColor, fontFamily = markerFontFamily)) { append(AYAH_END_CLOSE) }
}

/** The coloured end-marker as a standalone [AnnotatedString]. */
fun annotatedAyahEndMarker(
    ayahNumber: Int,
    bracketColor: Color,
    numberColor: Color,
    markerFontFamily: FontFamily = AmiriFontFamily,
): AnnotatedString = buildAnnotatedString {
    appendAyahEndMarker(ayahNumber, bracketColor, numberColor, markerFontFamily)
}

/**
 * True when this ayah carries a leading bismillah that the reader draws as a
 * separate header and so must strip from the verse body. That is ayah 1 of every
 * surah except:
 * - Surah 1 (Al-Fatiha) — the bismillah IS ayah 1.
 * - Surah 9 (At-Tawbah) — has no bismillah.
 */
val Ayah.hasLeadingBismillah: Boolean
    get() = numberInSurah == 1 && surahNumber != 1 && surahNumber != 9

/**
 * Strips the leading bismillah from the first ayah of a surah (see
 * [hasLeadingBismillah]), because the reader draws a separate bismillah header.
 */
fun Ayah.getDisplayArabicText(): String {
    return if (hasLeadingBismillah) {
        textArabic
            .removePrefix("$BISMILLAH_TEXT ")
            .removePrefix(BISMILLAH_TEXT)
            .trim()
    } else {
        textArabic
    }
}
