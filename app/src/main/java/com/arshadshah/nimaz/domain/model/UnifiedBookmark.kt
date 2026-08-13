package com.arshadshah.nimaz.domain.model

import com.arshadshah.nimaz.core.navigation.Route

/**
 * A Quran, Hadith or Dua bookmark under one type.
 *
 * Domain rather than presentation: it unifies three content kinds and carries the navigation
 * target each resolves to, which is a fact about the content, not about how a screen draws it.
 * It lived inside `BookmarksViewModel.kt`.
 */
data class UnifiedBookmark(
    val id: String,
    val type: BookmarkType,
    /**
     * How the user marked this — bookmarked, favourited, annotated, or several at once.
     *
     * A set rather than a single value because the store says so: `bookmarks` carries
     * `bookmarked` and `favourite` as two flags on one row (a verse can be both, and used to be
     * a row in each of two tables), and a non-null note is a third way of having marked
     * something. Collapsing that to one value would have to pick a winner and lose the rest.
     */
    val kinds: Set<SavedKind> = setOf(SavedKind.BOOKMARK),
    val title: String,
    val subtitle: String,
    val arabicText: String?,
    val createdAt: Long,
    val note: String?,
    val color: String?,
    // Navigation data
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null,
    val hadithBookId: String? = null,
    val hadithNumber: Int? = null,
    val duaId: String? = null,
    val categoryId: String? = null
)

enum class BookmarkType(val prefix: String) {
    QURAN("quran_"),
    HADITH("hadith_"),
    DUA("dua_");

    /** The unified id for a bookmark of this type. */
    fun idFor(rawId: Any): String = "$prefix$rawId"

    companion object {
        /**
         * The type a unified id belongs to, or null.
         *
         * The three prefixes used to be written out at nine separate call sites, split
         * between construction and parsing — and a typo in one of them was a silent
         * no-op rather than a compile error.
         */
        fun ofId(id: String): BookmarkType? = entries.firstOrNull { id.startsWith(it.prefix) }
    }
}

/**
 * The three ways a user marks something, which is the axis the Saved screen filters on
 * alongside the content type.
 *
 * Favouriting exists for the Qur'an alone — the other two corpora only bookmark — so a
 * Favourites filter over everything is a filter that quietly means "verses".
 */
enum class SavedKind { BOOKMARK, FAVOURITE, NOTE }
