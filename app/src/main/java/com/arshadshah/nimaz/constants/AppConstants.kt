package com.arshadshah.nimaz.constants

object AppConstants {

    const val DATABASE_VERSION = 5

    //prayer names
    const val PRAYER_NAME_FAJR = "Fajr"
    const val PRAYER_NAME_SUNRISE = "Sunrise"
    const val PRAYER_NAME_DHUHR = "Dhuhr"
    const val PRAYER_NAME_ASR = "Asr"
    const val PRAYER_NAME_MAGHRIB = "Maghrib"
    const val PRAYER_NAME_ISHA = "Isha"

    //Preferences file name
    const val PREFERENCES_FILE_NAME = "com.arshadshah.nimaz.SharedPreferences"

    //pref keys
    const val ARABIC_FONT_SIZE = "ArabicFontSize"
    const val TRANSLATION_FONT_SIZE = "TranslationFontSize"
    const val IS_FIRST_INSTALL = "isFirstInstall"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val FAJR_ANGLE = "fajr_angle"
    const val ISHA_ANGLE = "isha_angle"
    const val ISHA_INTERVAL = "isha_interval"
    const val CALCULATION_METHOD = "calculation_method"
    const val MADHAB = "madhab"
    const val HIGH_LATITUDE_RULE = "high_latitude_rule"
    const val FAJR_ADJUSTMENT = "fajr_adjustment"
    const val SUNRISE_ADJUSTMENT = "sunrise_adjustment"
    const val DHUHR_ADJUSTMENT = "dhuhr_adjustment"
    const val ASR_ADJUSTMENT = "asr_adjustment"
    const val MAGHRIB_ADJUSTMENT = "maghrib_adjustment"
    const val ISHA_ADJUSTMENT = "isha_adjustment"
    const val LOCATION_TYPE = "location_type"
    const val LOCATION_INPUT = "location_input"
    const val ALARM_LOCK = "alarmLock"
    const val AUTO_PARAMETERS = "AutoParameters"
    const val PAGE_TYPE = "PageType"
    const val TRANSLATION_LANGUAGE = "Translation"
    const val NOTIFICATION_ALLOWED = "notificationAllowed"
    const val BATTERY_OPTIMIZATION = "batteryOptimization"
    const val CHANNEL_LOCK = "channelLock"
    const val THEME = "theme"
    const val FONT_STYLE = "font_style"
    const val DARK_MODE: String = "darkMode"
    const val RANDOM_AYAT_NUMBER_IN_SURAH_LAST_FETCHED: String =
        "randomAyatNumberInSurahLastFetched"
    const val DEBUG_MODE: String = "debug"


    const val NAMES_OF_ALLAH_VIEWMODEL_KEY = "names_of_allah_viewmodel_key"

    //full quran downloaded
    const val FULL_QURAN_DOWNLOADED = "full_quran_downloaded"

    //codes
    const val APP_UPDATE_REQUEST_CODE = 100

    //notification channel ids
    const val FAJR_CHANNEL_ID = "fajr_channel_id"
    const val SUNRISE_CHANNEL_ID = "sunrise_channel_id"
    const val DHUHR_CHANNEL_ID = "dhuhr_channel_id"
    const val ASR_CHANNEL_ID = "asr_channel_id"
    const val MAGHRIB_CHANNEL_ID = "maghrib_channel_id"
    const val ISHA_CHANNEL_ID = "isha_channel_id"
    const val TEST_CHANNEL_ID = "test_channel_id"
    const val CHANNEL_MISSED_PRAYER_ID = "missed_prayer_channel_id"

    const val FAJR_NOTIFY_ID = 2000
    const val SUNRISE_NOTIFY_ID = 2001
    const val DHUHR_NOTIFY_ID = 2002
    const val ASR_NOTIFY_ID = 2003
    const val MAGHRIB_NOTIFY_ID = 2004
    const val ISHA_NOTIFY_ID = 2005
    const val TEST_NOTIFY_ID = 2006
    const val MISSED_PRAYER_NOTIFY_ID = 2007

    const val FAJR_PI_REQUEST_CODE = 1000
    const val SUNRISE_PI_REQUEST_CODE = 1001
    const val DHUHR_PI_REQUEST_CODE = 1002
    const val ASR_PI_REQUEST_CODE = 1003
    const val MAGHRIB_PI_REQUEST_CODE = 1004
    const val ISHA_PI_REQUEST_CODE = 1005
    const val TEST_PI_REQUEST_CODE = 1006

