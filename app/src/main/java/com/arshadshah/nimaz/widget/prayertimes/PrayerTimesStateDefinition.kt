package com.arshadshah.nimaz.widget.prayertimes

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

object PrayerTimesStateDefinition : GlanceStateDefinition<PrayerTimesWidgetState> {

    private const val DATA_STORE_FILENAME = "prayer_times_widget"

    private val Context.datastore by dataStore(
        DATA_STORE_FILENAME,
        PrayerTimesWidgetStateSerializer
    )

    object PrayerTimesWidgetStateSerializer : Serializer<PrayerTimesWidgetState> {

        override val defaultValue: PrayerTimesWidgetState =
            PrayerTimesWidgetState.Success(PrayerTimesData())

        override suspend fun readFrom(input: InputStream): PrayerTimesWidgetState = try {
            Json.decodeFromString(
                PrayerTimesWidgetState.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Could not read PrayerTimes data: ${e.message}")
        }

        override suspend fun writeTo(t: PrayerTimesWidgetState, output: OutputStream) {
            output.use {
                it.write(
                    Json.encodeToString(PrayerTimesWidgetState.serializer(), t)
                        .toByteArray()
                )
            }
        }
    }

    override suspend fun getDataStore(
        context: Context,
        fileKey: String,
    ): DataStore<PrayerTimesWidgetState> {
        return context.datastore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return context.dataStoreFile(DATA_STORE_FILENAME)
    }
}
