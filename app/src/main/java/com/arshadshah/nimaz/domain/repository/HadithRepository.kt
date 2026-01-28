package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import kotlinx.coroutines.flow.Flow

interface HadithRepository {
    // Hadith of the day
    suspend fun getHadithOfTheDay(): Hadith?

    // Book operations
    fun getAllBooks(): Flow<List<HadithBook>>
    suspend fun getBookById(bookId: String): HadithBook?

    // Chapter operations
    fun getChaptersByBook(bookId: String): Flow<List<HadithChapter>>
    suspend fun getChapterById(chapterId: String): HadithChapter?
    fun searchChapters(bookId: String, query: String): Flow<List<HadithChapter>>

    // Hadith operations
    fun getHadithsByChapter(chapterId: String): Flow<List<Hadith>>
    fun getHadithsByBook(bookId: String): Flow<List<Hadith>>
    suspend fun getHadithById(hadithId: String): Hadith?
    suspend fun getHadithByNumber(bookId: String, hadithNumber: Int): Hadith?
    fun getHadithsByGrade(grade: HadithGrade): Flow<List<Hadith>>

    // Search operations
    fun searchHadiths(query: String): Flow<List<HadithSearchResult>>
    fun searchHadithsInBook(bookId: String, query: String): Flow<List<HadithSearchResult>>

    // Bookmark operations
    fun getAllBookmarks(): Flow<List<HadithBookmark>>
    fun getBookmarksByBook(bookId: String): Flow<List<HadithBookmark>>
    suspend fun getBookmarkByHadithId(hadithId: String): HadithBookmark?
    fun isHadithBookmarked(hadithId: String): Flow<Boolean>
    suspend fun toggleBookmark(hadithId: String, bookId: String, hadithNumber: Int)
    suspend fun updateBookmark(bookmark: HadithBookmark)
    suspend fun deleteBookmark(hadithId: String)

    // Data initialization
    suspend fun initializeHadithData()
    suspend fun isDataInitialized(): Boolean
}
