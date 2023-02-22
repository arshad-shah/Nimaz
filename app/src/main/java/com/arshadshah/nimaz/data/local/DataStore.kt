package com.arshadshah.nimaz.data.local

import com.arshadshah.nimaz.data.local.models.*
import com.arshadshah.nimaz.data.remote.models.*
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

	//get trtacker for a specific date
	suspend fun getTrackerForDate(date : String) = prayerTrackerDao.getTrackerForDate(date).toPrayerTracker()

	//get all the trackers
	suspend fun getAllTrackers() = prayerTrackerDao.getAllTrackers().map { it.toPrayerTracker() }

	//save a tracker
	suspend fun saveTracker(tracker : PrayerTracker) = prayerTrackerDao.saveTracker(tracker.toLocalPrayersTracker())

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
	suspend fun getFastTrackerForDate(date : String) = fastTrackerDao.getFastTrackerForDate(date).toFastTracker()

	//get all the trackers
	suspend fun getAllFastTrackers() = fastTrackerDao.getAllFastTrackers().map { it.toFastTracker() }

	//save a tracker
	suspend fun saveFastTracker(tracker : FastTracker) = fastTrackerDao.saveFastTracker(tracker.toLocalFastTracker())

	//update a tracker
	suspend fun updateFastTracker(tracker : FastTracker) =
		fastTrackerDao.updateFastTracker(tracker.toLocalFastTracker())

	//delete a tracker
	suspend fun deleteFastTracker(tracker : FastTracker) =
		fastTrackerDao.deleteFastTracker(tracker.toLocalFastTracker())

	//delete all trackers
	suspend fun deleteFastAllTrackers() = fastTrackerDao.deleteFastAllTrackers()

	suspend fun fastTrackerExistsForDate(date : String) = fastTrackerDao.fastTrackerExistsForDate(date)


	//get all the ayas of a surah
	suspend fun getAyasOfSurah(surahNumber : Int) =
		ayaDao.getAyasOfSurah(surahNumber).map { it.toAya() }

	//get all the ayas of a juz
	suspend fun getAyasOfJuz(juzNumber : Int) =
		ayaDao.getAyasOfJuz(juzNumber).map { it.toAya() }

	//insert all the ayas
	suspend fun insertAyats(aya : List<Aya>) = ayaDao.insert(aya.map { it.toLocalAya() })

	//count the number of ayas
	suspend fun countSurahAyat(
		surahNumber : Int ,
							  ) =
		ayaDao.countSurahAya(surahNumber)

	suspend fun countJuzAyat(juzNumber : Int , translationLanguage : String) =
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
	suspend fun getDuasOfChapter(chapterId : Int) = duaDao.getDuasOfChapter(chapterId).toChapter()

	//count
	suspend fun countChapters() = duaDao.countChapters()

	//count
	suspend fun countDuas() = duaDao.countDuas()

	//save all chapters
	suspend fun saveAllChapters(chapters : ArrayList<Chapter>) =
		duaDao.saveChapters(chapters.map { it.toLocalChapter() })

	//save one chapter
	suspend fun saveChapter(chapter : Chapter) = duaDao.saveDuas(chapter.toLocalChapter())
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
		timeStamp = timestamp.toString() ,
		fajr = fajr.toString() ,
		sunrise = sunrise.toString() ,
		dhuhr = dhuhr.toString() ,
		asr = asr.toString() ,
		maghrib = maghrib.toString() ,
		isha = isha.toString() ,
		nextPrayer = LocalPrayertime(
				nextPrayer !!.name ,
				nextPrayer.time.toString() ,
									) ,
		currentPrayer = LocalPrayertime(
				currentPrayer !!.name ,
				currentPrayer.time.toString() ,
									   ) ,
															   )

private fun LocalPrayerTimes.toPrayerTimes() = PrayerTimes(
		timestamp = LocalDateTime.parse(timeStamp) ,
		fajr = LocalDateTime.parse(fajr) ,
		sunrise = LocalDateTime.parse(sunrise) ,
		dhuhr = LocalDateTime.parse(dhuhr) ,
		asr = LocalDateTime.parse(asr) ,
		maghrib = LocalDateTime.parse(maghrib) ,
		isha = LocalDateTime.parse(isha) ,
		nextPrayer = Prayertime(
				nextPrayer.name ,
				LocalDateTime.parse(nextPrayer.time) ,
							   ) ,
		currentPrayer = Prayertime(
				currentPrayer.name ,
				LocalDateTime.parse(currentPrayer.time) ,
								  ) ,
														  )

//duas
private fun Dua.toLocalDua() = LocalDua(
		_id = _id ,
		chapter_id = chapter_id ,
		favourite = favourite ,
		arabic_dua = arabic_dua ,
		english_translation = english_translation ,
		english_reference = english_reference ,
									   )

private fun LocalDua.toDua() = Dua(
		_id = _id ,
		chapter_id = chapter_id ,
		favourite = favourite ,
		arabic_dua = arabic_dua ,
		english_translation = english_translation ,
		english_reference = english_reference ,
								  )


private fun Chapter.toLocalChapter() = LocalChapter(
		_id = _id ,
		arabic_title = arabic_title ,
		english_title = english_title ,
		duas = duas.map { it.toLocalDua() } ,
												   )

private fun LocalChapter.toChapter() = Chapter(
		_id = _id ,
		arabic_title = arabic_title ,
		english_title = english_title ,
		duas = duas.map { it.toDua() } as ArrayList<Dua> ,
											  )

private fun PrayerTracker.toLocalPrayersTracker() = LocalPrayersTracker(
		date = date ,
		fajr = fajr ,
		dhuhr = dhuhr ,
		asr = asr ,
		maghrib = maghrib ,
		isha = isha,
		progress = progress,
																	 )

private fun LocalPrayersTracker.toPrayerTracker() = PrayerTracker(
		date = date ,
		fajr = fajr ,
		dhuhr = dhuhr ,
		asr = asr ,
		maghrib = maghrib ,
		isha = isha,
		progress = progress,
																	)

//fasting
private fun FastTracker.toLocalFastTracker() = LocalFastTracker(
		date = date ,
		isFasting = isFasting ,
																	   )

private fun LocalFastTracker.toFastTracker() = FastTracker(
		date = date ,
		isFasting = isFasting ,
																	  )