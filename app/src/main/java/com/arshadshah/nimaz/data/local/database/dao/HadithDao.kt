package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.HadithBookEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM hadiths WHERE text_english LIKE '%' || :query || '%' OR text_arabic LIKE '%' || :query || '%'")
    fun searchHadiths(query: String): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE book_id = :bookId AND (text_english LIKE '%' || :query || '%' OR text_arabic LIKE '%' || :query || '%')")
    fun searchHadithsInBook(bookId: Int, query: String): Flow<List<HadithEntity>>

    @Query("SELECT * FROM hadiths WHERE grade = :grade ORDER BY book_id, number_in_book")
    fun getHadithsByGrade(grade: String): Flow<List<HadithEntity>>

    @Query("SELECT DISTINCT chapter_id FROM hadiths WHERE book_id = :bookId ORDER BY chapter_id ASC")
    fun getChapterIdsForBook(bookId: Int): Flow<List<Int>>

    // Get all hadiths (for hadith of the day)
    @Query("SELECT * FROM hadiths")
    fun getAllHadiths(): Flow<List<HadithEntity>>

    // Get total hadith count (more efficient for hadith of the day calculation)
    @Query("SELECT COUNT(*) FROM hadiths")
    suspend fun getHadithCount(): Int

    // Get hadith by offset (for hadith of the day - deterministic selection)
    @Query("SELECT * FROM hadiths LIMIT 1 OFFSET :offset")
    suspend fun getHadithByOffset(offset: Int): HadithEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiths(hadiths: List<HadithEntity>)

    // Bookmark operations
    @Query("SELECT * FROM hadith_bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<HadithBookmarkEntity>>

    @Query("SELECT * FROM hadith_bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarksByBook(bookId: Int): Flow<List<HadithBookmarkEntity>>

    @Query("SELECT * FROM hadith_bookmarks WHERE hadithId = :hadithId LIMIT 1")
    suspend fun getBookmarkByHadithId(hadithId: Int): HadithBookmarkEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM hadith_bookmarks WHERE hadithId = :hadithId)")
    fun isHadithBookmarked(hadithId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: HadithBookmarkEntity)

    @Query("DELETE FROM hadith_bookmarks WHERE hadithId = :hadithId")
    suspend fun deleteBookmarkByHadithId(hadithId: Int)

    @Update
    suspend fun updateBookmark(bookmark: HadithBookmarkEntity)

    @Transaction
    suspend fun toggleBookmark(hadithId: Int, bookId: Int, hadithNumber: Int) {
        val existing = getBookmarkByHadithId(hadithId)
        if (existing != null) {
            deleteBookmarkByHadithId(hadithId)
        } else {
            insertBookmark(
                HadithBookmarkEntity(
                    hadithId = hadithId,
                    bookId = bookId,
                    hadithNumber = hadithNumber,
                    note = null,
                    color = null
                )
            )
        }
    }

    @Query("DELETE FROM hadith_bookmarks")
    suspend fun deleteAllUserData()
}
