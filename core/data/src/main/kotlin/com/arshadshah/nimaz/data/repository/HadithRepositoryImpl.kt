package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.common.mapItems
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.entity.HadithBookEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithEntity
import com.arshadshah.nimaz.data.local.search.ContentSearchIndex
import com.arshadshah.nimaz.data.local.search.SearchKind
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.repository.HadithRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HadithRepositoryImpl @Inject constructor(
    private val hadithDao: HadithDao,
    private val bookmarkDao: BookmarkDao,
    private val searchIndex: ContentSearchIndex
) : HadithRepository {

    override suspend fun getHadithCount(): Int = hadithDao.getHadithCount()

    override suspend fun getHadithByOffset(offset: Int): Hadith? =
        hadithDao.getHadithByOffset(offset)?.toDomain()

    override suspend fun getHadithOfTheDay(): Hadith? {
        val totalHadiths = hadithDao.getHadithCount()
        if (totalHadiths == 0) return null

        // Use day of year to get a deterministic but daily-changing hadith
        val dayOfYear = LocalDate.now().dayOfYear
        val offset = dayOfYear % totalHadiths

        return hadithDao.getHadithByOffset(offset)?.toDomain()
    }

    override fun getAllBooks(): Flow<List<HadithBook>> {
        return hadithDao.getAllBooks().mapItems { it.toDomain() }
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
        return hadithDao.getHadithsByChapter(chapterNum).mapItems { it.toDomain() }
    }

    override fun getHadithsByBook(bookId: String): Flow<List<Hadith>> {
        return hadithDao.getHadithsByBook(bookId.toIntOrNull() ?: 0).mapItems { it.toDomain() }
    }

    override suspend fun getHadithsByIds(hadithIds: List<String>): List<Hadith> {
        val numeric = hadithIds.mapNotNull { it.toIntOrNull() }
        if (numeric.isEmpty()) return emptyList()
        return hadithDao.getHadithsByIds(numeric).map { it.toDomain() }
    }

    override suspend fun getHadithById(hadithId: String): Hadith? {
        return hadithDao.getHadithById(hadithId.toIntOrNull() ?: return null)?.toDomain()
    }

    override suspend fun getHadithByNumber(bookId: String, hadithNumber: Int): Hadith? {
        return hadithDao.getHadithByNumber(bookId.toIntOrNull() ?: return null, hadithNumber)
            ?.toDomain()
    }

    override suspend fun getHadithByReference(reference: String): Hadith? {
        return hadithDao.getHadithByReference(reference)?.toDomain()
    }

    override fun getHadithsByGrade(grade: HadithGrade): Flow<List<Hadith>> {
        val gradeString = when (grade) {
            HadithGrade.SAHIH -> "sahih"
            HadithGrade.HASAN -> "hasan"
            HadithGrade.DAIF -> "daif"
            HadithGrade.MAWDU -> "mawdu"
            HadithGrade.UNKNOWN -> "unknown"
        }
        return hadithDao.getHadithsByGrade(gradeString).mapItems { it.toDomain() }
    }

    /**
     * The index first, the scan only where there is no index (#330).
     *
     * `text_arabic LIKE '%…%'` returned nothing for every Arabic query ever run against
     * the matn — it is vocalised, and no keyboard produces the marks — while scanning
     * 36 MB to fail. `createFromAsset` copies the artifact once, so installs made before
     * the index shipped keep the scan; everyone else gets 34,532 hadith folded into an
     * index that answers in under a millisecond.
     */
    override fun searchHadiths(query: String): Flow<List<HadithSearchResult>> {
        return combine(
            hadithDao.getAllBooks(),
            flow {
                if (searchIndex.isAvailable()) {
                    val ids = searchIndex.refs(query, SearchKind.HADITH)
                        .mapNotNull(String::toIntOrNull)
                    emit(hadithDao.getHadithsByIds(ids))
                } else {
                    emitAll(hadithDao.searchHadiths(query))
                }
            }
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

    // Bookmarks are the user's, so they come from the user's database — and from the one
    // table that holds every kind of mark, with `kind` where there used to be a table.

    override fun getAllBookmarks(): Flow<List<HadithBookmark>> {
        return bookmarkDao.bookmarks(BookmarkKind.HADITH).mapItems { it.toHadithBookmark() }
    }

    override fun getBookmarksByBook(bookId: String): Flow<List<HadithBookmark>> {
        return bookmarkDao.inContext(BookmarkKind.HADITH, bookId.toIntOrNull() ?: 0)
            .mapItems { it.toHadithBookmark() }
    }

    override suspend fun getBookmarkByHadithId(hadithId: String): HadithBookmark? {
        return bookmarkDao.find(BookmarkKind.HADITH, hadithId.toIntOrNull() ?: return null)
            ?.toHadithBookmark()
    }

    override fun isHadithBookmarked(hadithId: String): Flow<Boolean> {
        return bookmarkDao.observeIsBookmarked(BookmarkKind.HADITH, hadithId.toIntOrNull() ?: 0)
    }

    override suspend fun toggleBookmark(hadithId: String, bookId: String, hadithNumber: Int) {
        val target = hadithId.toIntOrNull() ?: return
        val existing = bookmarkDao.find(BookmarkKind.HADITH, target)
        if (existing != null && existing.bookmarked) {
            bookmarkDao.delete(BookmarkKind.HADITH, target)
        } else {
            val now = System.currentTimeMillis()
            bookmarkDao.upsert(
                BookmarkEntity(
                    kind = BookmarkKind.HADITH,
                    targetId = target,
                    bookmarked = true,
                    favourite = existing?.favourite ?: false,
                    note = existing?.note,
                    colour = existing?.colour,
                    contextId = bookId.toIntOrNull(),
                    ordinal = hadithNumber,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            )
        }
    }

    override suspend fun insertBookmark(bookmark: HadithBookmark) {
        bookmarkDao.upsert(bookmark.toBookmarkEntity())
    }

    override suspend fun updateBookmark(bookmark: HadithBookmark) {
        bookmarkDao.upsert(bookmark.toBookmarkEntity())
    }

    override suspend fun deleteBookmark(hadithId: String) {
        bookmarkDao.delete(BookmarkKind.HADITH, hadithId.toIntOrNull() ?: return)
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

    /**
     * The consolidated row, read as a hadith bookmark.
     *
     * `context_id` is the book and `ordinal` the number within it — the same two facts the
     * old table spelled `bookId` and `hadithNumber`. The domain type is unchanged, so nothing
     * above this line knows the seven tables became one.
     */
    private fun BookmarkEntity.toHadithBookmark(): HadithBookmark {
        return HadithBookmark(
            id = 0,
            hadithId = targetId.toString(),
            bookId = (contextId ?: 0).toString(),
            hadithNumber = ordinal ?: 0,
            note = note,
            color = colour,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun HadithBookmark.toBookmarkEntity(): BookmarkEntity {
        return BookmarkEntity(
            kind = BookmarkKind.HADITH,
            targetId = hadithId.toIntOrNull() ?: 0,
            bookmarked = true,
            note = note,
            colour = color,
            contextId = bookId.toIntOrNull(),
            ordinal = hadithNumber,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
