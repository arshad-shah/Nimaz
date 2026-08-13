package com.arshadshah.nimaz.domain.model

/**
 * How an ayah is named, everywhere.
 *
 * The section used to name the same verse three ways — "Al-Fatiha · Verse 2" in
 * favourites, "Surah 1, Ayah 3" in bookmarks (dropping the name), and
 * "Surah 2:45 / Al-Baqara" in search. One type, one format.
 *
 * The chosen form is the conventional citation: the surah's name followed by
 * `surah:ayah`. The words "Surah" and "Verse" are dropped — they are noise in a
 * list where every row is a verse.
 */
data class AyahReference(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String? = null,
) {
    fun format(): String {
        val numbers = "$surahNumber:$ayahNumber"
        val name = surahName?.trim().orEmpty()
        return if (name.isEmpty()) numbers else "$name $numbers"
    }
}
