package com.arshadshah.nimaz.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.arshadshah.nimaz.data.local.models.HadithChapter
import com.arshadshah.nimaz.data.local.models.HadithEntity
import com.arshadshah.nimaz.data.local.models.HadithFavourite
import com.arshadshah.nimaz.data.local.models.HadithMetadata


@Dao
interface HadithDao {
    @Query("SELECT * FROM Metadata")
    suspend fun getAllMetadata(): List<HadithMetadata>

    @Query("SELECT * FROM HadithChapters WHERE bookId = :bookId")
    suspend fun getAllHadithChaptersForABook(bookId: Int): List<HadithChapter>

    @Query("SELECT * FROM Hadiths WHERE bookId = :bookId AND chapterId = :chapterId")
    suspend fun getAllHadithsForABook(bookId: Int, chapterId: Int): List<HadithEntity>

    //update the favourite status of a hadith
    @Query("UPDATE Hadiths SET favourite = :favourite WHERE id = :id")
    suspend fun updateFavouriteStatus(id: Int, favourite: Boolean)

    @Query("SELECT * FROM Hadiths WHERE favourite = 1")
    suspend fun getAllHadithWithFavourites(): List<HadithEntity>

    @Query("SELECT * FROM Metadata WHERE id = :id")
    suspend fun getHadithMetadata(id: Int): HadithMetadata

    @Query("SELECT * FROM HadithChapters WHERE bookId = :bookId AND chapterId = :chapterId")
    suspend fun getHadithChapter(bookId: Int, chapterId: Int): HadithChapter

    suspend fun getAllFavourites(): List<HadithFavourite> {
        val hadithWithFavourites = getAllHadithWithFavourites()
        val hadithWithFavouritesMetadata = mutableMapOf<Int, HadithMetadata>()
        val hadithWithFavouritesChapters = mutableMapOf<Pair<Int, Int>, HadithChapter>()

        // Fetching metadata and chapters for each hadith
        hadithWithFavourites.forEach { hadith ->
            hadithWithFavouritesMetadata[hadith.bookId] = getHadithMetadata(hadith.bookId)
            val chapterKey = Pair(hadith.bookId, hadith.chapterId)
            hadithWithFavouritesChapters[chapterKey] =
                getHadithChapter(hadith.bookId, hadith.chapterId)
        }

        return hadithWithFavourites.map { hadith ->
            val metadata = hadithWithFavouritesMetadata[hadith.bookId] ?: HadithMetadata(
                id = 0,
                length = 0,
                title_arabic = "",
                author_arabic = "",
                introduction_arabic = "",
                title_english = "",
                author_english = "",
                introduction_english = ""
            ) // Replace with default or error handling
            val chapter = hadithWithFavouritesChapters[Pair(hadith.bookId, hadith.chapterId)]
                ?: HadithChapter(
                    chapterId = 0,
                    bookId = 0,
                    title_arabic = "",
                    title_english = ""
                ) // Replace with default or error handling

            HadithFavourite(
                hadithId = hadith.id,
                book_title_arabic = metadata.title_arabic,
                book_title_english = metadata.title_english,
                chapter_title_arabic = chapter.title_arabic,
                chapter_title_english = chapter.title_english,
                chapterId = chapter.chapterId,
                bookId = chapter.bookId,
                idInBook = hadith.idInBook,
                favourite = hadith.favourite
            )
        }
    }

}