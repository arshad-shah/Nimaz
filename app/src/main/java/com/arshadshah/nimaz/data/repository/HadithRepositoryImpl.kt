package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.entity.HadithBookEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithEntity
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.repository.HadithRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HadithRepositoryImpl @Inject constructor(
    private val hadithDao: HadithDao
) : HadithRepository {

    override suspend fun getHadithOfTheDay(): Hadith? {
        val totalHadiths = hadithDao.getHadithCount()
        if (totalHadiths == 0) return null

        // Use day of year to get a deterministic but daily-changing hadith
        val dayOfYear = LocalDate.now().dayOfYear
        val offset = dayOfYear % totalHadiths

        return hadithDao.getHadithByOffset(offset)?.toDomain()
    }

    override fun getAllBooks(): Flow<List<HadithBook>> {
        return hadithDao.getAllBooks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBookById(bookId: String): HadithBook? {
        return hadithDao.getBookById(bookId.toIntOrNull() ?: return null)?.toDomain()
    }

    override fun getChaptersByBook(bookId: String): Flow<List<HadithChapter>> {
        // Derive virtual chapters from the hadiths (there is no chapters table),
        // with a real per-chapter hadith count. The stored chapter_id is 0-based,
        // so the displayed chapter number is chapter_id + 1; the chapter `id`
        // keeps the raw chapter_id for loading.
        return hadithDao.getChapterCountsForBook(bookId.toIntOrNull() ?: 0).map { counts ->
            counts.map { (chapterId, count) ->
                val displayNumber = chapterId + 1
                HadithChapter(
                    id = "${bookId}_$chapterId",
                    bookId = bookId,
                    chapterNumber = displayNumber,
                    nameArabic = "الباب $displayNumber",
                    nameEnglish = "Chapter $displayNumber",
                    hadithCount = count,
                    hadithStartNumber = 0,
                    hadithEndNumber = 0
                )
            }
        }
    }

    override suspend fun getChapterById(chapterId: String): HadithChapter? {
        // Parse chapter ID (format: "{bookId}_{chapterNumber}")
        val parts = chapterId.split("_")
        if (parts.size != 2) return null
        val bookId = parts[0]
        val rawChapterId = parts[1].toIntOrNull() ?: return null
        val displayNumber = rawChapterId + 1
        return HadithChapter(
            id = chapterId,
            bookId = bookId,
            chapterNumber = displayNumber,
            nameArabic = "الباب $displayNumber",
            nameEnglish = "Chapter $displayNumber",
            hadithCount = 0,
            hadithStartNumber = 0,
            hadithEndNumber = 0
        )
    }

    override fun searchChapters(bookId: String, query: String): Flow<List<HadithChapter>> {
        // Search chapters by name (virtual chapters)
        return getChaptersByBook(bookId).map { chapters ->
            chapters.filter {
                it.nameEnglish.contains(query, ignoreCase = true) ||
                        it.nameArabic.contains(query)
            }
        }
    }

    override fun getHadithsByChapter(chapterId: String): Flow<List<Hadith>> {
        // Parse chapter ID (format: "{bookId}_{chapterNumber}")
        val parts = chapterId.split("_")
        val chapterNum =
            if (parts.size == 2) parts[1].toIntOrNull() ?: 0 else chapterId.toIntOrNull() ?: 0
        return hadithDao.getHadithsByChapter(chapterNum).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getHadithsByBook(bookId: String): Flow<List<Hadith>> {
        return hadithDao.getHadithsByBook(bookId.toIntOrNull() ?: 0).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getHadithById(hadithId: String): Hadith? {
        return hadithDao.getHadithById(hadithId.toIntOrNull() ?: return null)?.toDomain()
    }

    override suspend fun getHadithByNumber(bookId: String, hadithNumber: Int): Hadith? {
        return hadithDao.getHadithByNumber(bookId.toIntOrNull() ?: return null, hadithNumber)
            ?.toDomain()
    }

    override fun getHadithsByGrade(grade: HadithGrade): Flow<List<Hadith>> {
        val gradeString = when (grade) {
            HadithGrade.SAHIH -> "sahih"
            HadithGrade.HASAN -> "hasan"
            HadithGrade.DAIF -> "daif"
            HadithGrade.MAWDU -> "mawdu"
            HadithGrade.UNKNOWN -> "unknown"
        }
        return hadithDao.getHadithsByGrade(gradeString).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchHadiths(query: String): Flow<List<HadithSearchResult>> {
        return combine(
            hadithDao.getAllBooks(),
            hadithDao.searchHadiths(query)
        ) { books, entities ->
            // Create a map from book id to book name for lookup
            val bookMap = books.associate { it.id to it.nameEnglish }

            entities.map { hadith ->
                HadithSearchResult(
                    hadith = hadith.toDomain(),
                    bookName = bookMap[hadith.bookId] ?: "Book ${hadith.bookId}",
                    chapterName = "Chapter ${hadith.chapterId}",
                    matchedText = hadith.textEnglish
                )
            }
        }
    }

    override fun searchHadithsInBook(
        bookId: String,
        query: String
    ): Flow<List<HadithSearchResult>> {
        return combine(
            hadithDao.getAllBooks(),
            hadithDao.searchHadithsInBook(bookId.toIntOrNull() ?: 0, query)
        ) { books, entities ->
            val bookMap = books.associate { it.id to it.nameEnglish }

            entities.map { hadith ->
                HadithSearchResult(
                    hadith = hadith.toDomain(),
                    bookName = bookMap[hadith.bookId] ?: "Book ${hadith.bookId}",
                    chapterName = "Chapter ${hadith.chapterId}",
                    matchedText = hadith.textEnglish
                )
            }
        }
    }

    override fun getAllBookmarks(): Flow<List<HadithBookmark>> {
        return hadithDao.getAllBookmarks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBookmarksByBook(bookId: String): Flow<List<HadithBookmark>> {
        return hadithDao.getBookmarksByBook(bookId.toIntOrNull() ?: 0).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBookmarkByHadithId(hadithId: String): HadithBookmark? {
        return hadithDao.getBookmarkByHadithId(hadithId.toIntOrNull() ?: return null)?.toDomain()
    }

    override fun isHadithBookmarked(hadithId: String): Flow<Boolean> {
        return hadithDao.isHadithBookmarked(hadithId.toIntOrNull() ?: 0)
    }

    override suspend fun toggleBookmark(hadithId: String, bookId: String, hadithNumber: Int) {
        hadithDao.toggleBookmark(
            hadithId.toIntOrNull() ?: return,
            bookId.toIntOrNull() ?: return,
            hadithNumber
        )
    }

    override suspend fun updateBookmark(bookmark: HadithBookmark) {
        hadithDao.updateBookmark(bookmark.toEntity())
    }

    override suspend fun deleteBookmark(hadithId: String) {
        hadithDao.deleteBookmarkByHadithId(hadithId.toIntOrNull() ?: return)
    }

    override suspend fun initializeHadithData() {
        // Data is pre-populated in the database
    }

    override suspend fun isDataInitialized(): Boolean {
        return hadithDao.getAllBooks().first().isNotEmpty()
    }

    // Mapping functions
    private fun HadithBookEntity.toDomain(): HadithBook {
        return HadithBook(
            id = id.toString(),
            nameArabic = nameArabic,
            nameEnglish = nameEnglish,
            authorName = author,
            authorArabic = "",
            totalHadiths = hadithCount,
            totalChapters = 0,
            description = description,
            displayOrder = id
        )
    }

    private fun HadithEntity.toDomain(): Hadith {
        return Hadith(
            id = id.toString(),
            bookId = bookId.toString(),
            chapterId = chapterId.toString(),
            hadithNumber = numberInChapter,
            hadithNumberInBook = numberInBook,
            textArabic = textArabic,
            textEnglish = textEnglish,
            // Only an authentic, curated chain of narration is ever shown — we
            // never infer/guess one, so a hadith without verified isnād data
            // simply has no chain (the reader hides the section).
            narratorChain = narratorChain?.takeIf { it.isNotBlank() },
            narratorName = narrator,
            grade = HadithGrade.fromString(grade),
            gradeArabic = null,
            reference = reference
        )
    }

    private fun HadithBookmarkEntity.toDomain(): HadithBookmark {
        return HadithBookmark(
            id = id,
            hadithId = hadithId.toString(),
            bookId = bookId.toString(),
            hadithNumber = hadithNumber,
            note = note,
            color = color,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun HadithBookmark.toEntity(): HadithBookmarkEntity {
        return HadithBookmarkEntity(
            id = id,
            hadithId = hadithId.toIntOrNull() ?: 0,
            bookId = bookId.toIntOrNull() ?: 0,
            hadithNumber = hadithNumber,
            note = note,
            color = color,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
