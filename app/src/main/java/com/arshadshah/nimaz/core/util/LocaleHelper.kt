package com.arshadshah.nimaz.core.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Utility for runtime locale switching.
 *
 * HOW TO ADD A NEW LANGUAGE:
 * 1. Create res/values-XX/strings.xml (e.g., values-ar for Arabic)
 * 2. Copy the default strings.xml and translate all string values
 * 3. Add the language code to AppLanguage enum in SettingsViewModel.kt
 * 4. The language will automatically appear in the language picker
 */
object LocaleHelper {

    /**
     * Set the app locale at runtime.
     * @param context Application context
     * @param languageCode BCP 47 language tag (e.g., "en", "ar", "tr")
     */
    fun setLocale(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales = LocaleList.forLanguageTags(languageCode)
        } else {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val config = context.resources.configuration
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}
