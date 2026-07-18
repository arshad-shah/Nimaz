package com.arshadshah.nimaz.data.sync

import android.content.Context
import android.util.Log
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class CancelReason { BY_USER, BY_PARTNER, CONNECTION_LOST }

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Advertising : ConnectionState()
    data object Discovering : ConnectionState()
    data class Connecting(val endpointId: String, val endpointName: String, val authToken: String) :
        ConnectionState()

    data class WaitingForPartnerAccept(val endpointId: String, val endpointName: String) :
        ConnectionState()

    data class Connected(val endpointId: String, val endpointName: String) : ConnectionState()
    data class Transferring(val progress: Float) : ConnectionState()
    data class PartnerImporting(val step: Int, val total: Int, val label: String) :
        ConnectionState()

    data class Completed(val bytesReceived: Long) : ConnectionState()
    data class Cancelled(val reason: CancelReason) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

@Singleton
class NearbyConnectionsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NearbySync"
        private const val SERVICE_ID = "com.arshadshah.nimaz.sync"

        // Max safe size for BYTES payload (leaving margin below 32768)
        private const val MAX_BYTES_PAYLOAD_SIZE = 31_000

        // Prefix byte to distinguish compressed data from signal JSON
        private const val DATA_PREFIX: Byte = 0x1F // same as GZIP magic byte
    }

    private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var connectedEndpointId: String? = null
    private var onDataReceived: ((ByteArray) -> Unit)? = null
    private var onSignalReceived: ((SyncSignal) -> Unit)? = null

    // Track pending FILE payloads so we can read them after transfer completes
    private val pendingPayloads = mutableMapOf<Long, Payload>()

    // Track signal payload IDs (both sent and received) so their transfer
    // updates don't trigger state changes meant for data payloads
    private val signalPayloadIds = mutableSetOf<Long>()

    // Track STREAM payload IDs — their data is read on a background thread
    // in onPayloadReceived, so onPayloadTransferUpdate should not handle them
    private val streamPayloadIds = mutableSetOf<Long>()

    // Temp file used by sender — must survive until transfer completes
    private var pendingSendFile: File? = null

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "onConnectionInitiated: endpoint=$endpointId, name=${info.endpointName}")
            _connectionState.value = ConnectionState.Connecting(
                endpointId = endpointId,
                endpointName = info.endpointName,
                authToken = info.authenticationDigits
            )
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(
                TAG,
                "onConnectionResult: endpoint=$endpointId, status=${result.status.statusCode}, msg=${result.status.statusMessage}"
            )
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectedEndpointId = endpointId
                    val name = when (val current = _connectionState.value) {
                        is ConnectionState.Connecting -> current.endpointName
                        is ConnectionState.WaitingForPartnerAccept -> current.endpointName
                        else -> ""
                    }
                    Log.d(TAG, "Connected successfully to $name ($endpointId)")
                    _connectionState.value = ConnectionState.Connected(endpointId, name)
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected by $endpointId")
                    _connectionState.value = ConnectionState.Error("Connection rejected")
                }

                else -> {
                    Log.d(TAG, "Connection failed: ${result.status.statusMessage}")
                    _connectionState.value =
                        ConnectionState.Error("Connection failed: ${result.status.statusMessage}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(
                TAG,
                "onDisconnected: endpoint=$endpointId, currentState=${_connectionState.value}"
            )
            connectedEndpointId = null
            val current = _connectionState.value
            if (current !is ConnectionState.Completed &&
                current !is ConnectionState.Error &&
                current !is ConnectionState.Cancelled
            ) {
                Log.d(TAG, "Setting Cancelled(CONNECTION_LOST) from state: $current")
                _connectionState.value = ConnectionState.Cancelled(CancelReason.CONNECTION_LOST)
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d(
                TAG,
                "onPayloadReceived: id=${payload.id}, type=${payload.type}, endpoint=$endpointId"
            )
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val bytes = payload.asBytes() ?: run {
                        Log.d(TAG, "BYTES payload ${payload.id}: asBytes() returned null")
                        return
                    }
                    Log.d(
                        TAG,
                        "BYTES payload ${payload.id}: ${bytes.size} bytes, first byte=0x${
                            String.format(
                                "%02X",
                                bytes[0]
                            )
                        }"
                    )

                    // Check for compressed data (prefix byte 0x1F = GZIP magic)
                    if (bytes.isNotEmpty() && bytes[0] == DATA_PREFIX) {
                        Log.d(TAG, "BYTES payload ${payload.id}: detected compressed data")
                        // Skip transfer updates for this data payload
                        signalPayloadIds.add(payload.id)
                        try {
                            val compressed = bytes.copyOfRange(1, bytes.size)
                            val decompressed = gzipDecompress(compressed)
                            Log.d(
                                TAG,
                                "BYTES payload ${payload.id}: decompressed ${compressed.size} → ${decompressed.size} bytes"
                            )
                            onDataReceived?.invoke(decompressed)
                                ?: run {
                                    Log.e(TAG, "BYTES data payload: onDataReceived is null!")
                                    _connectionState.value =
                                        ConnectionState.Error("No data handler registered")
                                }
                        } catch (e: Exception) {
                            CrashReporter.recordException(e)
                            Log.e(TAG, "BYTES data payload: decompression failed", e)
                            _connectionState.value =
                                ConnectionState.Error("Failed to decompress: ${e.message}")
                        }
                        return
                    }

                    // Try to decode as signal
                    val signal = SyncSignal.decode(bytes)
                    if (signal != null) {
                        Log.d(TAG, "Decoded signal: $signal")
                        signalPayloadIds.add(payload.id)
                        onSignalReceived?.invoke(signal)
                            ?: Log.d(TAG, "WARNING: onSignalReceived is null!")
                    } else {
                        Log.d(
                            TAG,
                            "BYTES payload is not a signal or data, raw: ${String(bytes).take(100)}"
                        )
                    }
                }

                Payload.Type.STREAM -> {
                    Log.d(TAG, "STREAM payload ${payload.id}: starting background read thread")
                    streamPayloadIds.add(payload.id)
                    Thread {
                        try {
                            val stream = payload.asStream()
                            Log.d(TAG, "STREAM ${payload.id}: asStream()=${stream != null}")
                            val inputStream = stream?.asInputStream()
                                ?: throw IOException("No input stream available")
                            Log.d(TAG, "STREAM ${payload.id}: reading bytes...")
                            val data = inputStream.use { it.readBytes() }
                            Log.d(TAG, "STREAM ${payload.id}: read ${data.size} bytes")
                            if (data.isEmpty()) {
                                Log.d(TAG, "STREAM ${payload.id}: ERROR - empty data!")
                                _connectionState.value =
                                    ConnectionState.Error("Received empty data stream")
                                return@Thread
                            }
                            Log.d(
                                TAG,
                                "STREAM ${payload.id}: invoking onDataReceived (null=${onDataReceived == null})"
                            )
                            onDataReceived?.invoke(data)
                                ?: run {
                                    Log.d(
                                        TAG,
                                        "STREAM ${payload.id}: ERROR - onDataReceived is null!"
                                    )
                                    _connectionState.value =
                                        ConnectionState.Error("No data handler registered")
                                }
                        } catch (e: Exception) {
                            CrashReporter.recordException(e)
                            Log.e(TAG, "STREAM ${payload.id}: read failed", e)
                            _connectionState.value =
                                ConnectionState.Error("Failed to read data: ${e.message}")
                        }
                    }.start()
                }

                Payload.Type.FILE -> {
                    Log.d(TAG, "FILE payload ${payload.id}: storing as pending")
                    pendingPayloads[payload.id] = payload
                }

                else -> {
                    Log.d(TAG, "Unknown payload type: ${payload.type}")
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val statusName = when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> "IN_PROGRESS"
                PayloadTransferUpdate.Status.SUCCESS -> "SUCCESS"
                PayloadTransferUpdate.Status.FAILURE -> "FAILURE"
                PayloadTransferUpdate.Status.CANCELED -> "CANCELED"
                else -> "UNKNOWN(${update.status})"
            }
            Log.d(
                TAG, "onPayloadTransferUpdate: id=${update.payloadId}, status=$statusName, " +
                        "bytes=${update.bytesTransferred}/${update.totalBytes}"
            )

            // Skip ALL transfer updates for signal payloads.
            // Only remove the ID on terminal states so that subsequent
            // updates (IN_PROGRESS → SUCCESS) are also skipped.
            if (signalPayloadIds.contains(update.payloadId)) {
                Log.d(TAG, "Skipping signal payload ${update.payloadId} ($statusName)")
                if (update.status != PayloadTransferUpdate.Status.IN_PROGRESS) {
                    signalPayloadIds.remove(update.payloadId)
                }
                return
            }

            // Stream payloads: completion is handled by the reading thread.
            if (streamPayloadIds.contains(update.payloadId)) {
                Log.d(TAG, "Stream payload ${update.payloadId}: status=$statusName")
                when (update.status) {
                    PayloadTransferUpdate.Status.FAILURE,
                    PayloadTransferUpdate.Status.CANCELED -> {
                        streamPayloadIds.remove(update.payloadId)
                        _connectionState.value = ConnectionState.Error("Transfer failed")
                    }

                    PayloadTransferUpdate.Status.SUCCESS -> {
                        streamPayloadIds.remove(update.payloadId)
                        Log.d(
                            TAG,
                            "Stream payload ${update.payloadId}: transfer SUCCESS (reading thread handles data)"
                        )
                    }

                    else -> {}
                }
                return
            }

            // FILE / sender-side handling
            Log.d(
                TAG, "FILE/sender payload ${update.payloadId}: status=$statusName, " +
                        "inPendingPayloads=${pendingPayloads.containsKey(update.payloadId)}"
            )

            val progress = if (update.totalBytes > 0) {
                update.bytesTransferred.toFloat() / update.totalBytes
            } else 0f

            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    _connectionState.value = ConnectionState.Transferring(progress)
                }

                PayloadTransferUpdate.Status.SUCCESS -> {
                    val payload = pendingPayloads.remove(update.payloadId)
                    if (payload != null) {
                        // Receiver: read FILE payload and deliver to callback.
                        // Don't set Completed — the ViewModel sets it after import.
                        Log.d(
                            TAG,
                            "Receiver FILE payload ${update.payloadId}: reading via ContentResolver"
                        )
                        val data = readFilePayload(payload)
                        Log.d(
                            TAG,
                            "Receiver FILE payload ${update.payloadId}: read ${data.size} bytes"
                        )
                        if (data.isEmpty()) {
                            Log.e(TAG, "Receiver FILE payload: read 0 bytes!")
                            _connectionState.value = ConnectionState.Error("Received empty file")
                        } else {
                            Log.d(
                                TAG,
                                "Receiver FILE payload: invoking onDataReceived (null=${onDataReceived == null})"
                            )
                            onDataReceived?.invoke(data)
                                ?: run {
                                    Log.e(TAG, "Receiver FILE payload: onDataReceived is null!")
                                    _connectionState.value =
                                        ConnectionState.Error("No data handler registered")
                                }
                        }
                    } else {
                        // Sender: our outgoing transfer completed
                        Log.d(TAG, "Sender transfer complete: ${update.bytesTransferred} bytes")
                        cleanupSendFile()
                        _connectionState.value = ConnectionState.Completed(update.bytesTransferred)
                    }
                }

                PayloadTransferUpdate.Status.FAILURE -> {
                    Log.d(TAG, "Transfer FAILURE for payload ${update.payloadId}")
                    pendingPayloads.remove(update.payloadId)
                    _connectionState.value = ConnectionState.Error("Transfer failed")
                }

                PayloadTransferUpdate.Status.CANCELED -> {
                    Log.d(TAG, "Transfer CANCELED for payload ${update.payloadId}")
                    pendingPayloads.remove(update.payloadId)
                    _connectionState.value = ConnectionState.Error("Transfer canceled")
                }
            }
        }
    }

    private fun readFilePayload(payload: Payload): ByteArray {
        val payloadFile = payload.asFile()
        if (payloadFile == null) {
            Log.e(TAG, "readFilePayload: asFile() returned null")
            return byteArrayOf()
        }
        Log.d(TAG, "readFilePayload: asFile() OK, size=${payloadFile.size}")
        val uri = payloadFile.asUri()
        if (uri == null) {
            Log.e(TAG, "readFilePayload: asUri() returned null")
            return byteArrayOf()
        }
        Log.d(TAG, "readFilePayload: uri=$uri")
        val data = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: byteArrayOf()
        Log.d(TAG, "readFilePayload: read ${data.size} bytes from ContentResolver")
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            // Best-effort cleanup, but never silently: a leaked payload file here
            // is the kind of thing only Crashlytics will ever surface.
            CrashReporter.recordException(e)
        }
        return data
    }

    fun startAdvertising(deviceName: String) {
        Log.d(TAG, "startAdvertising: deviceName=$deviceName")
        _connectionState.value = ConnectionState.Advertising
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        connectionsClient.startAdvertising(
            deviceName,
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "startAdvertising: SUCCESS")
        }.addOnFailureListener { e ->
            Log.e(TAG, "startAdvertising: FAILED", e)
            CrashReporter.recordException(e)
            AppAnalytics.logError("sync", "start_advertising", e.message)
            _connectionState.value = ConnectionState.Error("Advertising failed: ${e.message}")
        }
    }

    fun startDiscovery(deviceName: String) {
        Log.d(TAG, "startDiscovery: deviceName=$deviceName")
        _connectionState.value = ConnectionState.Discovering
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    Log.d(
                        TAG,
                        "onEndpointFound: id=$endpointId, name=${info.endpointName}, serviceId=${info.serviceId}"
                    )
                    connectionsClient.requestConnection(
                        deviceName,
                        endpointId,
                        connectionLifecycleCallback
                    )
                }

                override fun onEndpointLost(endpointId: String) {
                    Log.d(TAG, "onEndpointLost: id=$endpointId")
                }
            },
            options
        ).addOnSuccessListener {
            Log.d(TAG, "startDiscovery: SUCCESS")
        }.addOnFailureListener { e ->
            Log.e(TAG, "startDiscovery: FAILED", e)
            CrashReporter.recordException(e)
            AppAnalytics.logError("sync", "start_discovery", e.message)
            _connectionState.value = ConnectionState.Error("Discovery failed: ${e.message}")
        }
    }

    fun acceptConnection(endpointId: String) {
        Log.d(TAG, "acceptConnection: endpoint=$endpointId")
        connectionsClient.acceptConnection(endpointId, payloadCallback)
        val current = _connectionState.value
        val name = if (current is ConnectionState.Connecting) current.endpointName else ""
        _connectionState.value = ConnectionState.WaitingForPartnerAccept(endpointId, name)
    }

    fun rejectConnection(endpointId: String) {
        Log.d(TAG, "rejectConnection: endpoint=$endpointId")
        connectionsClient.rejectConnection(endpointId)
        _connectionState.value = ConnectionState.Idle
    }

    fun sendData(data: ByteArray) {
        val endpointId = connectedEndpointId ?: run {
            Log.e(TAG, "sendData: connectedEndpointId is null! Cannot send.")
            return
        }
        Log.d(TAG, "sendData: ${data.size} bytes to endpoint=$endpointId")
        _connectionState.value = ConnectionState.Transferring(0f)

        // GZIP compress the data — JSON typically compresses 5-10x
        val compressed = gzipCompress(data)
        Log.d(
            TAG, "sendData: compressed ${data.size} → ${compressed.size} bytes " +
                    "(${100 * compressed.size / data.size}%)"
        )

        if (compressed.size <= MAX_BYTES_PAYLOAD_SIZE) {
            // Small enough for BYTES payload (most reliable transport).
            // Prefix with DATA_PREFIX byte to distinguish from signal payloads.
            val payload = Payload.fromBytes(byteArrayOf(DATA_PREFIX) + compressed)
            Log.d(
                TAG,
                "sendData: using BYTES payload id=${payload.id}, size=${compressed.size + 1}"
            )
            connectionsClient.sendPayload(endpointId, payload)
                .addOnFailureListener { e ->
                    Log.e(TAG, "sendData: BYTES sendPayload FAILED", e)
                    CrashReporter.recordException(e)
                    AppAnalytics.logError("sync", "send_payload_bytes", e.message)
                    _connectionState.value = ConnectionState.Error("Send failed: ${e.message}")
                }
        } else {
            // Too large for BYTES — fall back to FILE payload
            Log.d(TAG, "sendData: compressed too large for BYTES, using FILE payload")
            pendingSendFile = File(context.cacheDir, "sync_export.gz")
            pendingSendFile!!.writeBytes(compressed)
            val payload = Payload.fromFile(pendingSendFile!!)
            Log.d(TAG, "sendData: created FILE payload id=${payload.id}")
            connectionsClient.sendPayload(endpointId, payload)
                .addOnFailureListener { e ->
                    Log.e(TAG, "sendData: FILE sendPayload FAILED", e)
                    CrashReporter.recordException(e)
                    AppAnalytics.logError("sync", "send_payload_file", e.message)
                    cleanupSendFile()
                    _connectionState.value = ConnectionState.Error("Send failed: ${e.message}")
                }
        }
    }

    private fun gzipCompress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream(data.size / 4)
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun gzipDecompress(data: ByteArray): ByteArray {
        return GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
    }

    fun setOnDataReceived(callback: (ByteArray) -> Unit) {
        Log.d(TAG, "setOnDataReceived: callback set")
        onDataReceived = callback
    }

    fun setOnSignalReceived(callback: (SyncSignal) -> Unit) {
        Log.d(TAG, "setOnSignalReceived: callback set")
        onSignalReceived = callback
    }

    fun sendSignal(signal: SyncSignal) {
        val endpointId = connectedEndpointId ?: run {
            Log.e(TAG, "sendSignal: connectedEndpointId is null! Cannot send $signal")
            return
        }
        Log.d(TAG, "sendSignal: $signal to endpoint=$endpointId")
        val payload = Payload.fromBytes(SyncSignal.encode(signal))
        signalPayloadIds.add(payload.id)
        connectionsClient.sendPayload(endpointId, payload)
    }

    fun cancelWithSignal() {
        Log.d(TAG, "cancelWithSignal")
        sendSignal(SyncSignal.Cancel)
        disconnect()
        _connectionState.value = ConnectionState.Cancelled(CancelReason.BY_USER)
    }

    fun disconnect() {
        Log.d(TAG, "disconnect: endpoint=$connectedEndpointId")
        connectedEndpointId?.let { connectionsClient.disconnectFromEndpoint(it) }
        connectedEndpointId = null
    }

    private fun cleanupSendFile() {
        pendingSendFile?.let {
            Log.d(TAG, "Cleaning up temp send file: ${it.exists()}")
            it.delete()
        }
        pendingSendFile = null
    }

    fun stopAll() {
        Log.d(TAG, "stopAll: clearing everything")
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpointId = null
        pendingPayloads.clear()
        signalPayloadIds.clear()
        streamPayloadIds.clear()
        cleanupSendFile()
        onDataReceived = null
        onSignalReceived = null
    }
}
