package com.arshadshah.nimaz.utils

import android.content.Context
import com.arshadshah.nimaz.constants.AppConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PrivateSharedPreferences @Inject constructor(
    @ApplicationContext context: Context
) {

    private val sharedPreferences = context.getSharedPreferences(
        AppConstants.PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE
    )
    private val editor = sharedPreferences.edit()

    fun saveData(customkey: String, data: String) {
        editor.putString(customkey, data)
        editor.apply()
    }

    fun getData(key: String, s: String): String {
        return sharedPreferences.getString(key, s)!!
    }

    fun saveDataBoolean(customkey: String, data: Boolean) {
        editor.putBoolean(customkey, data)
        editor.apply()
    }

    fun getDataBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun saveDataInt(customkey: String, data: Int) {
        editor.putInt(customkey, data)
        editor.apply()
    }

    fun getDataInt(key: String): Int {
        return sharedPreferences.getInt(key, 0)
    }

    fun saveDataLong(customkey: String, data: Long) {
        editor.putLong(customkey, data)
        editor.apply()
    }

    fun saveDataFloat(customkey: String, data: Float) {
        editor.putFloat(customkey, data)
        editor.apply()
    }

    fun getDataFloat(key: String): Float {
        return sharedPreferences.getFloat(key, 0f)
    }

    fun getDataDouble(key: String, d: Double): Double {
        return sharedPreferences.getString(key, d.toString())!!.toDouble()
    }

    fun saveDataDouble(customkey: String, data: Double) {
        editor.putString(customkey, data.toString())
        editor.apply()
    }

    fun saveIntSet(customkey: String, data: Set<Int>) {
        editor.putStringSet(customkey, data.map { it.toString() }.toSet())
        editor.apply()
    }

    fun getIntSet(key: String, defaultValue: String): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun clearData() {
        editor.clear()
        editor.apply()
    }

    fun removeData(key: String) {
        editor.remove(key)
        editor.apply()
    }

    fun getAllData(): Map<String, *> {
        return sharedPreferences.all
    }

    fun saveDisplaySettings(settings: DisplaySettings) {
        editor.apply {
            putFloat(AppConstants.ARABIC_FONT_SIZE, settings.arabicFontSize)
            putFloat(AppConstants.TRANSLATION_FONT_SIZE, settings.translationFontSize)
            putString(AppConstants.FONT_STYLE, settings.arabicFont)
            putString(AppConstants.TRANSLATION_LANGUAGE, settings.translation)
            putString(AppConstants.PAGE_TYPE, settings.displayMode)
            apply()
        }
    }

    fun getDisplaySettings(): DisplaySettings {
        return DisplaySettings(
            arabicFontSize = getDataFloat(AppConstants.ARABIC_FONT_SIZE).takeIf { it != 0f } ?: 26f,
            translationFontSize = getDataFloat(AppConstants.TRANSLATION_FONT_SIZE).takeIf { it != 0f }
                ?: 16f,
            arabicFont = getData(AppConstants.FONT_STYLE, "Default"),
            translation = getData(AppConstants.TRANSLATION_LANGUAGE, "English"),
            displayMode = getData(AppConstants.PAGE_TYPE, "List")
        )
    }
}

data class DisplaySettings(
    val arabicFontSize: Float = 26f,
    val translationFontSize: Float = 16f,
    val arabicFont: String = "Default",
    val translation: String = "English",
    val displayMode: String = "List"
)