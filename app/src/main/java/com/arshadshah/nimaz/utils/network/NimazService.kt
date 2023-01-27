package com.arshadshah.nimaz.utils.network


interface NimazService
{

	//auth
	suspend fun login(username : String , password : String) : LoginResponse

	suspend fun getPrayerTimes(
		mapOfParams : Map<String , String> ,
							  ) : PrayerTimeResponse

	suspend fun getSurahs() : ArrayList<SurahResponse>

	suspend fun getJuzs() : ArrayList<JuzResponse>

	suspend fun getAyaForSurah(
		surahNumber : Int ,
		language : String ,
							  ) : ArrayList<AyaResponse>

	suspend fun getAyaForJuz(
		juzNumber : Int ,
		language : String ,
							) : ArrayList<AyaResponse>

	//get qibla direction
	suspend fun getQiblaDirection(
		latitude : Double ,
		longitude : Double ,
								 ) : QiblaDirectionResponse

	//get duas chapters
	suspend fun getChapters() : ArrayList<ChaptersResponse>

	//get duas for chapter
	suspend fun getDuasForChapter(
		chapterId : Int ,
								 ) : ChaptersResponse
}