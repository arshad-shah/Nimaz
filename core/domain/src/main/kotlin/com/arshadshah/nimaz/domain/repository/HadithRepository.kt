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

    /** Batched [getHadithById]; order is not guaranteed, look results up by id. */
    suspend fun getHadithsByIds(hadithIds: List<String>): List<Hadith>
    suspend fun getHadithByNumber(bookId: String, hadithNumber: Int): Hadith?

    /**
     * Looks a hadith up by its canonical `collection:number` reference (e.g.
     * "bukhari:6018") — the `reference` value stored on every hadith record.
     * Used to resolve AI-cited hadith references into local proof records.
     */
    suspend fun getHadithByReference(reference: String): Hadith?
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
    suspend fun insertBookmark(bookmark: HadithBookmark)
    suspend fun updateBookmark(bookmark: HadithBookmark)
    suspend fun deleteBookmark(hadithId: String)

    // Daily content (seeds the backfill before reading)
    suspend fun getHadithCount(): Int
    suspend fun getHadithByOffset(offset: Int): Hadith?

    // Data initialization
    suspend fun initializeHadithData()
    suspend fun isDataInitialized(): Boolean
}
