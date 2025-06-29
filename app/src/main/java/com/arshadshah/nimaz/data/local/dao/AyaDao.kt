package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.LocalAya

@Dao
interface AyaDao {

    //get all the ayas
    @Query("SELECT * FROM Aya")
    suspend fun getAllAyas(): List<LocalAya>

    //count all the ayas
    @Query("SELECT COUNT(*) FROM Aya")
    suspend fun countAllAyas(): Int

    //get all the ayas of a surah
    @Query("SELECT * FROM Aya WHERE suraNumber = :surahNumber")
    fun getAyasOfSurah(surahNumber: Int): List<LocalAya>

    //get all the ayas of a juz
    @Query("SELECT * FROM Aya WHERE juzNumber = :juzNumber")
    suspend fun getAyasOfJuz(juzNumber: Int): List<LocalAya>

    //bookmark an aya
    @Query("UPDATE Aya SET bookmark = :bookmark WHERE ayaNumberInSurah = :ayaNumber AND suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
    suspend fun bookmarkAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        bookmark: Boolean,
    )

    //favorite an aya
    @Query("UPDATE Aya SET favorite = :favorite WHERE ayaNumberInSurah = :ayaNumber AND suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
    suspend fun favoriteAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        favorite: Boolean,
    )

    //add a note to an aya
    @Query("UPDATE Aya SET note = :note WHERE ayaNumberInSurah = :ayaNumber AND suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
    suspend fun addNoteToAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        note: String,
    )

