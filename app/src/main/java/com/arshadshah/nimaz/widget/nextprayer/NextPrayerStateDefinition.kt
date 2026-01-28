package com.arshadshah.nimaz.widget.nextprayer

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

object NextPrayerStateDefinition : GlanceStateDefinition<NextPrayerWidgetState> {

    private const val DATA_STORE_FILENAME = "next_prayer_widget"

    private val Context.datastore by dataStore(
        DATA_STORE_FILENAME,
        NextPrayerWidgetStateSerializer
    )

    object NextPrayerWidgetStateSerializer : Serializer<NextPrayerWidgetState> {

        override val defaultValue: NextPrayerWidgetState =
            NextPrayerWidgetState.Success(NextPrayerData())

        override suspend fun readFrom(input: InputStream): NextPrayerWidgetState = try {
            Json.decodeFromString(
                NextPrayerWidgetState.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Could not read NextPrayer data: ${e.message}")
        }

        override suspend fun writeTo(t: NextPrayerWidgetState, output: OutputStream) {
            output.use {
                it.write(
                    Json.encodeToString(NextPrayerWidgetState.serializer(), t)
                        .toByteArray()
                )
            }
        }
    }

    override suspend fun getDataStore(
        context: Context,
        fileKey: String,
    ): DataStore<NextPrayerWidgetState> {
        return context.datastore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return context.dataStoreFile(DATA_STORE_FILENAME)
    }
}
