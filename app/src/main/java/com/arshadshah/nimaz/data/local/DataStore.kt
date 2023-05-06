package com.arshadshah.nimaz.data.local

import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.models.Chapter
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.data.remote.models.FastTracker
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.data.remote.models.Tasbih
import java.time.LocalDate
import java.time.LocalDateTime

class DataStore(db : AppDatabase)
{

	private val ayaDao = db.ayaDao
	private val juzDao = db.juz
	private val surahDao = db.surah
	private val prayerTimesDao = db.prayerTimes
	private val duaDao = db.dua
	private val prayerTrackerDao = db.prayersTracker

	//fastTracker
	private val fastTrackerDao = db.fastTracker

	//tasbihTracker
	private val tasbihTrackerDao = db.tasbihTracker


	//update tasbih
	suspend fun updateTasbih(tasbih : Tasbih) =
		tasbihTrackerDao.updateTasbih(tasbih.id , tasbih.count)

	//update tasbih goal
	suspend fun updateTasbihGoal(tasbih : Tasbih) =
		tasbihTrackerDao.updateTasbihGoal(tasbih.id , tasbih.goal)

	//save a tasbih to the database
	suspend fun saveTasbih(tasbih : Tasbih) = tasbihTrackerDao.saveTasbih(tasbih.toLocalTasbih())

	//getTasbihById
	suspend fun getTasbihById(id : Int) = tasbihTrackerDao.getTasbihById(id).toTasbih()

	//getLatestTasbih
	suspend fun getLatestTasbih() = tasbihTrackerDao.getLatestTasbih().toTasbih()

	//get all the tasbih
	suspend fun getAllTasbih() = tasbihTrackerDao.getAll().map { it.toTasbih() }

	//get all the tasbih for a specific date
	suspend fun getTasbihForDate(date : String) =
		tasbihTrackerDao.getForDate(date).map { it.toTasbih() }

	//get all the tasbih that are completed
	suspend fun getCompletedTasbih() = tasbihTrackerDao.getCompleted().map { it.toTasbih() }

	//get all the tasbih that are not completed
	suspend fun getNotCompletedTasbih() = tasbihTrackerDao.getNotCompleted().map { it.toTasbih() }

	//get all the tasbih that are completed today
	suspend fun getCompletedTasbihToday(date : String) =
		tasbihTrackerDao.getCompletedToday(date).map { it.toTasbih() }

	//get all the tasbih that are not completed today
	suspend fun getNotCompletedTasbihToday(date : String) =
		tasbihTrackerDao.getNotCompletedToday(date).map { it.toTasbih() }

	//delete a tasbih from the database
	suspend fun deleteTasbih(tasbih : Tasbih) =
		tasbihTrackerDao.deleteTasbih(tasbih.toLocalTasbih())


	//get trtacker for a specific date
	suspend fun getTrackerForDate(date : String) =
		prayerTrackerDao.getTrackerForDate(date).toPrayerTracker()

	//get all the trackers
	suspend fun getAllTrackers() = prayerTrackerDao.getAllTrackers().map { it.toPrayerTracker() }

	//save a tracker
	suspend fun saveTracker(tracker : PrayerTracker) =
		prayerTrackerDao.saveTracker(tracker.toLocalPrayersTracker())

	//update a tracker
	suspend fun updateTracker(tracker : PrayerTracker) =
		prayerTrackerDao.updateTracker(tracker.toLocalPrayersTracker())

	//delete a tracker
	suspend fun deleteTracker(tracker : PrayerTracker) =
		prayerTrackerDao.deleteTracker(tracker.toLocalPrayersTracker())

	//check if a tracker exists
	suspend fun checkIfTrackerExists(date : String) = prayerTrackerDao.trackerExistsForDate(date)
	suspend fun getDatesWithTrackers() = prayerTrackerDao.getDatesWithTrackers()

	suspend fun getProgressForDate(date : String) = prayerTrackerDao.getProgressForDate(date)

	//fasting tracker

	//get tracker for a specific date
	suspend fun getFastTrackerForDate(date : String) =
		fastTrackerDao.getFastTrackerForDate(date).toFastTracker()

	//get all the trackers
	suspend fun getAllFastTrackers() =
		fastTrackerDao.getAllFastTrackers().map { it.toFastTracker() }

