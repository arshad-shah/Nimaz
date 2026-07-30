package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbihDao {
    // Preset operations
    @Query("SELECT * FROM tasbih_presets ORDER BY display_order ASC")
    fun getAllPresets(): Flow<List<TasbihPresetEntity>>

    @Query("SELECT * FROM tasbih_presets WHERE is_custom = 0 ORDER BY display_order ASC")
    fun getDefaultPresets(): Flow<List<TasbihPresetEntity>>

    @Query("SELECT * FROM tasbih_presets WHERE is_custom = 1 ORDER BY display_order ASC")
    fun getCustomPresets(): Flow<List<TasbihPresetEntity>>

    @Query("SELECT * FROM tasbih_presets WHERE id = :id")
    suspend fun getPresetById(id: Long): TasbihPresetEntity?

    @Query("SELECT name FROM tasbih_presets")
    suspend fun getAllPresetNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: TasbihPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<TasbihPresetEntity>)

    @Update
    suspend fun updatePreset(preset: TasbihPresetEntity)

    @Delete
    suspend fun deletePreset(preset: TasbihPresetEntity)

    @Query("DELETE FROM tasbih_presets WHERE id = :id AND is_custom = 1")
    suspend fun deleteCustomPreset(id: Long)

    // Session operations












    @Query("SELECT * FROM tasbih_presets ORDER BY display_order ASC")
    suspend fun getAllPresetsSync(): List<TasbihPresetEntity>



    @Query("DELETE FROM tasbih_presets WHERE is_custom = 1")
    suspend fun deleteCustomPresets()

    /** The presets this person made. Their sessions live in the user database. */
    @Transaction
    suspend fun deleteCustomUserPresets() {
        deleteCustomPresets()
    }
}
