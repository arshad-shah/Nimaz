package com.arshadshah.nimaz.constants

object AppConstants
{

	private const val BASE_URL = "https://nimazapi.arshadshah.online"

	//    const val BASE_URL = "http://192.168.0.62:8080"
	const val PRAYER_TIMES_URL = "$BASE_URL/prayertimes/"

	//    const val PRAYER_TIMES_BY_METHOD_URL = "$BASE_URL/prayertimes/byMethod/"
	const val QURAN_SURAH_URL = "$BASE_URL/quran/surahs/"
	const val QURAN_JUZ_URL = "$BASE_URL/quran/juzs/"
	const val QURAN_SURAH_AYAT_URL = "$BASE_URL/quran/surah/"
	const val QURAN_JUZ_AYAT_URL = "$BASE_URL/quran/juz/"

	//function to return the map of methods
	fun getMethods() : Map<String , String>
	{
		val mapOfMethods = mutableMapOf<String , String>()
		mapOfMethods["MUSLIM_WORLD_LEAGUE"] = "Muslim World League"
		mapOfMethods["EGYPTIAN"] = "Egyptian General Authority of Survey"
		mapOfMethods["KARACHI"] = "University of Islamic Sciences, Karachi"
		mapOfMethods["UMM_AL_QURA"] = "Umm al-Qura University, Makkah"
		mapOfMethods["DUBAI"] = "Institute of Geophysics, University of Tehran"
		mapOfMethods["MOON_SIGHTING_COMMITTEE"] = "The Moonsighting Committee"
		mapOfMethods["NORTH_AMERICA"] = "North America"
		mapOfMethods["KUWAIT"] = "Kuwait"
		mapOfMethods["QATAR"] = "Qatar"
		mapOfMethods["SINGAPORE"] = "Singapore"
		mapOfMethods["FRANCE"] = "France"
		mapOfMethods["RUSSIA"] = "Russia"
		mapOfMethods["IRELAND"] = "Ireland"
		mapOfMethods["OTHER"] = "Other"

		return mapOfMethods
	}

	//function to return the map
	fun getAsrJuristic() : Map<String , String>
	{
		val mapOfAsrJuristic = mutableMapOf<String , String>()
		mapOfAsrJuristic["SHAFI"] = "Standard"
		mapOfAsrJuristic["HANAFI"] = "Hanafi"
		return mapOfAsrJuristic
	}

	//function to return the map of high latitudes
	fun getHighLatitudes() : Map<String , String>
	{
		val mapOfHighLatitudes = mutableMapOf<String , String>()
		mapOfHighLatitudes["MIDDLE_OF_THE_NIGHT"] = "Midnight"
		mapOfHighLatitudes["SEVENTH_OF_THE_NIGHT"] = "One Seventh"
		mapOfHighLatitudes["TWILIGHT_ANGLE"] = "Angle Based"
		return mapOfHighLatitudes
	}
}