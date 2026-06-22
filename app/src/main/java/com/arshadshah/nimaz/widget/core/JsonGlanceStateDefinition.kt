package com.arshadshah.nimaz.widget.core

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Base [GlanceStateDefinition] that persists a JSON-serializable widget state to
 * a DataStore file.
 *
 * Every widget package used to declare an almost identical ~60-line state
 * definition (the same serializer boilerplate, the same DataStore wiring). They
 * now extend this class and only supply their file name, serializer, default
 * value and a short label used in corruption messages.
 *
 * @param fileName     DataStore file name (without extension).
 * @param serializer   kotlinx-serialization serializer for the state type.
 * @param defaultValue value returned before anything has been written.
 * @param dataLabel    human-readable label used in [CorruptionException] messages.
 */
abstract class JsonGlanceStateDefinition<T>(
    private val fileName: String,
    serializer: KSerializer<T>,
    defaultValue: T,
    dataLabel: String,
) : GlanceStateDefinition<T> {

    private val itemSerializer = JsonItemSerializer(serializer, defaultValue, dataLabel)

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<T> =
        getOrCreateDataStore(context, fileName, itemSerializer)

    override fun getLocation(context: Context, fileKey: String): File =
        context.dataStoreFile(fileName)

    private class JsonItemSerializer<T>(
        private val serializer: KSerializer<T>,
        override val defaultValue: T,
        private val dataLabel: String,
    ) : Serializer<T> {

        override suspend fun readFrom(input: InputStream): T = try {
            Json.decodeFromString(serializer, input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            CrashReporter.recordException(e)
            throw CorruptionException("Could not read $dataLabel data: ${e.message}")
        }

        override suspend fun writeTo(t: T, output: OutputStream) {
            output.use {
                it.write(Json.encodeToString(serializer, t).toByteArray())
            }
        }
    }

    private companion object {
        // A DataStore must be a process-wide singleton per file; the `by dataStore`
        // delegate guaranteed this previously, so we keep one instance per file name.
        private val dataStores = ConcurrentHashMap<String, DataStore<*>>()

        @Suppress("UNCHECKED_CAST")
        fun <T> getOrCreateDataStore(
            context: Context,
            fileName: String,
            serializer: Serializer<T>,
        ): DataStore<T> = dataStores.getOrPut(fileName) {
            DataStoreFactory.create(serializer = serializer) {
                context.applicationContext.dataStoreFile(fileName)
            }
        } as DataStore<T>
    }
}
