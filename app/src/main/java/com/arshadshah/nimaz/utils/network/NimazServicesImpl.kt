package com.arshadshah.nimaz.utils.network

import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

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
		val response : LoginResponse = httpClient.request(AppConstants.LOGIN_URL) {
			method = HttpMethod.Post
			setBody(mapOfLogin)
			header("Content-Type" , "application/json")
		}.body() !!

		Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG , "login: $response")
		return response
	}

	override suspend fun getPrayerTimes(
		mapOfParams : Map<String , String> ,
									   ) : PrayerTimeResponse
	{
		//create a post request with stuff in body and return the response
		val response : PrayerTimeResponse = httpClient.request(AppConstants.PRAYER_TIMES_URL) {
			method = HttpMethod.Post
			setBody(mapOfParams)
			header("Content-Type" , "application/json")
		}.body() !!

		Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG , "getPrayerTimes: $response")

		return response
	}

	override suspend fun getSurahs() : ArrayList<SurahResponse>
	{
		//create a get request and return the response
		val response : ArrayList<SurahResponse> = httpClient.request(AppConstants.QURAN_SURAH_URL) {
			method = HttpMethod.Get
			header("Content-Type" , "application/json")
		}.body() !!

		Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG , "getSurahs: $response")

		return response
	}

	override suspend fun getJuzs() : ArrayList<JuzResponse>
	{
		//create a get request and return the response
		val response : ArrayList<JuzResponse> = httpClient.request(AppConstants.QURAN_JUZ_URL) {
			method = HttpMethod.Get
			header("Content-Type" , "application/json")
		}.body() !!

		Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG , "getJuzs: $response")

		return response
	}

	override suspend fun getAyaForSurah(
		surahNumber : Int ,
									   ) : Map<String , ArrayList<AyaResponse>>
	{
		val urlEnglish =
			AppConstants.QURAN_SURAH_AYAT_URL.replace("{surahNumber}" , surahNumber.toString())
				.replace("{translationLanguage}" , "ENGLISH")
		//create a get request and return the response
		val responseEnglish : ArrayList<AyaResponse> = httpClient.request(urlEnglish) {
			method = HttpMethod.Get
			header("Content-Type" , "application/json")
		}.body() !!

		//make second request for urdu translation
		val urlUrdu =
			AppConstants.QURAN_SURAH_AYAT_URL.replace("{surahNumber}" , surahNumber.toString())
				.replace("{translationLanguage}" , "URDU")
		//create a get request and return the response
		val responseUrdu : ArrayList<AyaResponse> = httpClient.request(urlUrdu) {
			method = HttpMethod.Get
			header("Content-Type" , "application/json")
		}.body() !!

		//combine the two responses
		val mapOfResponses = mapOf(
				"english" to responseEnglish ,
				"urdu" to responseUrdu
								  )
		Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG , "getAyaForSurah: $mapOfResponses")

		return mapOfResponses
	}

	override suspend fun getAyaForJuz(
		juzNumber : Int ,
									 ) : Map<String , ArrayList<AyaResponse>>
	{
		val urlEnglish =
			AppConstants.QURAN_JUZ_AYAT_URL.replace("{juzNumber}" , juzNumber.toString())
				.replace("{translationLanguage}" , "ENGLISH")
		//create a get request and return the response
		val response : ArrayList<AyaResponse> = httpClient.request(urlEnglish) {
			method = HttpMethod.Get
			header("Content-Type" , "application/json")
		}.body() !!

		//make second request for urdu translation
		val urlUrdu = AppConstants.QURAN_JUZ_AYAT_URL.replace("{juzNumber}" , juzNumber.toString())
			.replace("{translationLanguage}" , "URDU")
		//create a get request and return the response
		val responseUrdu : ArrayList<AyaResponse> = httpClient.request(urlUrdu) {
			method = HttpMethod.Get
			header("Content-Type" , "application/json")
		}.body() !!

		//combine the two responses
		val mapOfResponses = mapOf(
				"english" to response ,
				"urdu" to responseUrdu
								  )



		Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG , "getAyaForJuz: $mapOfResponses")

		return mapOfResponses
	}

	//get all the chapters for duas from api
	override suspend fun getChapters() : ArrayList<ChaptersResponse>
	{
		//create a get request and return the response
		val response : ArrayList<ChaptersResponse> =
			httpClient.request(AppConstants.DUA_CHAPTERS_URL) {
				method = HttpMethod.Get
				header("Content-Type" , "application/json")
			}.body() !!

		Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG , "getChapters: $response")

		return response
	}

	override suspend fun getDuasForChapter(
		chapterId : Int ,
										  ) : ChaptersResponse
	{
		val url = AppConstants.DUA_CHAPTER_URL.replace("{chapterId}" , chapterId.toString())
		//create a get request and return the response
		val response : ChaptersResponse = httpClient.request(url) {
			method = HttpMethod.Get
			header("Content-Type" , "application/json")
		}.body() !!

		Log.d(AppConstants.NIMAZ_SERVICES_IMPL_TAG , "getDuasForChapter: $response")

		return response
	}
}