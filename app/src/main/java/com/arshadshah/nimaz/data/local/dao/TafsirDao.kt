package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.Tafsir

@Dao
interface TafsirDao {
    // Basic CRUD operations remain the same
    @Query("SELECT * FROM Tafsir WHERE id = :id")
    suspend fun getTafsirById(id: Long): Tafsir?

    @Query("SELECT * FROM Tafsir WHERE ayaNumberInQuran = :ayaNumber AND editionId = :editionId")
    suspend fun getTafsirForAya(ayaNumber: Int, editionId: Int): Tafsir

    @Query("SELECT * FROM Tafsir WHERE editionId = :editionId")
    suspend fun getTafsirByEdition(editionId: Int): List<Tafsir>

    @Query("SELECT * FROM Tafsir WHERE language = :language")
    suspend fun getTafsirByLanguage(language: String): List<Tafsir>

}