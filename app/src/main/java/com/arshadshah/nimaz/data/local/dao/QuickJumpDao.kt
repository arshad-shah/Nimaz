package com.arshadshah.nimaz.data.local.dao

import androidx.room.*
import com.arshadshah.nimaz.data.local.models.QuickJump

@Dao
interface QuickJumpDao {

    @Query("SELECT * FROM QuickJump ORDER BY createdAt DESC")
    suspend fun getAllQuickJumps(): List<QuickJump>

    @Query("SELECT * FROM QuickJump WHERE surahNumber = :surahNumber ORDER BY ayaNumberInSurah")
    suspend fun getQuickJumpsForSurah(surahNumber: Int): List<QuickJump>

    @Insert
    suspend fun insertQuickJump(quickJump: QuickJump)

    @Update
    suspend fun updateQuickJump(quickJump: QuickJump)

    @Delete
    suspend fun deleteQuickJump(quickJump: QuickJump)

    @Query("DELETE FROM QuickJump WHERE id = :id")
    suspend fun deleteQuickJumpById(id: Long)
}