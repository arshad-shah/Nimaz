package com.arshadshah.nimaz.utils.network


interface NimazService
{

	suspend fun getPrayerTimes(mapOfParams : Map<String , String>) : PrayerTimeResponse

	suspend fun getSurahs() : ArrayList<SurahResponse>

	suspend fun getJuzs() : ArrayList<JuzResponse>

	suspend fun getAyaForSurah(surahNumber : Int , isEnglish : Boolean) : ArrayList<AyaResponse>

	suspend fun getAyaForJuz(juzNumber : Int , isEnglish : Boolean) : ArrayList<AyaResponse>
}