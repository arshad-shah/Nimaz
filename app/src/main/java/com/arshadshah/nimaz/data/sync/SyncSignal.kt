package com.arshadshah.nimaz.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lightweight signals sent between devices as Payload.fromBytes() messages
 * to coordinate sync state. Each signal is under 1KB when serialized.
 */
@Serializable
sealed class SyncSignal {

    /** Sender → Receiver: "I accepted and am preparing data" */
    @Serializable
    @SerialName("ready")
    data object Ready : SyncSignal()

    /** Either → Either: "I cancelled" */
    @Serializable
    @SerialName("cancel")
    data object Cancel : SyncSignal()

    /** Receiver → Sender: "Starting import" */
    @Serializable
    @SerialName("import_started")
    data object ImportStarted : SyncSignal()

    /** Receiver → Sender: per-category import progress */
    @Serializable
    @SerialName("import_progress")
    data class ImportProgress(
        val step: Int,
        val total: Int,
        val label: String
    ) : SyncSignal()

    /** Receiver → Sender: "All done importing" */
    @Serializable
    @SerialName("import_complete")
    data object ImportComplete : SyncSignal()

    /** Sender → Receiver: "Got it, safe to disconnect" */
    @Serializable
    @SerialName("ack")
    data object Ack : SyncSignal()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun encode(signal: SyncSignal): ByteArray =
            json.encodeToString(serializer(), signal).toByteArray(Charsets.UTF_8)

        fun decode(bytes: ByteArray): SyncSignal? = try {
            json.decodeFromString(serializer(), String(bytes, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }
}
