package com.arshadshah.nimaz.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// Separate DataStore file: the main "nimaz_preferences" delegate is private to
// PreferencesDataStore and announcements are a self-contained slice of state.
private val Context.announcementDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nimaz_announcements"
)

/**
 * Persists the latest received announcement and the set of permanently
 * dismissed announcement ids.
 */
@Singleton
class AnnouncementLocalDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.announcementDataStore
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val CURRENT = stringPreferencesKey("current_announcement")
        val DISMISSED_IDS = stringSetPreferencesKey("dismissed_announcement_ids")
    }

    val currentAnnouncement: Flow<Announcement?> = dataStore.data.map { prefs ->
        prefs[Keys.CURRENT]?.let(::decode)?.toDomain()
    }

    val dismissedIds: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[Keys.DISMISSED_IDS] ?: emptySet()
    }

    suspend fun setCurrentAnnouncement(announcement: Announcement) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENT] = json.encodeToString(announcement.toEntity())
        }
    }

    /** Records [id] as dismissed and clears the current announcement if it matches. */
    suspend fun dismiss(id: String) {
        dataStore.edit { prefs ->
            prefs[Keys.DISMISSED_IDS] = (prefs[Keys.DISMISSED_IDS] ?: emptySet()) + id
            if (prefs[Keys.CURRENT]?.let(::decode)?.id == id) prefs.remove(Keys.CURRENT)
        }
    }

    private fun decode(raw: String): AnnouncementEntity? =
        runCatching { json.decodeFromString<AnnouncementEntity>(raw) }.getOrNull()
}

/** Storage shape of an [Announcement]; kept separate so the domain model stays serialization-free. */
@Serializable
internal data class AnnouncementEntity(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val ctaLabel: String? = null,
    val route: String? = null,
    val minVersionCode: Int? = null,
    val maxVersionCode: Int? = null,
    val expiresAtMillis: Long? = null,
    val dismissable: Boolean = true,
)

internal fun AnnouncementEntity.toDomain(): Announcement? {
    val announcementType = AnnouncementType.fromKey(type) ?: return null
    return Announcement(
        id = id,
        type = announcementType,
        title = title,
        body = body,
        ctaLabel = ctaLabel,
        route = route,
        minVersionCode = minVersionCode,
        maxVersionCode = maxVersionCode,
        expiresAtMillis = expiresAtMillis,
        dismissable = dismissable,
    )
}

internal fun Announcement.toEntity(): AnnouncementEntity = AnnouncementEntity(
    id = id,
    type = type.key,
    title = title,
    body = body,
    ctaLabel = ctaLabel,
    route = route,
    minVersionCode = minVersionCode,
    maxVersionCode = maxVersionCode,
    expiresAtMillis = expiresAtMillis,
    dismissable = dismissable,
)
