package com.arshadshah.nimaz.utils.network

import com.arshadshah.nimaz.constants.AppConstants
import io.ktor.client.request.*

object NimazServicesImpl : NimazService
{

	private val httpClient by lazy {
		KtorClient.getInstance
	}

	override suspend fun login(username : String , password : String) : LoginResponse
	{
		val mapOfLogin = mapOf(
				"username" to username ,
				"password" to password
							  )
		return httpClient.post(AppConstants.LOGIN_URL) {
			body = mapOfLogin
			header("Content-Type" , "application/json")
		}
	}

	override suspend fun getQiblaDirection(
		latitude : Double ,
		longitude : Double ,
										  ) : QiblaDirectionResponse
	{
		return httpClient.get(AppConstants.QIBLA_URL) {
			header("Content-Type" , "application/json")
			parameter("latitude" , latitude)
			parameter("longitude" , longitude)
		}
	}


	override suspend fun getPrayerTimes(
		mapOfParams : Map<String , String> ,
									   ) : PrayerTimeResponse
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
		language : String ,
									   ) : ArrayList<AyaResponse>
	{
		val url =
			AppConstants.QURAN_SURAH_AYAT_URL.replace("{surahNumber}" , surahNumber.toString())
				.replace("{translationLanguage}" , language)
		//create a get request and return the response
		return httpClient.get(url)
	}

	override suspend fun getAyaForJuz(
		juzNumber : Int ,
		language : String ,
									 ) : ArrayList<AyaResponse>
	{
		val url = AppConstants.QURAN_JUZ_AYAT_URL.replace("{juzNumber}" , juzNumber.toString())
			.replace("{translationLanguage}" , language)
		return httpClient.get(url)
	}
}