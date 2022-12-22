package com.arshadshah.nimaz.utils.network

import android.content.Context


interface NimazService
{

	//auth
	suspend fun login(username : String , password : String) : LoginResponse

	suspend fun getPrayerTimes(
		context : Context ,
		mapOfParams : Map<String , String> ,
							  ) : PrayerTimeResponse

	suspend fun getSurahs(context : Context) : ArrayList<SurahResponse>

	suspend fun getJuzs(context : Context) : ArrayList<JuzResponse>

	suspend fun getAyaForSurah(
		context : Context ,
		surahNumber : Int ,
		language : String ,
							  ) : ArrayList<AyaResponse>

	suspend fun getAyaForJuz(
		context : Context ,
		juzNumber : Int ,
		language : String ,
							) : ArrayList<AyaResponse>
}