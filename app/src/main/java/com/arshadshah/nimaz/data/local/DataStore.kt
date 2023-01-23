package com.arshadshah.nimaz.data.local

import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import com.arshadshah.nimaz.data.local.models.LocalPrayertime
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.models.Prayertime
import com.arshadshah.nimaz.data.remote.models.Surah
import java.time.LocalDateTime

class DataStore(db : AppDatabase)
{

	private val juzDao = db.juz
	private val surahDao = db.surah
	private val prayerTimesDao = db.prayerTimes

	//get all juz
	suspend fun getAllJuz() = juzDao.getAllJuz().map { it.toJuz() }

	//save all juz by mapping the Array list of juz to local juz
	suspend fun saveAllJuz(juz : ArrayList<Juz>) = juzDao.insert(juz.map { it.toLocalJuz() })

	//get all surah
	fun getAllSurah() = surahDao.getAllSurahs().map { it.toSurah() }

	//save all surah by mapping the Array list of surah to local surah
	fun saveAllSurah(surah : ArrayList<Surah>) = surahDao.insert(surah.map { it.toLocalSurah() })

	//get all prayer times
	suspend fun getAllPrayerTimes() = prayerTimesDao.getPrayerTimes().toPrayerTimes()

	//delete all prayer times
	suspend fun deleteAllPrayerTimes() = prayerTimesDao.deleteAllPrayerTimes()

	//save all prayer times by mapping the Array list of prayer times to local prayer times
	suspend fun saveAllPrayerTimes(prayerTimes : PrayerTimes) =
		prayerTimesDao.insert(prayerTimes.toLocalPrayerTimes())
}

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