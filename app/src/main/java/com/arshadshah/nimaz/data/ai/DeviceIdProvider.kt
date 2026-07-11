package com.arshadshah.nimaz.data.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// A tiny dedicated DataStore for the AI install id — kept separate from user
// settings so it is never exported/cleared alongside preferences.
private val Context.aiDeviceStore by preferencesDataStore(name = "nimaz_ai_device")

/**
 * Provides a stable, random per-install device id. This is a generated UUID —
 * NEVER a hardware identifier (ANDROID_ID, IMEI, MAC, advertising id) — so it
 * cannot be used to track the physical device. It is generated once on first
 * use and persisted; clearing app data resets it.
 */
@Singleton
class DeviceIdProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("ai_device_id")

    suspend fun getOrCreate(): String {
        val existing = context.aiDeviceStore.data.first()[key]
        if (existing != null) return existing
        val generated = UUID.randomUUID().toString()
        context.aiDeviceStore.edit { it[key] = generated }
        return generated
    }
}
