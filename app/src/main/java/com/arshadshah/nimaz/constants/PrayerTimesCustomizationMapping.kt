package com.arshadshah.nimaz.constants

class PrayerTimesCustomizationMapping {
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