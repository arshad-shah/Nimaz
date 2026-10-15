package com.arshadshah.nimaz.domain.search

/**
 * Whether this character is written in the Arabic script — Arabic, Urdu, Persian alike.
 *
 * Search uses it to decide which text a result is shown against (an Arabic query matched the
 * Arabic matn, not the English translation) and the screen uses it to pick a face, because the
 * app's Latin fonts carry no Arabic-script glyphs at all.
 */
fun Char.isArabicScript(): Boolean =
    Character.UnicodeScript.of(code) == Character.UnicodeScript.ARABIC

/**
 * Whether [text] carries letters only Urdu (or Persian) uses — ے ں ٹ ڈ ڑ ہ ھ ی ک گ پ چ ژ —
 * which is how a translation snippet in Urdu is told from Arabic when nothing else says which
 * it is. Arabic Quran and hadith text never contains them.
 */
fun looksLikeUrdu(text: String): Boolean = text.any { it in URDU_LETTERS }

private const val URDU_LETTERS = "ےںٹڈڑہھیکگپچژۓۃ"
