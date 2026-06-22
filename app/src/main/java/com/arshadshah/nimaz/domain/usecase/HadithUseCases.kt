package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.repository.HadithRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class HadithUseCases(
    val getAllBooks: GetAllBooksUseCase,
    val getBookById: GetBookByIdUseCase,
    val getChaptersByBook: GetChaptersByBookUseCase,
    val getChapterById: GetChapterByIdUseCase,
    val getHadithsByChapter: GetHadithsByChapterUseCase,
    val getHadithById: GetHadithByIdUseCase,
    val getHadithByNumber: GetHadithByNumberUseCase,
    val getHadithsByGrade: GetHadithsByGradeUseCase,
    val getHadithOfTheDay: GetHadithOfTheDayUseCase,
    val searchHadiths: SearchHadithsUseCase,
    val searchHadithsInBook: SearchHadithsInBookUseCase,
    val getAllBookmarks: GetAllBookmarksUseCase,
    val isHadithBookmarked: IsHadithBookmarkedUseCase,
    val toggleBookmark: ToggleBookmarkUseCase,
    val insertBookmark: InsertHadithBookmarkUseCase,
    val updateBookmark: UpdateHadithBookmarkUseCase,
    val deleteBookmark: DeleteHadithBookmarkUseCase,
    val getDailyHadith: GetDailyHadithUseCase
)

class GetAllBooksUseCase @Inject constructor(private val repository: HadithRepository) {
    operator fun invoke(): Flow<List<HadithBook>> = repository.getAllBooks()
}

class GetBookByIdUseCase @Inject constructor(private val repository: HadithRepository) {
    suspend operator fun invoke(bookId: String): HadithBook? = repository.getBookById(bookId)
}

class GetChaptersByBookUseCase @Inject constructor(private val repository: HadithRepository) {
    operator fun invoke(bookId: String): Flow<List<HadithChapter>> = repository.getChaptersByBook(bookId)
}

class GetChapterByIdUseCase @Inject constructor(private val repository: HadithRepository) {
    suspend operator fun invoke(chapterId: String): HadithChapter? = repository.getChapterById(chapterId)
}

class GetHadithsByChapterUseCase @Inject constructor(private val repository: HadithRepository) {
    operator fun invoke(chapterId: String): Flow<List<Hadith>> = repository.getHadithsByChapter(chapterId)
}

class GetHadithByIdUseCase @Inject constructor(private val repository: HadithRepository) {
    suspend operator fun invoke(hadithId: String): Hadith? = repository.getHadithById(hadithId)
}

class GetHadithByNumberUseCase @Inject constructor(private val repository: HadithRepository) {
    suspend operator fun invoke(bookId: String, hadithNumber: Int): Hadith? =
        repository.getHadithByNumber(bookId, hadithNumber)
}

class GetHadithsByGradeUseCase @Inject constructor(private val repository: HadithRepository) {
    operator fun invoke(grade: HadithGrade): Flow<List<Hadith>> = repository.getHadithsByGrade(grade)
}

class GetHadithOfTheDayUseCase @Inject constructor(private val repository: HadithRepository) {
    suspend operator fun invoke(): Hadith? = repository.getHadithOfTheDay()
}

class SearchHadithsUseCase @Inject constructor(private val repository: HadithRepository) {
    operator fun invoke(query: String): Flow<List<HadithSearchResult>> = repository.searchHadiths(query)
}

class SearchHadithsInBookUseCase @Inject constructor(private val repository: HadithRepository) {
    operator fun invoke(bookId: String, query: String): Flow<List<HadithSearchResult>> =
        repository.searchHadithsInBook(bookId, query)
}

class GetAllBookmarksUseCase @Inject constructor(private val repository: HadithRepository) {
    operator fun invoke(): Flow<List<HadithBookmark>> = repository.getAllBookmarks()
}

class IsHadithBookmarkedUseCase @Inject constructor(private val repository: HadithRepository) {
    operator fun invoke(hadithId: String): Flow<Boolean> = repository.isHadithBookmarked(hadithId)
}

class ToggleBookmarkUseCase @Inject constructor(private val repository: HadithRepository) {
    suspend operator fun invoke(hadithId: String, bookId: String, hadithNumber: Int) =
        repository.toggleBookmark(hadithId, bookId, hadithNumber)
}

class InsertHadithBookmarkUseCase @Inject constructor(private val repository: HadithRepository) {
    suspend operator fun invoke(bookmark: HadithBookmark) = repository.insertBookmark(bookmark)
}

class UpdateHadithBookmarkUseCase @Inject constructor(private val repository: HadithRepository) {
    suspend operator fun invoke(bookmark: HadithBookmark) = repository.updateBookmark(bookmark)
}

class DeleteHadithBookmarkUseCase @Inject constructor(private val repository: HadithRepository) {
    suspend operator fun invoke(hadithId: String) = repository.deleteBookmark(hadithId)
}

/**
 * Deterministic "hadith of the day": scatters each day across the whole collection
 * with a Knuth multiplicative hash so consecutive days land on very different
 * hadiths, while the same day always yields the same one. [epochDay] is passed in
 * (e.g. `LocalDate.now().toEpochDay()`) so the use case stays pure/testable.
 */
class GetDailyHadithUseCase @Inject constructor(private val repository: HadithRepository) {
    suspend operator fun invoke(epochDay: Long): Hadith? {
        val total = repository.getHadithCount()
        if (total == 0) return null
        val offset = Math.floorMod(epochDay * 2654435761L, total.toLong()).toInt()
        return repository.getHadithByOffset(offset)
    }
}
