package com.arshadshah.nimaz.constants

object AppConstants
{

	private const val BASE_URL = "https://nimazapi.arshadshah.online/api"

	const val PRAYER_TIMES_URL = "$BASE_URL/prayertimes/custom"

	//auth
	const val LOGIN_URL = "$BASE_URL/auth/authenticate"

	const val QURAN_SURAH_URL = "$BASE_URL/quran/surahs/"
	const val QURAN_JUZ_URL = "$BASE_URL/quran/juzs/"
	const val QURAN_SURAH_AYAT_URL = "$BASE_URL/quran/surah/{surahNumber}/{translationLanguage}"
	const val QURAN_JUZ_AYAT_URL = "$BASE_URL/quran/juz/{juzNumber}/{translationLanguage}"

	const val QIBLA_URL = "$BASE_URL/prayertimes/qibla"

	const val DUA_CHAPTERS_URL = "$BASE_URL/Duas/chapters"
	const val DUA_CHAPTER_URL = "$BASE_URL/Duas/chapter/{chapterId}"

	const val USER_USERNAME = "user"
	const val USER_PASSWORD = "Thisworldis100%doomed"


	//Preferences file name
	const val PREFERENCES_FILE_NAME = "com.arshadshah.nimaz.SharedPreferences"

	//pref keys
	const val LOGIN_TOKEN = "login_token"
	const val ARABIC_FONT_SIZE = "ArabicFontSize"
	const val TRANSLATION_FONT_SIZE = "TranslationFontSize"
	const val IS_FIRST_INSTALL = "isFirstInstall"
	const val LATITUDE = "latitude"
	const val LONGITUDE = "longitude"
	const val FAJR_ANGLE = "fajr_angle"
	const val ISHA_ANGLE = "isha_angle"
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
	const val RECALCULATE_PRAYER_TIMES = "recalculate_prayer_times"
	const val ALARM_LOCK = "alarmLock"
	const val FAJR = "fajr"
	const val SUNRISE = "sunrise"
	const val DHUHR = "dhuhr"
	const val ASR = "asr"
	const val MAGHRIB = "maghrib"
	const val ISHA = "isha"
	const val CURRENT_PRAYER = "currentPrayer"
	const val PAGE_TYPE = "PageType"
	const val TRANSLATION_LANGUAGE = "Translation"

	//notificationAllowed
	const val NOTIFICATION_ALLOWED = "notificationAllowed"

	//battery_optimization
	const val BATTERY_OPTIMIZATION = "batteryOptimization"

	//channelLock
	const val CHANNEL_LOCK = "channelLock"

	//main activity app update request code
	const val APP_UPDATE_REQUEST_CODE = 100

	// bottom nav item titles
	const val PRAYER_TIMES_SCREEN_TITLE = "Prayer"
	const val QIBLA_SCREEN_TITLE = "Qibla"
	const val QURAN_SCREEN_TITLE = "Quran"
	const val MORE_SCREEN_TITLE = "More"
	const val SETTINGS_SCREEN_TITLE = "Settings"

	//bottom nav item routes
	const val PRAYER_TIMES_SCREEN_ROUTE = "prayer_times_screen"
	const val QIBLA_SCREEN_ROUTE = "qibla_screen"
	const val QURAN_SCREEN_ROUTE = "quran_screen"
	const val QURAN_AYA_SCREEN_ROUTE = "ayatScreen/{number}/{isSurah}/{language}"
	const val MORE_SCREEN_ROUTE = "more_screen"
	const val SHAHADAH_SCREEN_ROUTE = "shahadah_screen"

	const val CHAPTERS_SCREEN_ROUTE = "chapters_screen"
	const val CHAPTER_SCREEN_ROUTE = "chapter_screen/{chapterId}"
	const val TASBIH_SCREEN_ROUTE = "tasbih_screen/{arabic}"
	const val NAMESOFALLAH_SCREEN_ROUTE = "namesofallah_screen"

	const val SETTINGS_SCREEN_ROUTE = "settings_screen"
	const val PRAYER_TIMES_SETTINGS_SCREEN_ROUTE = "prayer_times_settings_screen"
	const val ABOUT_SCREEN_ROUTE = "about_screen"

	//Logging tags

	//logging tags
	const val TAG = "Nimaz"

	//splash screen tag
	const val SPLASH_SCREEN_TAG = "$TAG: SplashScreen"

	//main activity tag
	const val MAIN_ACTIVITY_TAG = "$TAG: MainActivity"

	const val PRAYER_TIMES_SCREEN_TAG = "$TAG: PrayerTimesFragment"

	//Qibla compass screen tag
	const val QIBLA_COMPASS_SCREEN_TAG = "$TAG: QiblaCompassScreen"

	//Quran screen tag
	const val QURAN_SCREEN_TAG = "$TAG: QuranScreen"

	//Quran screen tag
	const val QURAN_SURAH_SCREEN_TAG = "$TAG: QuranSurahScreen"

	//Quran screen tag
	const val QURAN_JUZ_SCREEN_TAG = "$TAG: QuranJuzScreen"

	//error detector tag
	const val ERROR_DETECTOR_TAG = "$TAG: ErrorDetector"

	//notification Tag
	const val NOTIFICATION_TAG = "$TAG: Notification"

	//data store tag
	const val DATA_STORE_TAG = "$TAG: DataStore"

	//ResetAdhansReciever
	const val RESET_ADHANS_RECEIVER_TAG = "$TAG: ResetAdhansReciever"

	//BootReciever
	const val BOOT_RECEIVER_TAG = "$TAG: BootReciever"

	//AdhanReciever
	const val ADHAN_RECEIVER_TAG = "$TAG: AdhanReciever"

	//NimazServicesImpl
	const val NIMAZ_SERVICES_IMPL_TAG = "$TAG: NimazServicesImpl"


	//function to return the map of methods
	fun getMethods() : Map<String , String>
	{
		val mapOfMethods = mutableMapOf<String , String>()
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