    //channels titles
    const val CHANNEL_FAJR = "Fajr"
    const val CHANNEL_SUNRISE = "Sunrise"
    const val CHANNEL_ZUHAR = "Dhuhr"
    const val CHANNEL_ASAR = "Asar"
    const val CHANNEL_MAGHRIB = "Maghrib"
    const val CHANNEL_ISHAA = "Ishaa"
    const val CHANNEL_TEST = "Test"
    const val CHANNEL_MISSED_PRAYER = "Missed Prayer"

    //channel descriptions
    private const val CHANNEL_DESC = "Prayer Adhan Notification"
    const val CHANNEL_DESC_FAJR = "Fajr $CHANNEL_DESC"
    const val CHANNEL_DESC_SUNRISE = "Sunrise $CHANNEL_DESC"
    const val CHANNEL_DESC_ZUHAR = "Zuhar $CHANNEL_DESC"
    const val CHANNEL_DESC_ASAR = "Asar $CHANNEL_DESC"
    const val CHANNEL_DESC_MAGHRIB = "Maghrib $CHANNEL_DESC"
    const val CHANNEL_DESC_ISHAA = "Ishaa $CHANNEL_DESC"
    const val CHANNEL_DESC_TEST = "Test $CHANNEL_DESC"
    const val CHANNEL_DESC_MISSED_PRAYER = "Missed Prayer Notification"


    //notification pending intent request codes
    const val NOTIFICATION_PENDING_INTENT_REQUEST_CODE = 8

    const val PRAYER_COMPLETED_PENDING_INTENT_REQUEST_CODE = 12


    // bottom nav item titles
    const val PRAYER_TIMES_SCREEN_TITLE = "Prayer"
    const val DASHBOARD_SCREEN_TITLE = "Home"
    const val QURAN_SCREEN_TITLE = "Quran"
    const val MORE_SCREEN_TITLE = "More"
    const val SETTINGS_SCREEN_TITLE = "Settings"


    //bottom nav item routes
    const val PRAYER_TIMES_SCREEN_ROUTE = "prayer_times_screen"
    const val QIBLA_SCREEN_ROUTE = "qibla_screen"
    const val QURAN_SCREEN_ROUTE = "quran_screen"
    const val QURAN_AYA_SCREEN_ROUTE = "ayatScreen/{number}/{isSurah}/{language}"
    const val MY_QURAN_SCREEN_ROUTE = "ayatScreen/{number}/{isSurah}/{language}/{scrollTo}"
    const val MORE_SCREEN_ROUTE = "more_screen"
    const val SHAHADAH_SCREEN_ROUTE = "shahadah_screen"

    const val CATEGORY_SCREEN_ROUTE = "category_screen"
    const val CHAPTERS_SCREEN_ROUTE = "chapters_screen/{title}/{id}"
    const val CHAPTER_SCREEN_ROUTE = "chapter_screen/{chapterId}/{categoryName}"
    const val TASBIH_SCREEN_ROUTE = "tasbih_screen/{id}/{arabic}/{translation}/{transliteration}"
    const val NAMESOFALLAH_SCREEN_ROUTE = "namesofallah_screen"

    const val SETTINGS_SCREEN_ROUTE = "settings_screen"
    const val PRAYER_TIMES_SETTINGS_SCREEN_ROUTE = "prayer_times_settings_screen"
    const val ABOUT_SCREEN_ROUTE = "about_screen"
    const val PRAYER_TRACKER_SCREEN_ROUTE = "prayer_tracker_screen"
    const val CALENDER_SCREEN_ROUTE: String = "calendar_screen"
    const val DASHBOARD_SCREEN = "dashboard_screen"
    const val TASBIH_LIST_SCREEN = "tasbih_list_screen"
    const val WEB_VIEW_SCREEN_ROUTE = "web_view_screen/{url}"
    const val LICENCES_SCREEN_ROUTE: String = "Licences"
    const val HADITH_SHELF_SCREEN_ROUTE = "hadith_shelf_screen"
    const val HADITH_CHAPTERS_LIST_SCREEN_ROUTE = "hadith_chapters_list_screen/{bookId}/{bookName}"
    const val HADITH_LIST_SCREEN_ROUTE = "hadith_list_screen/{bookId}/{chapterId}"

