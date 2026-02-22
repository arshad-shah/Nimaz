package com.arshadshah.nimaz.data.sync

import android.content.Context
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
import javax.inject.Inject
import javax.inject.Singleton

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Advertising : ConnectionState()
    data object Discovering : ConnectionState()
    data class Connecting(val endpointId: String, val endpointName: String, val authToken: String) : ConnectionState()
    data class Connected(val endpointId: String, val endpointName: String) : ConnectionState()
    data class Transferring(val progress: Float) : ConnectionState()
    data class Completed(val bytesReceived: Long) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

@Singleton
class NearbyConnectionsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SERVICE_ID = "com.arshadshah.nimaz.sync"
    }

    private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var connectedEndpointId: String? = null
    private var receivedData = ByteArrayOutputStream()
    private var onDataReceived: ((ByteArray) -> Unit)? = null

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            _connectionState.value = ConnectionState.Connecting(
                endpointId = endpointId,
                endpointName = info.endpointName,
                authToken = info.authenticationDigits
            )
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectedEndpointId = endpointId
                    val current = _connectionState.value
                    val name = if (current is ConnectionState.Connecting) current.endpointName else ""
                    _connectionState.value = ConnectionState.Connected(endpointId, name)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    _connectionState.value = ConnectionState.Error("Connection rejected")
                }
                else -> {
                    _connectionState.value = ConnectionState.Error("Connection failed: ${result.status.statusMessage}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpointId = null
            val current = _connectionState.value
            if (current !is ConnectionState.Completed && current !is ConnectionState.Error) {
                _connectionState.value = ConnectionState.Idle
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    payload.asBytes()?.let { bytes ->
                        receivedData.write(bytes)
                    }
                }
                Payload.Type.STREAM -> {
                    payload.asStream()?.asInputStream()?.let { stream ->
                        stream.copyTo(receivedData)
                        stream.close()
                    }
                }
                else -> { /* FILE type not used */ }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val progress = if (update.totalBytes > 0) {
                update.bytesTransferred.toFloat() / update.totalBytes
            } else 0f

            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    _connectionState.value = ConnectionState.Transferring(progress)
                }
                PayloadTransferUpdate.Status.SUCCESS -> {
                    val data = receivedData.toByteArray()
                    _connectionState.value = ConnectionState.Completed(data.size.toLong())
                    onDataReceived?.invoke(data)
                    receivedData = ByteArrayOutputStream()
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    _connectionState.value = ConnectionState.Error("Transfer failed")
                    receivedData = ByteArrayOutputStream()
                }
                PayloadTransferUpdate.Status.CANCELED -> {
                    _connectionState.value = ConnectionState.Error("Transfer canceled")
                    receivedData = ByteArrayOutputStream()
                }
            }
        }
    }

    fun startAdvertising(deviceName: String) {
        _connectionState.value = ConnectionState.Advertising
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        connectionsClient.startAdvertising(
            deviceName,
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnFailureListener { e ->
            _connectionState.value = ConnectionState.Error("Advertising failed: ${e.message}")
        }
    }

    fun startDiscovery(deviceName: String) {
        _connectionState.value = ConnectionState.Discovering
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    connectionsClient.requestConnection(
                        deviceName,
                        endpointId,
                        connectionLifecycleCallback
                    )
                }

                override fun onEndpointLost(endpointId: String) {
                    // Endpoint no longer available
                }
            },
            options
        ).addOnFailureListener { e ->
            _connectionState.value = ConnectionState.Error("Discovery failed: ${e.message}")
        }
    }

    fun acceptConnection(endpointId: String) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
    }

    fun rejectConnection(endpointId: String) {
        connectionsClient.rejectConnection(endpointId)
        _connectionState.value = ConnectionState.Idle
    }

    fun sendData(data: ByteArray) {
        val endpointId = connectedEndpointId ?: return
        _connectionState.value = ConnectionState.Transferring(0f)
        val payload = Payload.fromStream(ByteArrayInputStream(data))
        connectionsClient.sendPayload(endpointId, payload)
    }

    fun setOnDataReceived(callback: (ByteArray) -> Unit) {
        onDataReceived = callback
    }

    fun disconnect() {
        connectedEndpointId?.let { connectionsClient.disconnectFromEndpoint(it) }
        connectedEndpointId = null
    }

    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpointId = null
        receivedData = ByteArrayOutputStream()
        onDataReceived = null
        _connectionState.value = ConnectionState.Idle
    }
}
