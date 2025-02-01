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
import com.arshadshah.nimaz.data.local.systems.DuaSystem
import com.arshadshah.nimaz.data.local.systems.HadithSystem
import com.arshadshah.nimaz.data.local.systems.PrayerSystem
import com.arshadshah.nimaz.data.local.systems.QuranSystem
import com.arshadshah.nimaz.data.local.systems.TafsirSystem
import com.arshadshah.nimaz.data.local.systems.TasbihSystem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStore @Inject constructor(
    private val hadithSystem: HadithSystem,
    private val tasbihSystem: TasbihSystem,
    private val prayerSystem: PrayerSystem,
    private val quranSystem: QuranSystem,
    private val duaSystem: DuaSystem,
    private val tafsirSystem: TafsirSystem
) {
    // Hadith Operations
    suspend fun getAllMetadata(): List<HadithMetadata> = hadithSystem.getAllMetadata()
    suspend fun getAllHadithChaptersForABook(bookId: Int): List<HadithChapter> =
        hadithSystem.getAllHadithChaptersForABook(bookId)

    suspend fun getAllHadithsForABook(bookId: Int, chapterId: Int): List<HadithEntity> =
        hadithSystem.getAllHadithsForABook(bookId, chapterId)

    suspend fun updateFavouriteStatus(id: Int, favourite: Boolean) =
        hadithSystem.updateFavouriteStatus(id, favourite)

    suspend fun getAllFavourites(): List<HadithFavourite> = hadithSystem.getAllFavourites()
    suspend fun getAllCategories() = hadithSystem.getAllCategories()

    // Tasbih Operations
    fun updateTasbih(tasbih: LocalTasbih) = tasbihSystem.updateTasbih(tasbih)
    fun updateTasbihGoal(tasbih: LocalTasbih) = tasbihSystem.updateTasbihGoal(tasbih)
    fun saveTasbih(tasbih: LocalTasbih) = tasbihSystem.saveTasbih(tasbih)
    fun getTasbihById(id: Int) = tasbihSystem.getTasbihById(id)
    fun getAllTasbih() = tasbihSystem.getAllTasbih()
    fun getTasbihForDate(date: LocalDate) = tasbihSystem.getTasbihForDate(date)
    fun deleteTasbih(tasbih: LocalTasbih) = tasbihSystem.deleteTasbih(tasbih)

    // Prayer & Fast Operations
    suspend fun getTrackerForDate(date: LocalDate) = prayerSystem.getTrackerForDate(date)
    fun getTrackersForMonth(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<LocalPrayersTracker>> = prayerSystem.getTrackersForMonth(startDate, endDate)

    fun getTrackersForWeek(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<LocalPrayersTracker>> = prayerSystem.getTrackersForWeek(startDate, endDate)

    suspend fun updateIsMenstruating(date: LocalDate, isMenstruating: Boolean) {
        prayerSystem.updateIsMenstruating(date, isMenstruating)
    }

    suspend fun getAllTrackers() = prayerSystem.getAllTrackers()
    suspend fun saveTracker(tracker: LocalPrayersTracker) = prayerSystem.saveTracker(tracker)
    suspend fun updateTracker(tracker: LocalPrayersTracker) = prayerSystem.updateTracker(tracker)
    suspend fun updateSpecificPrayer(date: LocalDate, prayerName: String, prayerDone: Boolean) =
        prayerSystem.updateSpecificPrayer(date, prayerName, prayerDone)

    fun getPrayersForDate(date: LocalDate) = prayerSystem.getPrayersForDate(date)
    suspend fun checkIfTrackerExists(date: LocalDate) = prayerSystem.checkIfTrackerExists(date)
    suspend fun getFastTrackerForDate(date: LocalDate) = prayerSystem.getFastTrackerForDate(date)
    fun getFastTrackersForMonth(firstDay: LocalDate, lastDay: LocalDate) =
        prayerSystem.getFastTrackersForMonth(firstDay, lastDay)

    fun isFastingForDate(date: LocalDate) = prayerSystem.isFastingForDate(date)
    suspend fun saveFastTracker(tracker: LocalFastTracker) = prayerSystem.saveFastTracker(tracker)
    suspend fun updateFastTracker(tracker: LocalFastTracker) =
        prayerSystem.updateFastTracker(tracker)

    suspend fun fastTrackerExistsForDate(date: LocalDate) =
        prayerSystem.fastTrackerExistsForDate(date)

    suspend fun getPrayerTimesForADate(date: String) = prayerSystem.getPrayerTimesForADate(date)
    suspend fun saveAllPrayerTimes(prayerTimes: LocalPrayerTimes) =
        prayerSystem.saveAllPrayerTimes(prayerTimes)

    suspend fun countPrayerTimes() = prayerSystem.countPrayerTimes()
    fun getMenstruatingState(date: LocalDate): Flow<Boolean> =
        prayerSystem.getMenstruatingState(date)

    // Quran Operations
    fun getAyasOfSurah(surahNumber: Int) = quranSystem.getAyasOfSurah(surahNumber)
    suspend fun getAyasOfJuz(juzNumber: Int) = quranSystem.getAyasOfJuz(juzNumber)
    suspend fun getRandomAya() = quranSystem.getRandomAya()
    suspend fun countAllAyat() = quranSystem.countAllAyat()
    suspend fun deleteAllAyat() = quranSystem.deleteAllAyat()
    suspend fun bookmarkAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        bookmarkAya: Boolean
    ) =
        quranSystem.bookmarkAya(ayaNumber, surahNumber, ayaNumberInSurah, bookmarkAya)

    suspend fun favoriteAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        favoriteAya: Boolean
    ) =
        quranSystem.favoriteAya(ayaNumber, surahNumber, ayaNumberInSurah, favoriteAya)

    suspend fun addNoteToAya(
        ayaNumber: Int,
        surahNumber: Int,
        ayaNumberInSurah: Int,
        note: String
    ) =
        quranSystem.addNoteToAya(ayaNumber, surahNumber, ayaNumberInSurah, note)

    suspend fun getNoteOfAya(ayaNumber: Int, surahNumber: Int, ayaNumberInSurah: Int) =
        quranSystem.getNoteOfAya(ayaNumber, surahNumber, ayaNumberInSurah)

    suspend fun getBookmarkedAyas() = quranSystem.getBookmarkedAyas()
    suspend fun getFavoritedAyas() = quranSystem.getFavoritedAyas()
    suspend fun getAyasWithNotes() = quranSystem.getAyasWithNotes()
    suspend fun deleteNoteFromAya(ayaNumber: Int, surahNumber: Int, ayaNumberInSurah: Int) =
        quranSystem.deleteNoteFromAya(ayaNumber, surahNumber, ayaNumberInSurah)

    suspend fun deleteBookmarkFromAya(ayaNumber: Int, surahNumber: Int, ayaNumberInSurah: Int) =
        quranSystem.deleteBookmarkFromAya(ayaNumber, surahNumber, ayaNumberInSurah)

    suspend fun deleteFavoriteFromAya(ayaNumber: Int, surahNumber: Int, ayaNumberInSurah: Int) =
        quranSystem.deleteFavoriteFromAya(ayaNumber, surahNumber, ayaNumberInSurah)

    suspend fun addAudioToAya(surahNumber: Int, ayaNumberInSurah: Int, audio: String) =
        quranSystem.addAudioToAya(surahNumber, ayaNumberInSurah, audio)

    suspend fun getAllJuz() = quranSystem.getAllJuz()
    suspend fun getJuzById(number: Int) = quranSystem.getJuzById(number)
    fun getAllSurah() = quranSystem.getAllSurah()
    fun getSurahById(number: Int) = quranSystem.getSurahById(number)

    // Dua Operations
    suspend fun getChaptersByCategory(categoryId: Int) = duaSystem.getChaptersByCategory(categoryId)
    suspend fun getDuasOfChapter(chapterId: Int) = duaSystem.getDuasOfChapter(chapterId)
    suspend fun getFavoriteDuas() = duaSystem.getFavoriteDuas()
    fun getFavoriteDuasFlow() = duaSystem.getFavoriteDuasFlow()
    suspend fun updateDua(dua: LocalDua) = duaSystem.updateDua(dua)
    suspend fun searchDuas(query: String) = duaSystem.searchDuas(query)
    suspend fun searchDuasAdvanced(
        query: String = "",
        chapterId: Int? = null,
        isFavorite: Int? = null
    ) =
        duaSystem.searchDuasAdvanced(query, chapterId, isFavorite)

    suspend fun getDuaById(duaId: Int) = duaSystem.getDuaById(duaId)
    suspend fun getChapterById(chapterId: Int) = duaSystem.getChapterById(chapterId)
    suspend fun getRelatedDuas(chapterId: Int, currentDuaId: Int) =
        duaSystem.getRelatedDuas(chapterId, currentDuaId)

    suspend fun deleteAllDuas() = duaSystem.deleteAllDuas()
    suspend fun deleteAllChapters() = duaSystem.deleteAllChapters()
    suspend fun countFavoriteDuas() = duaSystem.countFavoriteDuas()
    suspend fun countDuasInChapter(chapterId: Int) = duaSystem.countDuasInChapter(chapterId)
    suspend fun getLastAccessedDua() = duaSystem.getLastAccessedDua()
    suspend fun replaceDuas(duas: List<LocalDua>) = duaSystem.replaceDuas(duas)
    suspend fun replaceChapters(chapters: List<LocalChapter>) = duaSystem.replaceChapters(chapters)
    suspend fun getRandomDua() = duaSystem.getRandomDua()
    suspend fun getRandomFavoriteDua() = duaSystem.getRandomFavoriteDua()
    suspend fun saveDuas(duas: List<LocalDua>) = duaSystem.saveDuas(duas)
    suspend fun saveChapters(chapters: List<LocalChapter>) = duaSystem.saveChapters(chapters)
    suspend fun countChapters() = duaSystem.countChapters()
    suspend fun countDuas() = duaSystem.countDuas()

    // Tafsir Operations
    suspend fun getTafsirById(id: Long) = tafsirSystem.getTafsirById(id)
    suspend fun getTafsirForAya(ayaNumber: Int, editionId: Int) =
        tafsirSystem.getTafsirForAya(ayaNumber, editionId)

    suspend fun getTafsirByEdition(editionId: Int) = tafsirSystem.getTafsirByEdition(editionId)
    suspend fun getTafsirByLanguage(language: String) = tafsirSystem.getTafsirByLanguage(language)
    suspend fun getAllEditions() = tafsirSystem.getAllEditions()
    suspend fun getEditionById(id: Int) = tafsirSystem.getEditionById(id)
    suspend fun getEditionsByLanguage(language: String) =
        tafsirSystem.getEditionsByLanguage(language)

    suspend fun getEditionsByLanguageAndAuthor(language: String, authorName: String) =
        tafsirSystem.getEditionsByLanguageAndAuthor(language, authorName)

    suspend fun getEditionsByAuthor(authorName: String) =
        tafsirSystem.getEditionsByAuthor(authorName)

    suspend fun getEditionCount() = tafsirSystem.getEditionCount()
}