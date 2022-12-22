package com.arshadshah.nimaz.utils.network

import android.content.Context
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import io.ktor.client.request.*

object NimazServicesImpl : NimazService
{

	private val httpClient by lazy {
		KtorClient.getInstance
	}

	//shared preferences instance lateinit
	private lateinit var sharedPrefs : PrivateSharedPreferences

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


	override suspend fun getPrayerTimes(
		context : Context ,
		mapOfParams : Map<String , String> ,
									   ) : PrayerTimeResponse
	{
		//get shared preferences instance
		sharedPrefs = PrivateSharedPreferences.getInstance(context)
		//get token from shared preferences
		val token = sharedPrefs.getData(AppConstants.LOGIN_TOKEN , "")
		//create a post request with stuff in body and return the response
		return httpClient.post(AppConstants.PRAYER_TIMES_URL) {
			body = mapOfParams
			//set headers for json
			header("Content-Type" , "application/json")
			//add auth token
			header("Authorization" , "Bearer $token")
		}
	}

	override suspend fun getSurahs(context : Context) : ArrayList<SurahResponse>
	{
		//get shared preferences instance
		sharedPrefs = PrivateSharedPreferences.getInstance(context)
		//get token from shared preferences
		val token = sharedPrefs.getData(AppConstants.LOGIN_TOKEN , "")
		//create a get request and return the response
		return httpClient.get(AppConstants.QURAN_SURAH_URL) {
			header("Authorization" , "Bearer $token")
		}
	}

	override suspend fun getJuzs(context : Context) : ArrayList<JuzResponse>
	{
		//get shared preferences instance
		sharedPrefs = PrivateSharedPreferences.getInstance(context)
		//get token from shared preferences
		val token = sharedPrefs.getData(AppConstants.LOGIN_TOKEN , "")
		//create a get request and return the response
		return httpClient.get(AppConstants.QURAN_JUZ_URL) {
			header("Authorization" , "Bearer $token")
		}
	}

	override suspend fun getAyaForSurah(
		context : Context ,
		surahNumber : Int ,
		language : String ,
									   ) : ArrayList<AyaResponse>
	{
		//get shared preferences instance
		sharedPrefs = PrivateSharedPreferences.getInstance(context)
		//get token from shared preferences
		val token = sharedPrefs.getData(AppConstants.LOGIN_TOKEN , "")
		val url =
			AppConstants.QURAN_SURAH_AYAT_URL.replace("{surahNumber}" , surahNumber.toString())
				.replace("{translationLanguage}" , language)
		//create a get request and return the response
		return httpClient.get(url) {
			header("Authorization" , "Bearer $token")
		}
	}

	override suspend fun getAyaForJuz(
		context : Context ,
		juzNumber : Int ,
		language : String ,
									 ) : ArrayList<AyaResponse>
	{
		//get shared preferences instance
		sharedPrefs = PrivateSharedPreferences.getInstance(context)
		//get token from shared preferences
		val token = sharedPrefs.getData(AppConstants.LOGIN_TOKEN , "")
		val url = AppConstants.QURAN_JUZ_AYAT_URL.replace("{juzNumber}" , juzNumber.toString())
			.replace("{translationLanguage}" , language)
		return httpClient.get(url) {
			header("Authorization" , "Bearer $token")
		}
	}
}