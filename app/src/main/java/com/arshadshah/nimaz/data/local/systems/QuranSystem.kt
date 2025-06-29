package com.arshadshah.nimaz.data.local.systems

import com.arshadshah.nimaz.data.local.dao.AyaDao
import com.arshadshah.nimaz.data.local.dao.JuzDao
import com.arshadshah.nimaz.data.local.dao.SurahDao
import javax.inject.Inject

class QuranSystem @Inject constructor(
    private val ayaDao: AyaDao,
    private val juzDao: JuzDao,
    private val surahDao: SurahDao
) {
    // Existing Aya Reading Operations
    fun getAyasOfSurah(surahNumber: Int) = ayaDao.getAyasOfSurah(surahNumber)
    suspend fun getAyasOfJuz(juzNumber: Int) = ayaDao.getAyasOfJuz(juzNumber)
    suspend fun getRandomAya() = ayaDao.getRandomAya()

    // Aya Management
    suspend fun countAllAyat() = ayaDao.countAllAyas()
    suspend fun deleteAllAyat() = ayaDao.deleteAllAyas()

    // Bookmark Operations
    suspend fun bookmarkAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        bookmarkAya: Boolean
    ) = ayaDao.bookmarkAya(ayaNumber, surahNumber, ayaNumberInSurah, bookmarkAya)

    suspend fun deleteBookmarkFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int
    ) = ayaDao.deleteBookmarkFromAya(ayaNumber, surahNumber, ayaNumberInSurah)

    suspend fun getBookmarkedAyas() = ayaDao.getBookmarkedAyas()

    // Favorite Operations
    suspend fun favoriteAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        favoriteAya: Boolean
    ) = ayaDao.favoriteAya(ayaNumber, surahNumber, ayaNumberInSurah, favoriteAya)

    suspend fun deleteFavoriteFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int
    ) = ayaDao.deleteFavoriteFromAya(ayaNumber, surahNumber, ayaNumberInSurah)

    suspend fun getFavoritedAyas() = ayaDao.getFavoritedAyas()

    // Note Operations
    suspend fun addNoteToAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        note: String
    ) = ayaDao.addNoteToAya(ayaNumber, surahNumber, ayaNumberInSurah, note)

    suspend fun getNoteOfAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int
    ) = ayaDao.getNoteOfAya(ayaNumber, surahNumber, ayaNumberInSurah)

    suspend fun deleteNoteFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int
    ) = ayaDao.deleteNoteFromAya(ayaNumber, surahNumber, ayaNumberInSurah)

    suspend fun getAyasWithNotes() = ayaDao.getAyasWithNotes()

    // Audio Operations
    suspend fun addAudioToAya(
        surahNumber: Int,
        ayaNumberInSurah: Int,
        audio: String
    ) = ayaDao.addAudioToAya(surahNumber, ayaNumberInSurah, audio)

    // NEW: Search Operations
    suspend fun searchAyas(query: String) = ayaDao.searchAyas(query)

    suspend fun searchAyasAdvanced(
        query: String = "",
        surahNumber: Int? = null,
        juzNumber: Int? = null,
        isFavorite: Int? = null,
        isBookmarked: Int? = null,
        hasNote: Int? = null
    ) = ayaDao.searchAyasAdvanced(query, surahNumber, juzNumber, isFavorite, isBookmarked, hasNote)

    suspend fun searchAyasInArabic(query: String) = ayaDao.searchAyasInArabic(query)
    suspend fun searchAyasInEnglish(query: String) = ayaDao.searchAyasInEnglish(query)
    suspend fun searchAyasInUrdu(query: String) = ayaDao.searchAyasInUrdu(query)

    suspend fun searchFavoriteAyas(query: String) = ayaDao.searchFavoriteAyas(query)
    suspend fun searchBookmarkedAyas(query: String) = ayaDao.searchBookmarkedAyas(query)
    suspend fun searchAyasWithNotes(query: String) = ayaDao.searchAyasWithNotes(query)

    suspend fun getRandomSearchAya(query: String) = ayaDao.getRandomSearchAya(query)
    suspend fun countSearchResults(query: String) = ayaDao.countSearchResults(query)

    // Juz Operations
    suspend fun getAllJuz() = juzDao.getAllJuz()
    suspend fun getJuzById(number: Int) = juzDao.getJuzById(number)

    // Surah Operations
    fun getAllSurah() = surahDao.getAllSurahs()
    fun getSurahById(number: Int) = surahDao.getSurahById(number)
}