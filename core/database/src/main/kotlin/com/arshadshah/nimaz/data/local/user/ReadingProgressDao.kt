package com.arshadshah.nimaz.data.local.user

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Where the reader was. One row, and deliberately still its own table.
 *
 * It was not folded into [ProgressEntity] with the per-item counters: six typed fields read
 * on every app open to answer "where was I" is a different shape and a hotter path than
 * counting repetitions, and flattening it into a generic counter or a JSON blob would make
 * the most-read row in the app the least legible.
 */
@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress WHERE id = 1")
    fun observe(): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE id = 1")
    suspend fun get(): ReadingProgressEntity?

    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress")
    suspend fun clear()
}
