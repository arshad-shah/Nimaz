package com.arshadshah.nimaz.data.local.user

import androidx.room.Dao
import androidx.room.Transaction
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Bookmarks and favourites, for every kind of thing at once.
 *
 * The seven tables this replaces had seven DAOs' worth of near-identical methods —
 * `getAllBookmarks`, `getBookmarkByAyahId`, `isBookmarked`, `toggleFavorite` repeated per
 * kind, each with its own spelling. One table means one set, with `kind` as an argument
 * where there used to be a copy of the method.
 */
@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE kind = :kind AND bookmarked = 1 ORDER BY created_at DESC")
    fun bookmarks(kind: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE kind = :kind AND favourite = 1 ORDER BY created_at DESC")
    fun favourites(kind: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE kind = :kind AND target_id = :targetId")
    suspend fun find(kind: String, targetId: Int): BookmarkEntity?

    /** Marks within one context — a hadith book, a dua category, a surah. */
    @Query(
        """
        SELECT * FROM bookmarks
        WHERE kind = :kind AND context_id = :contextId AND bookmarked = 1
        ORDER BY created_at DESC
        """
    )
    fun inContext(kind: String, contextId: Int): Flow<List<BookmarkEntity>>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM bookmarks
            WHERE kind = :kind AND target_id = :targetId AND bookmarked = 1
        )
        """
    )
    fun observeIsBookmarked(kind: String, targetId: Int): Flow<Boolean>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM bookmarks
            WHERE kind = :kind AND target_id = :targetId AND favourite = 1
        )
        """
    )
    fun observeIsFavourite(kind: String, targetId: Int): Flow<Boolean>

    /** Ids only, for the "is this bookmarked" pass over a page of content. */
    @Query("SELECT target_id FROM bookmarks WHERE kind = :kind AND bookmarked = 1")
    suspend fun bookmarkedIds(kind: String): List<Int>

    @Query("SELECT target_id FROM bookmarks WHERE kind = :kind AND favourite = 1")
    suspend fun favouriteIds(kind: String): List<Int>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE kind = :kind AND bookmarked = 1")
    fun bookmarkCount(kind: String): Flow<Int>

    @Upsert
    suspend fun upsert(bookmark: BookmarkEntity)

    @Upsert
    suspend fun upsertAll(bookmarks: List<BookmarkEntity>)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE kind = :kind AND target_id = :targetId")
    suspend fun delete(kind: String, targetId: Int)

    /**
     * Clears one flag and removes the row only when neither is left, so un-favouriting a
     * verse you also bookmarked does not delete the bookmark. The old pair of tables got
     * this right by accident, by being two tables; here it has to be said.
     */
    @Query(
        """
        UPDATE bookmarks SET favourite = 0, updated_at = :now
        WHERE kind = :kind AND target_id = :targetId
        """
    )
    suspend fun clearFavourite(kind: String, targetId: Int, now: Long)

    @Query(
        """
        UPDATE bookmarks SET bookmarked = 0, updated_at = :now
        WHERE kind = :kind AND target_id = :targetId
        """
    )
    suspend fun clearBookmark(kind: String, targetId: Int, now: Long)

    @Query("DELETE FROM bookmarks WHERE bookmarked = 0 AND favourite = 0")
    suspend fun pruneEmpty()

    @Query("SELECT * FROM bookmarks")
    suspend fun all(): List<BookmarkEntity>

    @Query("DELETE FROM bookmarks")
    suspend fun clear()
}

/** Per-item progress: dua repetitions, qaida lessons and cells. */
@Dao
interface ProgressDao {

    @Query("SELECT * FROM progress WHERE kind = :kind ORDER BY updated_at DESC")
    fun ofKind(kind: String): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE kind = :kind AND target_id = :targetId AND date = :date")
    suspend fun find(kind: String, targetId: Int, date: Long = 0): ProgressEntity?

    @Query("SELECT * FROM progress WHERE kind = :kind AND context_id = :contextId")
    fun inContext(kind: String, contextId: Int): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE kind = :kind AND date = :date ORDER BY target_id ASC")
    fun onDate(kind: String, date: Long): Flow<List<ProgressEntity>>

    @Query("SELECT COUNT(*) FROM progress WHERE kind = :kind AND is_completed = 1")
    fun completedCount(kind: String): Flow<Int>

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Upsert
    suspend fun upsertAll(progress: List<ProgressEntity>)

    @Query("DELETE FROM progress WHERE kind = :kind AND target_id = :targetId AND date = :date")
    suspend fun delete(kind: String, targetId: Int, date: Long = 0)

    @Query("DELETE FROM progress WHERE kind = :kind")
    suspend fun deleteKind(kind: String)

    @Query("SELECT * FROM progress")
    suspend fun all(): List<ProgressEntity>

    @Query("DELETE FROM progress")
    suspend fun clear()

    /**
     * A dua counted one more time today, creating the row if this is the first.
     *
     * `dua_progress` had a pair of these as `@Transaction` helpers on the content DAO, which
     * is where they stopped making sense once the counting moved: they read and wrote the
     * user's rows through a DAO bound to the content database.
     */
    @Transaction
    suspend fun increment(kind: String, targetId: Int, date: Long, target: Int?) {
        val now = System.currentTimeMillis()
        val existing = find(kind, targetId, date)
        val completed = (existing?.completed ?: 0) + 1
        upsert(
            ProgressEntity(
                kind = kind,
                targetId = targetId,
                date = date,
                contextId = existing?.contextId,
                completed = completed,
                total = target ?: existing?.total,
                isCompleted = (target ?: existing?.total)?.let { completed >= it } ?: false,
                state = existing?.state,
                score = existing?.score,
                resumeId = existing?.resumeId,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        )
    }

    /** One fewer, never below zero, and the row is left in place at zero. */
    @Transaction
    suspend fun decrement(kind: String, targetId: Int, date: Long) {
        val existing = find(kind, targetId, date) ?: return
        if (existing.completed <= 0) return
        val completed = existing.completed - 1
        upsert(
            existing.copy(
                completed = completed,
                isCompleted = existing.total?.let { completed >= it } ?: false,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }
}
