package com.arshadshah.nimaz.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arshadshah.nimaz.constants.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

@Singleton
class ThemeDataStore @Inject constructor(
    private val context: Context
) {
    private val themeKey = stringPreferencesKey(AppConstants.THEME)
    private val darkModeKey = booleanPreferencesKey(AppConstants.DARK_MODE)

    val themeFlow: Flow<String> = context.themeDataStore.data
        .map { preferences ->
            preferences[themeKey] ?: AppConstants.THEME_SYSTEM
        }

    val darkModeFlow: Flow<Boolean> = context.themeDataStore.data
        .map { preferences ->
            preferences[darkModeKey] ?: false
        }

    suspend fun updateTheme(theme: String) {
        context.themeDataStore.edit { preferences ->
            preferences[themeKey] = theme
        }
    }

    suspend fun updateDarkMode(isDarkMode: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[darkModeKey] = isDarkMode
        }
    }
}