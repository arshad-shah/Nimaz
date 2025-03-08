package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.TafsirEdition

@Dao
interface TafsirEditionDao {
    @Query("SELECT * FROM TafsirEditions")
    suspend fun getAllEditions(): List<TafsirEdition>

    @Query("SELECT * FROM TafsirEditions WHERE id = :id")
    suspend fun getEditionById(id: Int): TafsirEdition?

    @Query("SELECT * FROM TafsirEditions WHERE language = :language")
    suspend fun getEditionsByLanguage(language: String): List<TafsirEdition>

    @Query("SELECT * FROM TafsirEditions WHERE author LIKE '%' || :authorName || '%'")
    suspend fun getEditionsByAuthor(authorName: String): List<TafsirEdition>

    @Query("SELECT COUNT(*) FROM TafsirEditions")
    suspend fun getEditionCount(): Int

    //get by language and author
    @Query("SELECT * FROM TafsirEditions WHERE language = :language AND author LIKE '%' || :authorName || '%'")
    suspend fun getEditionsByLanguageAndAuthor(language: String, authorName: String): TafsirEdition
}