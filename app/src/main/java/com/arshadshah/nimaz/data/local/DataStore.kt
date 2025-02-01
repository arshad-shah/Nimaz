package com.arshadshah.nimaz.data.local

import com.arshadshah.nimaz.data.local.models.HadithChapter
import com.arshadshah.nimaz.data.local.models.HadithEntity
import com.arshadshah.nimaz.data.local.models.HadithFavourite
import com.arshadshah.nimaz.data.local.models.HadithMetadata
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStore<Tafsir> @Inject constructor(
    db: AppDatabase
) {
    private val ayaDao = db.ayaDao
    private val juzDao = db.juz
    private val surahDao = db.surah
    private val prayerTimesDao = db.prayerTimes
    private val duaDao = db.dua
    private val prayerTrackerDao = db.prayersTracker
    private val fastTrackerDao = db.fastTracker
    private val tasbihTrackerDao = db.tasbihTracker
    private val categoryDao = db.category
    private val hadithDao = db.hadith
    private val tafsirDao = db.tafsirDao
    private val tafsirEditionDao = db.tafsirEditionDao

    suspend fun getAllMetadata(): List<HadithMetadata> = hadithDao.getAllMetadata()
    suspend fun getAllHadithChaptersForABook(bookId: Int): List<HadithChapter> =
        hadithDao.getAllHadithChaptersForABook(bookId)

    suspend fun getAllHadithsForABook(bookId: Int, chapterId: Int): List<HadithEntity> =
        hadithDao.getAllHadithsForABook(bookId, chapterId)

    suspend fun updateFavouriteStatus(id: Int, favourite: Boolean) =
        hadithDao.updateFavouriteStatus(id, favourite)

    //getAllFavourites
    suspend fun getAllFavourites(): List<HadithFavourite> = hadithDao.getAllFavourites()

    suspend fun getAllCategories() = categoryDao.getAllCategories()

    //update tasbih
    fun updateTasbih(tasbih: LocalTasbih) =
        tasbihTrackerDao.updateTasbih(tasbih.id, tasbih.count)

    //update tasbih goal
    fun updateTasbihGoal(tasbih: LocalTasbih) =
        tasbihTrackerDao.updateTasbihGoal(tasbih.id, tasbih.goal)

    //save a tasbih to the database
    fun saveTasbih(tasbih: LocalTasbih) = tasbihTrackerDao.saveTasbih(tasbih)

    //getTasbihById
    fun getTasbihById(id: Int) = tasbihTrackerDao.getTasbihById(id)

    //get all the tasbih
    fun getAllTasbih() = tasbihTrackerDao.getAll()

    //get all the tasbih for a specific date
    fun getTasbihForDate(date: LocalDate) =
        tasbihTrackerDao.getForDate(date)

    //delete a tasbih from the database
    fun deleteTasbih(tasbih: LocalTasbih) =
        tasbihTrackerDao.deleteTasbih(tasbih)


    //get trtacker for a specific date
    suspend fun getTrackerForDate(date: LocalDate) =
        prayerTrackerDao.getTrackerForDate(date)


    fun getTrackersForMonth(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<LocalPrayersTracker>> =
        prayerTrackerDao.getTrackersForMonth(startDate, endDate)

    fun getTrackersForWeek(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<LocalPrayersTracker>> =
        prayerTrackerDao.getTrackersForWeek(startDate, endDate)

    suspend fun updateIsMenstruating(date: LocalDate, isMenstruating: Boolean) {
        fastTrackerDao.updateIsMenstruating(date, isMenstruating)
        prayerTrackerDao.updateMenstruationStatus(date, isMenstruating)
    }


    //get all the trackers
    suspend fun getAllTrackers() = prayerTrackerDao.getAllTrackers()

    //save a tracker
    suspend fun saveTracker(tracker: LocalPrayersTracker) =
        prayerTrackerDao.saveTracker(tracker)

    //update a tracker
    suspend fun updateTracker(tracker: LocalPrayersTracker) =
        prayerTrackerDao.updateTracker(tracker)

    //updateSpecificPrayer
    suspend fun updateSpecificPrayer(date: LocalDate, prayerName: String, prayerDone: Boolean) =
        prayerTrackerDao.updateSpecificPrayer(date, prayerName, prayerDone)

    fun getPrayersForDate(date: LocalDate) = prayerTrackerDao.getPrayersForDate(date)


    //check if a tracker exists
    suspend fun checkIfTrackerExists(date: LocalDate) = prayerTrackerDao.trackerExistsForDate(date)

    //fasting tracker

    //get tracker for a specific date
    suspend fun getFastTrackerForDate(date: LocalDate) =
        fastTrackerDao.getFastTrackerForDate(date)

    fun getFastTrackersForMonth(
        firstDay: LocalDate,
        lastDay: LocalDate
    ): Flow<List<LocalFastTracker>> =
        fastTrackerDao.getFastTrackersForMonth(firstDay, lastDay)

    fun isFastingForDate(date: LocalDate) = fastTrackerDao.isFastingForDate(date)

    //save a tracker
    suspend fun saveFastTracker(tracker: LocalFastTracker) =
        fastTrackerDao.saveFastTracker(tracker)

    //update a tracker
    suspend fun updateFastTracker(tracker: LocalFastTracker) =
        fastTrackerDao.updateFastTracker(tracker)

    suspend fun fastTrackerExistsForDate(date: LocalDate) =
        fastTrackerDao.fastTrackerExistsForDate(date)


    //get all the ayas of a surah
    fun getAyasOfSurah(surahNumber: Int) =
        ayaDao.getAyasOfSurah(surahNumber)

    //get all the ayas of a juz
    suspend fun getAyasOfJuz(juzNumber: Int) =
        ayaDao.getAyasOfJuz(juzNumber)

    //getRandomAya
    suspend fun getRandomAya() = ayaDao.getRandomAya()

    //countAllAyas
    suspend fun countAllAyat() = ayaDao.countAllAyas()

    //deleteAllAyas
    suspend fun deleteAllAyat() = ayaDao.deleteAllAyas()

    //bookmark an aya
    suspend fun bookmarkAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        bookmarkAya: Boolean,
    ) =
        ayaDao.bookmarkAya(ayaNumber, surahNumber, ayaNumberInSurah, bookmarkAya)

    //favorite an aya
    suspend fun favoriteAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        favoriteAya: Boolean,
    ) =
        ayaDao.favoriteAya(ayaNumber, surahNumber, ayaNumberInSurah, favoriteAya)

    //add a note to an aya
    suspend fun addNoteToAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        note: String,
    ) =
        ayaDao.addNoteToAya(ayaNumber, surahNumber, ayaNumberInSurah, note)

    suspend fun getNoteOfAya(ayaNumber: Int, surahNumber: Int, ayaNumberInSurah: Int) =
        ayaDao.getNoteOfAya(ayaNumber, surahNumber, ayaNumberInSurah)

    //get all the bookmarked ayas
    suspend fun getBookmarkedAyas() =
        ayaDao.getBookmarkedAyas()

    //get all the favorited ayas
    suspend fun getFavoritedAyas() =
        ayaDao.getFavoritedAyas()

    //get all the ayas with notes
    suspend fun getAyasWithNotes() =
        ayaDao.getAyasWithNotes()

    suspend fun deleteNoteFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    ) =
        ayaDao.deleteNoteFromAya(ayaNumber, surahNumber, ayaNumberInSurah)

    suspend fun deleteBookmarkFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    ) =
        ayaDao.deleteBookmarkFromAya(ayaNumber, surahNumber, ayaNumberInSurah)

    suspend fun deleteFavoriteFromAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
    ) =
        ayaDao.deleteFavoriteFromAya(ayaNumber, surahNumber, ayaNumberInSurah)

    //addAudioToAya
    suspend fun addAudioToAya(
        surahNumber: Int,
        ayaNumberInSurah: Int,
        audio: String,
    ) =
        ayaDao.addAudioToAya(surahNumber, ayaNumberInSurah, audio)

    //get all juz
    suspend fun getAllJuz() = juzDao.getAllJuz()

    suspend fun getJuzById(number: Int) = juzDao.getJuzById(number)

    //get all surah
    fun getAllSurah() = surahDao.getAllSurahs()

    fun getSurahById(number: Int) = surahDao.getSurahById(number)

    //getPrayerTimesForADate
    suspend fun getPrayerTimesForADate(date: String) =
        prayerTimesDao.getPrayerTimesForADate(date)

    //save all prayer times by mapping the Array list of prayer times to local prayer times
    suspend fun saveAllPrayerTimes(prayerTimes: LocalPrayerTimes) =
        prayerTimesDao.insert(prayerTimes)

    //get the count of all prayer times
    suspend fun countPrayerTimes() = prayerTimesDao.count()

    //getChaptersByCategory
    suspend fun getChaptersByCategory(categoryId: Int) =
        duaDao.getChaptersByCategory(categoryId)

    //get duas of a chapter by chapter id
    suspend fun getDuasOfChapter(chapterId: Int) =
        duaDao.getDuasOfChapter(chapterId)

    fun getMenstruatingState(date: LocalDate): Flow<Boolean> {
        return prayerTrackerDao.getMenstruatingState(date)
    }


    suspend fun getFavoriteDuas() = duaDao.getFavoriteDuas()

    fun getFavoriteDuasFlow() = duaDao.getFavoriteDuasFlow()

    suspend fun updateDua(dua: LocalDua) = duaDao.updateDua(dua)

    // Search
    suspend fun searchDuas(query: String) = duaDao.searchDuas(query)

    suspend fun searchDuasAdvanced(
        query: String = "",
        chapterId: Int? = null,
        isFavorite: Int? = null
    ) = duaDao.searchDuasAdvanced(query, chapterId, isFavorite)

    // Single item retrieval
    suspend fun getDuaById(duaId: Int) = duaDao.getDuaById(duaId)

    suspend fun getChapterById(chapterId: Int) = duaDao.getChapterById(chapterId)

    // Related content
    suspend fun getRelatedDuas(chapterId: Int, currentDuaId: Int) =
        duaDao.getRelatedDuas(chapterId, currentDuaId)

    // Bulk operations
    suspend fun deleteAllDuas() = duaDao.deleteAllDuas()

    suspend fun deleteAllChapters() = duaDao.deleteAllChapters()

    // Statistics
    suspend fun countFavoriteDuas() = duaDao.countFavoriteDuas()

    suspend fun countDuasInChapter(chapterId: Int) = duaDao.countDuasInChapter(chapterId)

    // Last accessed
    suspend fun getLastAccessedDua() = duaDao.getLastAccessedDua()

    // Bulk replace operations
    suspend fun replaceDuas(duas: List<LocalDua>) = duaDao.replaceDuas(duas)

    suspend fun replaceChapters(chapters: List<LocalChapter>) = duaDao.replaceChapters(chapters)

    // Random duas
    suspend fun getRandomDua() = duaDao.getRandomDua()

    suspend fun getRandomFavoriteDua() = duaDao.getRandomFavoriteDua()

    // Save operations that were missing from original
    suspend fun saveDuas(duas: List<LocalDua>) = duaDao.saveDuas(duas)

    suspend fun saveChapters(chapters: List<LocalChapter>) = duaDao.saveChapters(chapters)

    // Count operations that were missing from original
    suspend fun countChapters() = duaDao.countChapters()

    suspend fun countDuas() = duaDao.countDuas()

    // Tafsir operations
    suspend fun getTafsirById(id: Long) = tafsirDao.getTafsirById(id)

    suspend fun getTafsirForAya(ayaNumber: Int, editionId: Int) =
        tafsirDao.getTafsirForAya(ayaNumber, editionId)

    suspend fun getTafsirByEdition(editionId: Int) = tafsirDao.getTafsirByEdition(editionId)

    suspend fun getTafsirByLanguage(language: String) = tafsirDao.getTafsirByLanguage(language)

    // TafsirEdition operations
    suspend fun getAllEditions() = tafsirEditionDao.getAllEditions()

    suspend fun getEditionById(id: Int) = tafsirEditionDao.getEditionById(id)

    suspend fun getEditionsByLanguage(language: String) =
        tafsirEditionDao.getEditionsByLanguage(language)

    //getEditionsByLanguageAndAuthor
    suspend fun getEditionsByLanguageAndAuthor(language: String, authorName: String) =
        tafsirEditionDao.getEditionsByLanguageAndAuthor(language, authorName)

    suspend fun getEditionsByAuthor(authorName: String) =
        tafsirEditionDao.getEditionsByAuthor(authorName)

    suspend fun getEditionCount() = tafsirEditionDao.getEditionCount()
}
