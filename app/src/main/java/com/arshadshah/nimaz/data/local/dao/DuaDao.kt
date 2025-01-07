package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.data.local.models.LocalDua

@Dao
interface DuaDao {

    //get duas of a chapter by chapter id
    @Query("SELECT * FROM Dua WHERE chapter_id = :chapterId")
    suspend fun getDuasOfChapter(chapterId: Int): List<LocalDua>

    //save a list of chapters
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChapters(chapters: List<LocalChapter>)

    //save a one chapter
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDuas(duas: List<LocalDua>)

    //count
    @Query("SELECT COUNT(*) FROM Chapter")
    suspend fun countChapters(): Int

    //count
    @Query("SELECT COUNT(*) FROM Dua")
    suspend fun countDuas(): Int

    // get all chanpters by category id
    @Query("SELECT * FROM Chapter WHERE category_id = :categoryId")
    suspend fun getChaptersByCategory(categoryId: Int): List<LocalChapter>
}