	//save a tracker
	suspend fun saveFastTracker(tracker : FastTracker) =
		fastTrackerDao.saveFastTracker(tracker.toLocalFastTracker())

	//update a tracker
	suspend fun updateFastTracker(tracker : FastTracker) =
		fastTrackerDao.updateFastTracker(tracker.toLocalFastTracker())

	//delete a tracker
	suspend fun deleteFastTracker(tracker : FastTracker) =
		fastTrackerDao.deleteFastTracker(tracker.toLocalFastTracker())

	//delete all trackers
	suspend fun deleteFastAllTrackers() = fastTrackerDao.deleteFastAllTrackers()

	suspend fun fastTrackerExistsForDate(date : String) =
		fastTrackerDao.fastTrackerExistsForDate(date)


	//get all the ayas of a surah
	suspend fun getAyasOfSurah(surahNumber : Int) =
		ayaDao.getAyasOfSurah(surahNumber).map { it.toAya() }

	//get all the ayas of a juz
	suspend fun getAyasOfJuz(juzNumber : Int) =
		ayaDao.getAyasOfJuz(juzNumber).map { it.toAya() }

	//insert all the ayas
	suspend fun insertAyats(aya : List<Aya>) = ayaDao.insert(aya.map { it.toLocalAya() })

	//getRandomAya
	suspend fun getRandomAya() = ayaDao.getRandomAya().toAya()

	//getAyatByAyaNumberInSurah
	suspend fun getAyatByAyaNumberInSurah(
		ayaNumberInSurah : Int ,
										 ) =
		ayaDao.getAyatByAyaNumberInSurah(ayaNumberInSurah).toAya()

	//countAllAyas
	suspend fun countAllAyat() = ayaDao.countAllAyas()

	//get allAyas
	suspend fun getAllAyat() = ayaDao.getAllAyas().map { it.toAya() }

	//deleteAllAyas
	suspend fun deleteAllAyat() = ayaDao.deleteAllAyas()

	//count the number of ayas
	suspend fun countSurahAyat(
		surahNumber : Int ,
							  ) =
		ayaDao.countSurahAya(surahNumber)

	suspend fun countJuzAyat(juzNumber : Int) =
		ayaDao.countJuzAya(juzNumber)