    //Test tags
    //navigation tags
    const val TEST_TAG_HOME = "Home"
    const val TEST_TAG_QIBLA = "Qibla"
    const val TEST_TAG_QURAN = "Quran"
    const val TEST_TAG_QURAN_JUZ = "QURAN JUZ"

    const val TEST_TAG_MORE = "More"


    const val TEST_TAG_PRAYER_TRACKER = "Prayer Tracker"
    const val TEST_TAG_CALENDER = "Calender"
    const val TEST_TAG_TASBIH_LIST = "Tasbih List"
    const val TEST_TAG_TASBIH = "Tasbih"
    const val TEST_TAG_NAMES_OF_ALLAH = "Names of Allah"
    const val TEST_TAG_CHAPTERS = "Chapters"
    const val TEST_TAG_CHAPTER = "Chapter"


    //logging tags
    private const val TAG = "Nimaz"

    //main activity tag
    const val MAIN_ACTIVITY_TAG = "$TAG: MainActivity"

    //Quran screen tag
    const val QURAN_SCREEN_TAG = "$TAG: QuranScreen"

    //notification Tag
    const val NOTIFICATION_TAG = "$TAG: Notification"

    //BootReciever
    const val BOOT_RECEIVER_TAG = "$TAG: BootReciever"

    //AdhanReciever
    const val ADHAN_RECEIVER_TAG = "$TAG: AdhanReciever"

    //MissedPrayerReciever
    const val MISSED_PRAYER_RECEIVER_TAG = "$TAG: MissedPrayerReciever"

    //themes names
    const val THEME_SYSTEM = "SYSTEM"
    const val THEME_DEFAULT = "DEFAULT"
    const val THEME_RAISIN_BLACK = "Raisin_Black"
    const val THEME_DARK_RED = "Dark_Red"
    const val THEME_RUSTIC_BROWN = "Rustic_brown"


    //function to return the map of methods
    fun getMethods(): Map<String, String> {
        val mapOfMethods = mutableMapOf<String, String>()
        mapOfMethods["MWL"] = "Muslim World League"
        mapOfMethods["EGYPTIAN"] = "Egyptian General Authority of Survey"
        mapOfMethods["KARACHI"] = "University of Islamic Sciences, Karachi"
        mapOfMethods["MAKKAH"] = "Umm al-Qura University, Makkah"
        mapOfMethods["DUBAI"] = "Dubai"
        mapOfMethods["ISNA"] = "Islamic Society of North America (ISNA)"
        mapOfMethods["KUWAIT"] = "Kuwait"
        mapOfMethods["TEHRAN"] = "Institute of Geophysics, University of Tehran"
        mapOfMethods["SHIA"] = "Shia Ithna Ashari, Leva Institute, Qum"
        mapOfMethods["GULF"] = "Gulf Region"
        mapOfMethods["QATAR"] = "Qatar"
        mapOfMethods["SINGAPORE"] = "Singapore"
        mapOfMethods["FRANCE"] = "France"
        mapOfMethods["TURKEY"] = "Turkey"
        mapOfMethods["RUSSIA"] = "Russia"
        mapOfMethods["MOONSIGHTING"] = "Moonsighting Committee"
        mapOfMethods["IRELAND"] = "Ireland"
        mapOfMethods["OTHER"] = "Other"
        return mapOfMethods
    }

