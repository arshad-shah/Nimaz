package com.arshadshah.nimaz.data.local.user

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** The counting presets this person made. The shipped ones are content and live elsewhere. */
@Dao
interface CustomPresetDao {

    @Query("SELECT * FROM custom_tasbih_presets ORDER BY display_order ASC, id ASC")
    fun observe(): Flow<List<CustomTasbihPresetEntity>>

    @Query("SELECT * FROM custom_tasbih_presets ORDER BY display_order ASC, id ASC")
    suspend fun all(): List<CustomTasbihPresetEntity>

    @Query("SELECT * FROM custom_tasbih_presets WHERE id = :id")
    suspend fun find(id: Long): CustomTasbihPresetEntity?

    @Upsert
    suspend fun upsert(preset: CustomTasbihPresetEntity): Long

    @Upsert
    suspend fun upsertAll(presets: List<CustomTasbihPresetEntity>)

    @Delete
    suspend fun delete(preset: CustomTasbihPresetEntity)

    @Query("DELETE FROM custom_tasbih_presets WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM custom_tasbih_presets")
    suspend fun clear()
}
