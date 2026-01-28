package com.arshadshah.nimaz.widget.hijridate

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

object HijriDateStateDefinition : GlanceStateDefinition<HijriDateWidgetState> {

    private const val DATA_STORE_FILENAME = "hijri_date_widget"

    private val Context.datastore by dataStore(
        DATA_STORE_FILENAME,
        HijriDateWidgetStateSerializer
    )

    object HijriDateWidgetStateSerializer : Serializer<HijriDateWidgetState> {

        override val defaultValue: HijriDateWidgetState =
            HijriDateWidgetState.Success(HijriDateData())

        override suspend fun readFrom(input: InputStream): HijriDateWidgetState = try {
            Json.decodeFromString(
                HijriDateWidgetState.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Could not read HijriDate data: ${e.message}")
        }

        override suspend fun writeTo(t: HijriDateWidgetState, output: OutputStream) {
            output.use {
                it.write(
                    Json.encodeToString(HijriDateWidgetState.serializer(), t)
                        .toByteArray()
                )
            }
        }
    }

    override suspend fun getDataStore(
        context: Context,
        fileKey: String,
    ): DataStore<HijriDateWidgetState> {
        return context.datastore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return context.dataStoreFile(DATA_STORE_FILENAME)
    }
}