    //get the default parameters for the prayer times method
    fun getDefaultParametersForMethod(method: String): Map<String, String> {
        val mapOfDefaultParameters = mutableMapOf<String, String>()
        when (method) {
            "MWL" -> {
                mapOfDefaultParameters["fajrAngle"] = "18"
                mapOfDefaultParameters["ishaAngle"] = "17"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "EGYPTIAN" -> {
                mapOfDefaultParameters["fajrAngle"] = "19"
                mapOfDefaultParameters["ishaAngle"] = "17"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "KARACHI" -> {
                mapOfDefaultParameters["fajrAngle"] = "18"
                mapOfDefaultParameters["ishaAngle"] = "18"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "MAKKAH" -> {
                mapOfDefaultParameters["fajrAngle"] = "18"
                mapOfDefaultParameters["ishaAngle"] = "0"
                mapOfDefaultParameters["ishaInterval"] = "90"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "DUBAI" -> {
                mapOfDefaultParameters["fajrAngle"] = "18"
                mapOfDefaultParameters["ishaAngle"] = "18"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "ISNA" -> {
                mapOfDefaultParameters["fajrAngle"] = "15"
                mapOfDefaultParameters["ishaAngle"] = "15"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "KUWAIT" -> {
                mapOfDefaultParameters["fajrAngle"] = "18"
                mapOfDefaultParameters["ishaAngle"] = "17"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "TEHRAN" -> {
                mapOfDefaultParameters["fajrAngle"] = "17"
                mapOfDefaultParameters["ishaAngle"] = "14"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "SHIA" -> {
                mapOfDefaultParameters["fajrAngle"] = "16"
                mapOfDefaultParameters["ishaAngle"] = "14"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "GULF" -> {
                mapOfDefaultParameters["fajrAngle"] = "19"
                mapOfDefaultParameters["ishaAngle"] = "0"
                mapOfDefaultParameters["ishaInterval"] = "90"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "QATAR" -> {
                mapOfDefaultParameters["fajrAngle"] = "18"
                mapOfDefaultParameters["ishaAngle"] = "0"
                mapOfDefaultParameters["ishaInterval"] = "90"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "SINGAPORE" -> {
                mapOfDefaultParameters["fajrAngle"] = "20"
                mapOfDefaultParameters["ishaAngle"] = "18"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "FRANCE" -> {
                mapOfDefaultParameters["fajrAngle"] = "12"
                mapOfDefaultParameters["ishaAngle"] = "12"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "TURKEY" -> {
                mapOfDefaultParameters["fajrAngle"] = "18"
                mapOfDefaultParameters["ishaAngle"] = "17"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "RUSSIA" -> {
                mapOfDefaultParameters["fajrAngle"] = "16"
                mapOfDefaultParameters["ishaAngle"] = "15"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "MOONSIGHTING" -> {
                mapOfDefaultParameters["fajrAngle"] = "18"
                mapOfDefaultParameters["ishaAngle"] = "18"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

            "IRELAND" -> {
                mapOfDefaultParameters["fajrAngle"] = "15"
                mapOfDefaultParameters["ishaAngle"] = "15"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "-3"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "2"
                mapOfDefaultParameters["asrAdjustment"] = "1"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-3"
            }

            "OTHER" -> {
                mapOfDefaultParameters["fajrAngle"] = "0"
                mapOfDefaultParameters["ishaAngle"] = "0"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "0"
                mapOfDefaultParameters["sunriseAdjustment"] = "0"
                mapOfDefaultParameters["dhuhrAdjustment"] = "0"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "0"
                mapOfDefaultParameters["ishaAdjustment"] = "0"
            }

            else -> {
                mapOfDefaultParameters["fajrAngle"] = "18"
                mapOfDefaultParameters["ishaAngle"] = "17"
                mapOfDefaultParameters["ishaInterval"] = "0"
                mapOfDefaultParameters["madhab"] = "SHAFI"
                mapOfDefaultParameters["highLatitudeRule"] = "TWILIGHT_ANGLE"
                mapOfDefaultParameters["fajrAdjustment"] = "2"
                mapOfDefaultParameters["sunriseAdjustment"] = "-1"
                mapOfDefaultParameters["dhuhrAdjustment"] = "5"
                mapOfDefaultParameters["asrAdjustment"] = "0"
                mapOfDefaultParameters["maghribAdjustment"] = "2"
                mapOfDefaultParameters["ishaAdjustment"] = "-1"
            }

        }
        return mapOfDefaultParameters
    }

    //function to return the map
    fun getAsrJuristic(): Map<String, String> {
        val mapOfAsrJuristic = mutableMapOf<String, String>()
        mapOfAsrJuristic["SHAFI"] = "Standard"
        mapOfAsrJuristic["HANAFI"] = "Hanafi"
        return mapOfAsrJuristic
    }

    //function to return the map of high latitudes
    fun getHighLatitudes(): Map<String, String> {
        val mapOfHighLatitudes = mutableMapOf<String, String>()
        mapOfHighLatitudes["MIDDLE_OF_THE_NIGHT"] = "Midnight"
        mapOfHighLatitudes["SEVENTH_OF_THE_NIGHT"] = "One Seventh"
        mapOfHighLatitudes["TWILIGHT_ANGLE"] = "Angle Based"
        return mapOfHighLatitudes
    }
}