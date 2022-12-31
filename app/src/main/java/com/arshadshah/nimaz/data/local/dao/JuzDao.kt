package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.LocalJuz


@Dao
interface JuzDao {
    //get all the juz
    @Query("SELECT * FROM Juz")
    suspend fun getAllJuz() : List<LocalJuz>

    //insert all the juz
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(juz: List<LocalJuz>)

    //count the number of juz
    @Query("SELECT COUNT(*) FROM Juz")
    suspend fun count() : Int
}