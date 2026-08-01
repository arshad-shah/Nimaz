package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.HadithBookEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithEntity
import kotlinx.coroutines.flow.Flow

/** A chapter id paired with how many hadiths it contains. */
data class HadithChapterCount(
    val chapterId: Int,
    val hadithCount: Int
)

@Dao
interface HadithDao {
    // Book operations
    @Query("SELECT * FROM hadith_books ORDER BY id ASC")
    fun getAllBooks(): Flow<List<HadithBookEntity>>

    @Query("SELECT * FROM hadith_books WHERE id = :bookId")
    suspend fun getBookById(bookId: Int): HadithBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<HadithBookEntity>)

    // Hadith operations
    @Query("SELECT * FROM hadiths WHERE chapter_id = :chapterId ORDER BY number_in_chapter ASC")
    fun getHadithsByChapter(chapterId: Int): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE book_id = :bookId ORDER BY number_in_book ASC")
    fun getHadithsByBook(bookId: Int): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE id = :hadithId")
    suspend fun getHadithById(hadithId: Int): HadithEntity?

    @Query("SELECT * FROM hadiths WHERE book_id = :bookId AND number_in_book = :hadithNumber")
    suspend fun getHadithByNumber(bookId: Int, hadithNumber: Int): HadithEntity?

    @Query("SELECT * FROM hadiths WHERE reference = :reference LIMIT 1")
    suspend fun getHadithByReference(reference: String): HadithEntity?

    @Query("SELECT * FROM hadiths WHERE text_english LIKE '%' || :query || '%' OR text_arabic LIKE '%' || :query || '%'")
    fun searchHadiths(query: String): Flow<List<HadithEntity>>

    /**
     * The hadiths the shipped search index named (#330).
     *
     * `text_arabic LIKE '%…%'` above could never match a typed Arabic query — the matn
     * is vocalised — and scanned 36 MB to fail. This is a primary-key lookup after a
     * folded index has already done the matching.
     */
    @Query("SELECT * FROM hadiths WHERE id IN (:ids) ORDER BY book_id, number_in_book")
    suspend fun getHadithsByIds(ids: List<Int>): List<HadithEntity>

    @Query("SELECT * FROM hadiths WHERE book_id = :bookId AND (text_english LIKE '%' || :query || '%' OR text_arabic LIKE '%' || :query || '%')")
    fun searchHadithsInBook(bookId: Int, query: String): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE grade = :grade ORDER BY book_id, number_in_book")
    fun getHadithsByGrade(grade: String): Flow<List<HadithEntity>>

    @Query("SELECT chapter_id AS chapterId, COUNT(*) AS hadithCount FROM hadiths WHERE book_id = :bookId GROUP BY chapter_id ORDER BY chapter_id ASC")
    fun getChapterCountsForBook(bookId: Int): Flow<List<HadithChapterCount>>

    // Get all hadiths (for hadith of the day)
    @Query("SELECT * FROM hadiths")
    fun getAllHadiths(): Flow<List<HadithEntity>>

    // Get total hadith count (more efficient for hadith of the day calculation)
    @Query("SELECT COUNT(*) FROM hadiths")
    suspend fun getHadithCount(): Int

    // Get hadith by offset (for hadith of the day - deterministic selection).
    // ORDER BY id keeps the offset stable regardless of SQLite's internal row
    // ordering, so the same offset always maps to the same hadith.
    @Query("SELECT * FROM hadiths ORDER BY id ASC LIMIT 1 OFFSET :offset")
    suspend fun getHadithByOffset(offset: Int): HadithEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiths(hadiths: List<HadithEntity>)

    // Bookmark operations
}
