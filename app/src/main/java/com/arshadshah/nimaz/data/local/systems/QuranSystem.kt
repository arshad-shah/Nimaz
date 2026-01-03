package com.arshadshah.nimaz.data.local.systems

import com.arshadshah.nimaz.data.local.dao.AyaDao
import com.arshadshah.nimaz.data.local.dao.JuzDao
import com.arshadshah.nimaz.data.local.dao.KhatamProgressDao
import com.arshadshah.nimaz.data.local.dao.KhatamSessionDao
import com.arshadshah.nimaz.data.local.dao.ReadingProgressDao
import com.arshadshah.nimaz.data.local.dao.SurahDao
import com.arshadshah.nimaz.data.local.models.KhatamProgress
import com.arshadshah.nimaz.data.local.models.KhatamSession
import com.arshadshah.nimaz.data.local.models.ReadingProgress
import com.arshadshah.nimaz.utils.QuranUtils
import javax.inject.Inject

class QuranSystem @Inject constructor(
    private val ayaDao: AyaDao,
    private val juzDao: JuzDao,
    private val surahDao: SurahDao,
    private val readingProgressDao: ReadingProgressDao,
    private val khatamSessionDao: KhatamSessionDao,
    private val khatamProgressDao: KhatamProgressDao
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

    suspend fun getReadingProgress(surahNumber: Int) =
        readingProgressDao.getProgressForSurah(surahNumber)

    suspend fun updateReadingProgress(progress: ReadingProgress) =
        readingProgressDao.updateProgress(progress)

    suspend fun getRecentlyRead() =
        readingProgressDao.getRecentlyRead()

    //deleteProgress
    suspend fun deleteReadingProgress(progress: ReadingProgress) =
        readingProgressDao.deleteProgress(progress)

    //clearAllReadingProgress
    suspend fun clearAllReadingProgress() =
        readingProgressDao.clearAllReadingProgress()

    //getAllProgressOrderedByCompletion
    suspend fun getAllProgressOrderedByCompletion() =
        readingProgressDao.getAllProgressOrderedByCompletion()

    // Navigation helpers
    suspend fun getBookmarkedAyaNumbers(surahNumber: Int) =
        ayaDao.getBookmarkedAyaNumbers(surahNumber)

    suspend fun getFavoriteAyaNumbers(surahNumber: Int) =
        ayaDao.getFavoriteAyaNumbers(surahNumber)

    suspend fun getNotedAyaNumbers(surahNumber: Int) =
        ayaDao.getNotedAyaNumbers(surahNumber)

    suspend fun getPreviousAya(currentAyaNumber: Int) =
        ayaDao.getPreviousAya(currentAyaNumber)

    suspend fun getNextAya(currentAyaNumber: Int) =
        ayaDao.getNextAya(currentAyaNumber)

    suspend fun getTotalAyasInSurah(surahNumber: Int) =
        ayaDao.getTotalAyasInSurah(surahNumber)

    // Khatam Session Operations
    suspend fun getActiveKhatam() = khatamSessionDao.getActiveKhatam()
    suspend fun getAllKhatamSessions() = khatamSessionDao.getAllKhatamSessions()
    suspend fun getCompletedKhatams() = khatamSessionDao.getCompletedKhatams()
    suspend fun getKhatamById(id: Long) = khatamSessionDao.getKhatamById(id)
    suspend fun insertKhatam(khatam: KhatamSession) = khatamSessionDao.insertKhatam(khatam)
    suspend fun updateKhatam(khatam: KhatamSession) = khatamSessionDao.updateKhatam(khatam)
    suspend fun deleteKhatam(khatam: KhatamSession) = khatamSessionDao.deleteKhatam(khatam)
    suspend fun pauseKhatam(id: Long) = khatamSessionDao.pauseKhatam(id)
    suspend fun resumeKhatam(id: Long) = khatamSessionDao.resumeKhatam(id)
    suspend fun completeKhatam(id: Long, completionDate: String) =
        khatamSessionDao.completeKhatam(id, completionDate)
    suspend fun updateKhatamProgress(id: Long, surah: Int, aya: Int, totalRead: Int) =
        khatamSessionDao.updateKhatamProgress(id, surah, aya, totalRead)

    // Khatam Progress Operations
    suspend fun getProgressForKhatam(khatamId: Long) = khatamProgressDao.getProgressForKhatam(khatamId)
    suspend fun getProgressForDate(khatamId: Long, date: String) =
        khatamProgressDao.getProgressForDate(khatamId, date)
    suspend fun getLastProgressBeforeDate(khatamId: Long, date: String) =
        khatamProgressDao.getLastProgressBeforeDate(khatamId, date)
    
    suspend fun getAyasReadToday(khatamId: Long, date: String): Int {
        // Calculate based on today's max progress - yesterday's last progress
        val lastProgress = khatamProgressDao.getLastProgressBeforeDate(khatamId, date)
        val startTotal = if (lastProgress != null) {
            QuranUtils.calculateTotalAyasRead(lastProgress.surahNumber, lastProgress.ayaNumber)
        } else {
            0
        }

        val todayProgress = khatamProgressDao.getProgressForDate(khatamId, date)
        if (todayProgress.isEmpty()) return 0

        val maxToday = todayProgress.maxOf { 
            QuranUtils.calculateTotalAyasRead(it.surahNumber, it.ayaNumber)
        }
        
        return (maxToday - startTotal).coerceAtLeast(0)
    }
        
    suspend fun insertKhatamProgress(progress: KhatamProgress) =
        khatamProgressDao.insertProgress(progress)
    suspend fun deleteKhatamProgress(progress: KhatamProgress) =
        khatamProgressDao.deleteProgress(progress)
    suspend fun deleteAllProgressForKhatam(khatamId: Long) =
        khatamProgressDao.deleteAllProgressForKhatam(khatamId)
    suspend fun getAverageSessionDuration(khatamId: Long) =
        khatamProgressDao.getAverageSessionDuration(khatamId)
    suspend fun getActiveDaysCount(khatamId: Long) =
        khatamProgressDao.getActiveDaysCount(khatamId)
}
