package com.arshadshah.nimaz.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
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

	//get all the ayas of a surah
	suspend fun getAyasOfSurah(surahNumber : Int ,translationLanguage: String) =
		ayaDao.getAyasOfSurah(surahNumber,translationLanguage).map { it.toAya() }

	//get all the ayas of a juz
	suspend fun getAyasOfJuz(juzNumber : Int, translationLanguage: String) = ayaDao.getAyasOfJuz(juzNumber,translationLanguage).map { it.toAya() }

	//insert all the ayas
	suspend fun insertAyats(aya : List<Aya>) = ayaDao.insert(aya.map { it.toLocalAya() })

	//count the number of ayas
	suspend fun countSurahAyat(surahNumber : Int ,translationLanguage: String) = ayaDao.countSurahAya(surahNumber,translationLanguage)

	suspend fun countJuzAyat(juzNumber : Int, translationLanguage: String) = ayaDao.countJuzAya(juzNumber,translationLanguage)


	//get all juz
	suspend fun getAllJuz() = juzDao.getAllJuz().map { it.toJuz() }

	//save all juz by mapping the Array list of juz to local juz
	suspend fun saveAllJuz(juz : ArrayList<Juz>) = juzDao.insert(juz.map { it.toLocalJuz() })

	suspend fun countJuz() = juzDao.count()

	//get all surah
	fun getAllSurah() = surahDao.getAllSurahs().map { it.toSurah() }

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
		id = 0,
		ayaNumber = ayaNumber ,
		ayaArabic = ayaArabic ,
		translation = translation ,
		ayaType = ayaType ,
		numberOfType = numberOfType ,
		translationLanguage = TranslationLanguage ,
									   )

private fun LocalAya.toAya() = Aya(
		ayaNumber = ayaNumber ,
		ayaArabic = ayaArabic ,
		translation = translation ,
		ayaType = ayaType ,
		numberOfType = numberOfType ,
		TranslationLanguage = translationLanguage ,
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
		arabic_reference = arabic_reference ,
		english_translation = english_translation ,
		english_reference = english_reference ,
									   )

private fun LocalDua.toDua() = Dua(
		_id = _id ,
		chapter_id = chapter_id ,
		favourite = favourite ,
		arabic_dua = arabic_dua ,
		arabic_reference = arabic_reference ,
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