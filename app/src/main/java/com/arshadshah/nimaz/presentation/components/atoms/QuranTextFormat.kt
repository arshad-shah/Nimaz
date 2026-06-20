package com.arshadshah.nimaz.presentation.components.atoms

import com.arshadshah.nimaz.domain.model.Ayah

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
 * Strips the leading bismillah from the first ayah of a surah, except where the
 * bismillah is genuinely part of the text:
 * - Surah 1 (Al-Fatiha) — the bismillah IS ayah 1.
 * - Surah 9 (At-Tawbah) — has no bismillah.
 */
fun Ayah.getDisplayArabicText(): String {
    return if (numberInSurah == 1 && surahNumber != 1 && surahNumber != 9) {
        textArabic
            .removePrefix("$BISMILLAH_TEXT ")
            .removePrefix(BISMILLAH_TEXT)
            .trim()
    } else {
        textArabic
    }
}
