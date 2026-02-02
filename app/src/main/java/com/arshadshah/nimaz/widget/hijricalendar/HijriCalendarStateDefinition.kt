package com.arshadshah.nimaz.widget.hijricalendar

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

object HijriCalendarStateDefinition : GlanceStateDefinition<HijriCalendarWidgetState> {

    private const val DATA_STORE_FILENAME = "hijri_calendar_widget"

    private val Context.datastore by dataStore(
        DATA_STORE_FILENAME,
        HijriCalendarWidgetStateSerializer
    )

    object HijriCalendarWidgetStateSerializer : Serializer<HijriCalendarWidgetState> {

        override val defaultValue: HijriCalendarWidgetState =
            HijriCalendarWidgetState.Success(HijriCalendarData())

        override suspend fun readFrom(input: InputStream): HijriCalendarWidgetState = try {
            Json.decodeFromString(
                HijriCalendarWidgetState.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Could not read HijriCalendar data: ${e.message}")
        }

        override suspend fun writeTo(t: HijriCalendarWidgetState, output: OutputStream) {
            output.use {
                it.write(
                    Json.encodeToString(HijriCalendarWidgetState.serializer(), t)
                        .toByteArray()
                )
            }
        }
    }

    override suspend fun getDataStore(
        context: Context,
        fileKey: String,
    ): DataStore<HijriCalendarWidgetState> {
        return context.datastore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return context.dataStoreFile(DATA_STORE_FILENAME)
    }
}
