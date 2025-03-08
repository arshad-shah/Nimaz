package com.arshadshah.nimaz.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.showcaseDataStore: DataStore<Preferences> by preferencesDataStore(name = "showcase_settings")

@Singleton
class ShowcaseDataStore @Inject constructor(
    private val context: Context
) {
    companion object {
        // Keys for different showcase features
        private val CALENDAR_DAY_SHOWCASE_KEY = booleanPreferencesKey("calendar_showcase_shown")

        // Add more showcase keys here as needed
        // private val SOME_OTHER_FEATURE_SHOWCASE_KEY = booleanPreferencesKey("some_other_feature_showcase")
    }

    // Flow to check if calendar day showcase has been shown
    val calendarShowcaseShown: Flow<Boolean> = context.showcaseDataStore.data
        .map { preferences ->
            preferences[CALENDAR_DAY_SHOWCASE_KEY] ?: false
        }

    // Mark calendar day showcase as shown
    suspend fun markCalendarShowcaseAsShown() {
        context.showcaseDataStore.edit { preferences ->
            preferences[CALENDAR_DAY_SHOWCASE_KEY] = true
        }
    }

    // Reset calendar day showcase (for testing or if needed)
    suspend fun resetCalendarShowcase() {
        context.showcaseDataStore.edit { preferences ->
            preferences[CALENDAR_DAY_SHOWCASE_KEY] = false
        }
    }

    // Reset all showcases
    suspend fun resetAllShowcases() {
        context.showcaseDataStore.edit { preferences ->
            preferences[CALENDAR_DAY_SHOWCASE_KEY] = false
            // Add more resets here as more showcases are added
        }
    }
}

// Example Constants (Add to AppConstants.kt)
/*
object AppConstants {
    // Showcase Constants
    const val CALENDAR_DAY_SHOWCASE = "calendar_day_showcase"
    // Add more showcase constants as needed
}
*/