	//bookmark an aya
	suspend fun bookmarkAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
		bookmarkAya : Boolean ,
						   ) =
		ayaDao.bookmarkAya(ayaNumber , surahNumber , ayaNumberInSurah , bookmarkAya)

	//favorite an aya
	suspend fun favoriteAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
		favoriteAya : Boolean ,
						   ) =
		ayaDao.favoriteAya(ayaNumber , surahNumber , ayaNumberInSurah , favoriteAya)

	//add a note to an aya
	suspend fun addNoteToAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
		note : String ,
							) =
		ayaDao.addNoteToAya(ayaNumber , surahNumber , ayaNumberInSurah , note)

	suspend fun getNoteOfAya(ayaNumber : Int , surahNumber : Int , ayaNumberInSurah : Int) =
		ayaDao.getNoteOfAya(ayaNumber , surahNumber , ayaNumberInSurah)

	//get all the bookmarked ayas
	suspend fun getBookmarkedAyas() =
		ayaDao.getBookmarkedAyas().map { it.toAya() }

	//get all the favorited ayas
	suspend fun getFavoritedAyas() =
		ayaDao.getFavoritedAyas().map { it.toAya() }

	//get all the ayas with notes
	suspend fun getAyasWithNotes() =
		ayaDao.getAyasWithNotes().map { it.toAya() }

	suspend fun deleteNoteFromAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
								 ) =
		ayaDao.deleteNoteFromAya(ayaNumber , surahNumber , ayaNumberInSurah)

	suspend fun deleteBookmarkFromAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
									 ) =
		ayaDao.deleteBookmarkFromAya(ayaNumber , surahNumber , ayaNumberInSurah)

	suspend fun deleteFavoriteFromAya(
		ayaNumber : Int ,
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
									 ) =
		ayaDao.deleteFavoriteFromAya(ayaNumber , surahNumber , ayaNumberInSurah)

	//addAudioToAya
	suspend fun addAudioToAya(
		surahNumber : Int ,
		ayaNumberInSurah : Int ,
		audio : String ,
							 ) =
		ayaDao.addAudioToAya(surahNumber , ayaNumberInSurah , audio)

	//get all juz
	suspend fun getAllJuz() = juzDao.getAllJuz().map { it.toJuz() }

	suspend fun getJuzById(number : Int) = juzDao.getJuzById(number).toJuz()

	//save all juz by mapping the Array list of juz to local juz
	suspend fun saveAllJuz(juz : ArrayList<Juz>) = juzDao.insert(juz.map { it.toLocalJuz() })

	suspend fun countJuz() = juzDao.count()

	//get all surah
	fun getAllSurah() = surahDao.getAllSurahs().map { it.toSurah() }


	fun getSurahById(number : Int) = surahDao.getSurahById(number).toSurah()

	//save all surah by mapping the Array list of surah to local surah
	fun saveAllSurah(surah : ArrayList<Surah>) = surahDao.insert(surah.map { it.toLocalSurah() })

	fun countSurah() = surahDao.count()

	//get all prayer times
	suspend fun getAllPrayerTimes() = prayerTimesDao.getPrayerTimes().toPrayerTimes()

	//getPrayerTimesForADate
	suspend fun getPrayerTimesForADate(date : String) =
		prayerTimesDao.getPrayerTimesForADate(date).toPrayerTimes()

	//delete all prayer times
	suspend fun deleteAllPrayerTimes() = prayerTimesDao.deleteAllPrayerTimes()

	//save all prayer times by mapping the Array list of prayer times to local prayer times
	suspend fun saveAllPrayerTimes(prayerTimes : PrayerTimes) =
		prayerTimesDao.insert(prayerTimes.toLocalPrayerTimes())

	//get the count of all prayer times
	suspend fun countPrayerTimes() = prayerTimesDao.count()

	//get all the chapters
	suspend fun getAllChapters() = duaDao.getAllChapters().map { it.toChapter() }

	//get duas of a chapter by chapter id
	suspend fun getDuasOfChapter(chapterId : Int) = duaDao.getDuasOfChapter(chapterId).map { it.toDua() }

	//count
	suspend fun countChapters() = duaDao.countChapters()

	//count
	suspend fun countDuas() = duaDao.countDuas()

	//save all chapters
	suspend fun saveAllChapters(chapters : ArrayList<Chapter>) =
		duaDao.saveChapters(chapters.map { it.toLocalChapter() })

	//save all duas
	suspend fun saveAllDuas(duas : ArrayList<Dua>) =
		duaDao.saveDuas(duas.map { it.toLocalDua() })
}

private fun Aya.toLocalAya() = LocalAya(
		ayaNumberInQuran = ayaNumberInQuran ,
		ayaNumber = ayaNumber ,
		ayaArabic = ayaArabic ,
		translationEnglish = ayaTranslationEnglish ,
		translationUrdu = ayaTranslationUrdu ,
		suraNumber = suraNumber ,
		ayaNumberInSurah = ayaNumberInSurah ,
		bookmark = bookmark ,
		favorite = favorite ,
		note = note ,
		audioFileLocation = audioFileLocation ,
		sajda = sajda ,
		sajdaType = sajdaType ,
		ruku = ruku ,
		juzNumber = juzNumber ,
									   )

private fun LocalAya.toAya() = Aya(
		ayaNumberInQuran = ayaNumberInQuran ,
		ayaNumber = ayaNumber ,
		ayaArabic = ayaArabic ,
		ayaTranslationEnglish = translationEnglish ,
		ayaTranslationUrdu = translationUrdu ,
		suraNumber = suraNumber ,
		ayaNumberInSurah = ayaNumberInSurah ,
		bookmark = bookmark ,
		favorite = favorite ,
		note = note ,
		audioFileLocation = audioFileLocation ,
		sajda = sajda ,
		sajdaType = sajdaType ,
		ruku = ruku ,
		juzNumber = juzNumber ,
								  )


private fun Juz.toLocalJuz() = LocalJuz(
		number = number ,
		name = name ,
		tname = tname ,
		juzStartAyaInQuran = juzStartAyaInQuran ,
									   )

private fun LocalJuz.toJuz() = Juz(
		number = number ,
		name = name ,
		tname = tname ,
		juzStartAyaInQuran = juzStartAyaInQuran ,
								  )