    //get a  note fro an aya
    @Query("SELECT note FROM Aya WHERE ayaNumberInSurah = :ayaNumber AND suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
    suspend fun getNoteOfAya(ayaNumber: Int, surahNumber: Int, ayaNumberInSurah: Int): String


    //get all the bookmarked ayas
    @Query("SELECT * FROM Aya WHERE bookmark = 1")
    suspend fun getBookmarkedAyas(): List<LocalAya>

    //get all the favorited ayas
    @Query("SELECT * FROM Aya WHERE favorite = 1")
    suspend fun getFavoritedAyas(): List<LocalAya>

    //get all the ayas with notes
    @Query("SELECT * FROM Aya WHERE note != ''")
    suspend fun getAyasWithNotes(): List<LocalAya>

    //delete a note from an aya
    @Query("UPDATE Aya SET note = '' WHERE ayaNumberInSurah = :ayaNumber AND suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
    suspend fun deleteNoteFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    )

    //delete a bookmark from an aya
    @Query("UPDATE Aya SET bookmark = 0 WHERE ayaNumberInSurah = :ayaNumber AND suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
    suspend fun deleteBookmarkFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    )

    //delete a favorite from an aya
    @Query("UPDATE Aya SET favorite = 0 WHERE ayaNumberInSurah = :ayaNumber AND suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
    suspend fun deleteFavoriteFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    )

    //add a audio local path to an aya
    //audioFileLocation
    @Query("UPDATE Aya SET audioFileLocation = :audioFileLocation WHERE suraNumber = :surahNumber AND ayaNumberInSurah = :ayaNumberInSurah")
    suspend fun addAudioToAya(
        surahNumber: Int,
        ayaNumberInSurah: Int,
        audioFileLocation: String,
    )

    @Insert(entity = LocalAya::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aya: List<LocalAya>)

    //count the number of ayas
    @Query("SELECT COUNT(*) FROM Aya WHERE juzNumber = :juzNumber")
    suspend fun countJuzAya(juzNumber: Int): Int

    @Query("SELECT COUNT(*) FROM Aya WHERE suraNumber = :surahNumber")
    suspend fun countSurahAya(surahNumber: Int): Int

    //get a random aya
    @Query("SELECT * FROM Aya ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomAya(): LocalAya

    //get ayat by aya number in surah
    @Query("SELECT * FROM Aya WHERE ayaNumberInSurah = :ayaNumberInSurah")
    suspend fun getAyatByAyaNumberInSurah(ayaNumberInSurah: Int): LocalAya

    //delete all the ayas
    @Query("DELETE FROM Aya")
    suspend fun deleteAllAyas()


    // Basic search operations
    @Query("""
        SELECT * FROM Aya 
        WHERE LOWER(ayaArabic) LIKE '%' || LOWER(:query) || '%' 
        OR LOWER(translationEnglish) LIKE '%' || LOWER(:query) || '%'
        OR LOWER(translationUrdu) LIKE '%' || LOWER(:query) || '%'
        ORDER BY suraNumber ASC, ayaNumberInSurah ASC
    """)
    suspend fun searchAyas(query: String): List<LocalAya>

    // Advanced search with multiple criteria
    @Query("""
        SELECT * FROM Aya 
        WHERE (:surahNumber IS NULL OR suraNumber = :surahNumber)
        AND (:juzNumber IS NULL OR juzNumber = :juzNumber)
        AND (:isFavorite IS NULL OR favorite = :isFavorite)
        AND (:isBookmarked IS NULL OR bookmark = :isBookmarked)
        AND (:hasNote IS NULL OR (CASE WHEN :hasNote = 1 THEN note != '' ELSE note = '' END))
        AND (
            LOWER(ayaArabic) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(translationEnglish) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(translationUrdu) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY suraNumber ASC, ayaNumberInSurah ASC
    """)
    suspend fun searchAyasAdvanced(
        query: String = "",
        surahNumber: Int? = null,
        juzNumber: Int? = null,
        isFavorite: Int? = null,
        isBookmarked: Int? = null,
        hasNote: Int? = null
    ): List<LocalAya>

    // Search specifically in Arabic text only
    @Query("""
        SELECT * FROM Aya 
        WHERE LOWER(ayaArabic) LIKE '%' || LOWER(:query) || '%'
        ORDER BY suraNumber ASC, ayaNumberInSurah ASC
    """)
    suspend fun searchAyasInArabic(query: String): List<LocalAya>

    // Search specifically in English translation only
    @Query("""
        SELECT * FROM Aya 
        WHERE LOWER(translationEnglish) LIKE '%' || LOWER(:query) || '%'
        ORDER BY suraNumber ASC, ayaNumberInSurah ASC
    """)
    suspend fun searchAyasInEnglish(query: String): List<LocalAya>

    // Search specifically in Urdu translation only
    @Query("""
        SELECT * FROM Aya 
        WHERE LOWER(translationUrdu) LIKE '%' || LOWER(:query) || '%'
        ORDER BY suraNumber ASC, ayaNumberInSurah ASC
    """)
    suspend fun searchAyasInUrdu(query: String): List<LocalAya>

    // Search in favorites only
    @Query("""
        SELECT * FROM Aya 
        WHERE favorite = 1
        AND (
            LOWER(ayaArabic) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(translationEnglish) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(translationUrdu) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY suraNumber ASC, ayaNumberInSurah ASC
    """)
    suspend fun searchFavoriteAyas(query: String): List<LocalAya>

    // Search in bookmarks only
    @Query("""
        SELECT * FROM Aya 
        WHERE bookmark = 1
        AND (
            LOWER(ayaArabic) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(translationEnglish) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(translationUrdu) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY suraNumber ASC, ayaNumberInSurah ASC
    """)
    suspend fun searchBookmarkedAyas(query: String): List<LocalAya>

    // Search in notes
    @Query("""
        SELECT * FROM Aya 
        WHERE note != ''
        AND (
            LOWER(ayaArabic) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(translationEnglish) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(translationUrdu) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(note) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY suraNumber ASC, ayaNumberInSurah ASC
    """)
    suspend fun searchAyasWithNotes(query: String): List<LocalAya>

    // Get random aya from search results
    @Query("""
        SELECT * FROM Aya 
        WHERE (
            LOWER(ayaArabic) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(translationEnglish) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(translationUrdu) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY RANDOM() 
        LIMIT 1
    """)
    suspend fun getRandomSearchAya(query: String): LocalAya?

    // Count search results
    @Query("""
        SELECT COUNT(*) FROM Aya 
        WHERE (
            LOWER(ayaArabic) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(translationEnglish) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(translationUrdu) LIKE '%' || LOWER(:query) || '%'
        )
    """)
    suspend fun countSearchResults(query: String): Int
}