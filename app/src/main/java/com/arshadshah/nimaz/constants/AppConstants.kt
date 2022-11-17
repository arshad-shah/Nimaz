package com.arshadshah.nimaz.constants

object AppConstants {

    const val applicationContext = "com.arshadshah.nimaz"
    const val BASE_URL = "http://178.62.35.217:8080"

    //    const val BASE_URL = "http://192.168.0.62:8080"
    const val PRAYER_TIMES_URL = "$BASE_URL/prayertimes/"
    const val PRAYER_TIMES_BY_METHOD_URL = "$BASE_URL/prayertimes/byMethod/"
    const val QURAN_SURAH_URL = "$BASE_URL/quran/surahs/"
    const val QURAN_JUZ_URL = "$BASE_URL/quran/juzs/"
    const val QURAN_SURAH_AYAT_URL = "$BASE_URL/quran/surah/"
    const val QURAN_JUZ_AYAT_URL = "$BASE_URL/quran/juz/"

    //function to return the list of Methods
    fun getMethods(): List<String> {
        return listOf(
            "MUSLIM_WORLD_LEAGUE",
            "EGYPTIAN",
            "KARACHI",
            "UMM_AL_QURA",
            "DUBAI",
            "MOON_SIGHTING_COMMITTEE",
            "NORTH_AMERICA",
            "KUWAIT",
            "QATAR",
            "SINGAPORE",
            "FRANCE",
            "RUSSIA",
            "IRELAND",
            "OTHER"
        )
    }

    //function to return the List of Madhabs
    fun getMadhabs(): List<String> {
        return listOf(
            "SHAFI",
            "HANAFI",
        )
    }

    //function to return the List of High Latitude Rules
    fun getHighLatitudeRules(): List<String> {
        return listOf(
            "MIDDLE_OF_THE_NIGHT",
            "SEVENTH_OF_THE_NIGHT",
            "TWILIGHT_ANGLE",
        )
    }
}