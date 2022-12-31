package com.arshadshah.nimaz.data.local

import com.arshadshah.nimaz.data.local.models.*
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.models.Surah

class DataStore(db: AppDatabase) {
    private val ayaDao = db.aya
    private val juzDao = db.juz
    private val surahDao = db.surah
    private val prayerTimesDao = db.prayerTimes

    //get all aya
    suspend fun getAllAya(number: Int, type: String) = ayaDao.getAll(number,type).map { it.toAya() }
    //save all aya by mapping the Array list of aya to local aya
    suspend fun saveAllAya(aya: ArrayList<Aya>) = ayaDao.insert(aya.map { it.toLocalAya() })

    //get all juz
    suspend fun getAllJuz() = juzDao.getAllJuz().map { it.toJuz() }
    //save all juz by mapping the Array list of juz to local juz
    suspend fun saveAllJuz(juz: ArrayList<Juz>) = juzDao.insert(juz.map { it.toLocalJuz() })

    //get all surah
    suspend fun getAllSurah() = surahDao.getAllSurahs().map { it.toSurah() }
    //save all surah by mapping the Array list of surah to local surah
    suspend fun saveAllSurah(surah: ArrayList<Surah>) = surahDao.insert(surah.map { it.toLocalSurah() })

    //get all prayer times
    suspend fun getAllPrayerTimes() = prayerTimesDao.getPrayerTimes().toPrayerTimes()

    //save all prayer times by mapping the Array list of prayer times to local prayer times
    suspend fun saveAllPrayerTimes(prayerTimes: PrayerTimes) = prayerTimesDao.insert(prayerTimes.toLocalPrayerTimes())
}


//mappers
private fun Aya.toLocalAya() = LocalAya(
    ayaNumber = ayaNumber,
    ayaArabic = ayaArabic,
    translation = translation,
    ayaType = ayaType,
    numberOfType = numberOfType,
)

private fun LocalAya.toAya() = Aya(
    ayaNumber = ayaNumber,
    ayaArabic = ayaArabic,
    translation = translation,
    ayaType = ayaType,
    numberOfType = numberOfType,
)

private fun Juz.toLocalJuz() = LocalJuz(
    number = number,
    name = name,
    tname = tname,
    juzStartAyaInQuran = juzStartAyaInQuran,
)

private fun LocalJuz.toJuz() = Juz(
    number = number,
    name = name,
    tname = tname,
    juzStartAyaInQuran = juzStartAyaInQuran,
)

private fun Surah.toLocalSurah() = LocalSurah(
    number = number,
    numberOfAyahs = numberOfAyahs,
    startAya = startAya,
    name = name,
    englishName = englishName,
    englishNameTranslation = englishNameTranslation,
    revelationType = revelationType,
    revelationOrder = revelationOrder,
    rukus = rukus,
)

private fun LocalSurah.toSurah() = Surah(
    number = number,
    numberOfAyahs = numberOfAyahs,
    startAya = startAya,
    name = name,
    englishName = englishName,
    englishNameTranslation = englishNameTranslation,
    revelationType = revelationType,
    revelationOrder = revelationOrder,
    rukus = rukus,
)

private fun PrayerTimes.toLocalPrayerTimes() = LocalPrayerTimes(
    fajr = fajr,
    sunrise = sunrise,
    dhuhr = dhuhr,
    asr = asr,
    maghrib = maghrib,
    isha = isha,
    nextPrayer =  nextPrayer,
    currentPrayer = currentPrayer,
)

private fun LocalPrayerTimes.toPrayerTimes() = PrayerTimes(
    fajr = fajr,
    sunrise = sunrise,
    dhuhr = dhuhr,
    asr = asr,
    maghrib = maghrib,
    isha = isha,
    nextPrayer =  nextPrayer,
    currentPrayer = currentPrayer,
)