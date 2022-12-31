package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.LocalAya


@Dao
interface AyaDao {

    //get all aya for type and number
    @Query("SELECT * FROM aya WHERE numberOfType = :number AND ayaType = :type")
    suspend fun getAll(number: Int, type: String): ArrayList<LocalAya>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aya: List<LocalAya>)

    @Query("SELECT COUNT(*) FROM aya")
    suspend fun count(): Int
}