package com.arshadshah.nimaz.widget.prayertracker

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object PrayerTrackerStateDefinition : GlanceStateDefinition<PrayerTrackerWidgetState> {

    private const val DATA_STORE_FILENAME = "prayer_tracker_widget"

    private val Context.datastore by dataStore(
        DATA_STORE_FILENAME,
        PrayerTrackerWidgetStateSerializer
    )

    object PrayerTrackerWidgetStateSerializer : Serializer<PrayerTrackerWidgetState> {

        override val defaultValue: PrayerTrackerWidgetState =
            PrayerTrackerWidgetState.Success(PrayerTrackerData())

        override suspend fun readFrom(input: InputStream): PrayerTrackerWidgetState = try {
            Json.decodeFromString(
                PrayerTrackerWidgetState.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Could not read PrayerTracker data: ${e.message}")
        }

        override suspend fun writeTo(t: PrayerTrackerWidgetState, output: OutputStream) {
            output.use {
                it.write(
                    Json.encodeToString(PrayerTrackerWidgetState.serializer(), t)
                        .toByteArray()
                )
            }
        }
    }

    override suspend fun getDataStore(
        context: Context,
        fileKey: String,
    ): DataStore<PrayerTrackerWidgetState> {
        return context.datastore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return context.dataStoreFile(DATA_STORE_FILENAME)
    }
}