private fun Surah.toLocalSurah() = LocalSurah(
		number = number ,
		numberOfAyahs = numberOfAyahs ,
		startAya = startAya ,
		name = name ,
		englishName = englishName ,
		englishNameTranslation = englishNameTranslation ,
		revelationType = revelationType ,
		revelationOrder = revelationOrder ,
		rukus = rukus ,
											 )

private fun LocalSurah.toSurah() = Surah(
		number = number ,
		numberOfAyahs = numberOfAyahs ,
		startAya = startAya ,
		name = name ,
		englishName = englishName ,
		englishNameTranslation = englishNameTranslation ,
		revelationType = revelationType ,
		revelationOrder = revelationOrder ,
		rukus = rukus ,
										)

private fun PrayerTimes.toLocalPrayerTimes() = LocalPrayerTimes(
		date = date.toString() ,
		fajr = fajr.toString() ,
		sunrise = sunrise.toString() ,
		dhuhr = dhuhr.toString() ,
		asr = asr.toString() ,
		maghrib = maghrib.toString() ,
		isha = isha.toString() ,
															   )

private fun LocalPrayerTimes.toPrayerTimes() : PrayerTimes?
{
	val date = LocalDate.parse(date)
	return if (fajr != null && sunrise != null && dhuhr != null && asr != null && maghrib != null && isha != null && date != null)
	{
		PrayerTimes(
				date = date ,
				fajr = LocalDateTime.parse(fajr) ,
				sunrise = LocalDateTime.parse(sunrise) ,
				dhuhr = LocalDateTime.parse(dhuhr) ,
				asr = LocalDateTime.parse(asr) ,
				maghrib = LocalDateTime.parse(maghrib) ,
				isha = LocalDateTime.parse(isha) ,
				   )
	} else
	{
		null
	}
}


//duas
private fun Dua.toLocalDua() = LocalDua(
		_id = _id ,
		chapter_id = chapter_id ,
		favourite = favourite ,
		arabic_dua = arabic_dua ,
		english_translation = english_translation ,
		english_reference = english_reference ,
		category = category ,
		isFavourite = isFavourite ,
									   )

private fun LocalDua.toDua() = Dua(
		_id = _id ,
		chapter_id = chapter_id ,
		favourite = favourite ,
		arabic_dua = arabic_dua ,
		english_translation = english_translation ,
		english_reference = english_reference ,
		category = category ,
		isFavourite = isFavourite ,
								  )


private fun Chapter.toLocalChapter() = LocalChapter(
		_id = _id ,
		arabic_title = arabic_title ,
		english_title = english_title ,
		category = category ,
												   )

private fun LocalChapter.toChapter() = Chapter(
		_id = _id ,
		arabic_title = arabic_title ,
		english_title = english_title ,
		category = category ,
											  )

private fun PrayerTracker.toLocalPrayersTracker() = LocalPrayersTracker(
		date = date ,
		fajr = fajr ,
		dhuhr = dhuhr ,
		asr = asr ,
		maghrib = maghrib ,
		isha = isha ,
		progress = progress ,
		isMenstruating = isMenstruating ,
																	   )

private fun LocalPrayersTracker.toPrayerTracker() = PrayerTracker(
		date = date ,
		fajr = fajr ,
		dhuhr = dhuhr ,
		asr = asr ,
		maghrib = maghrib ,
		isha = isha ,
		progress = progress ,
		isMenstruating = isMenstruating ,
																 )

//fasting
private fun FastTracker.toLocalFastTracker() = LocalFastTracker(
		date = date ,
		isFasting = isFasting ,
		isMenstruating = isMenstruating ,
															   )

private fun LocalFastTracker.toFastTracker() = FastTracker(
		date = date ,
		isFasting = isFasting ,
		isMenstruating = isMenstruating ,
														  )

private fun Tasbih.toLocalTasbih() = LocalTasbih(
		id = id ,
		date = date ,
		arabicName = arabicName ,
		englishName = englishName ,
		translationName = translationName ,
		goal = goal ,
		count = count ,
												)

private fun LocalTasbih.toTasbih() = Tasbih(
		id = id ,
		date = date ,
		arabicName = arabicName ,
		englishName = englishName ,
		translationName = translationName ,
		goal = goal ,
		count = count ,
										   )