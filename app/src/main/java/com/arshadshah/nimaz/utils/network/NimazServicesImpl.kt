package com.arshadshah.nimaz.utils.network

import com.arshadshah.nimaz.constants.AppConstants
import io.ktor.client.request.*

object NimazServicesImpl : NimazService
{

	private val httpClient by lazy {
		KtorClient.getInstance
	}

	override suspend fun getPrayerTimes(mapOfParams : Map<String , String>) : PrayerTimeResponse
	{
		//create a post request with stuff in body and return the response
		return httpClient.post(AppConstants.PRAYER_TIMES_URL) {
			body = mapOfParams
			//set headers for json
			header("Content-Type" , "application/json")
		}
	}

	override suspend fun getSurahs() : ArrayList<SurahResponse>
	{
		//create a get request and return the response
		return httpClient.get(AppConstants.QURAN_SURAH_URL)
	}

	override suspend fun getJuzs() : ArrayList<JuzResponse>
	{
		//create a get request and return the response
		return httpClient.get(AppConstants.QURAN_JUZ_URL)
	}

	override suspend fun getAyaForSurah(
		surahNumber : Int ,
		isEnglish : Boolean ,
									   ) : ArrayList<AyaResponse>
	{
		//create a get request and return the response
		return httpClient.get(AppConstants.QURAN_SURAH_AYAT_URL) {
			parameter("surahNumber" , surahNumber)
			parameter("isEnglish" , isEnglish)
		}
	}

	override suspend fun getAyaForJuz(
		juzNumber : Int ,
		isEnglish : Boolean ,
									 ) : ArrayList<AyaResponse>
	{
		//create a get request and return the response
		return httpClient.get(AppConstants.QURAN_JUZ_AYAT_URL) {
			parameter("juzNumber" , juzNumber)
			parameter("isEnglish" , isEnglish)
		}
	}
}