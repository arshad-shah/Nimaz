package com.arshadshah.nimaz.presentation.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.sync.ConnectionState
import com.arshadshah.nimaz.data.sync.NearbyConnectionsManager
import com.arshadshah.nimaz.data.sync.SyncDataExporter
import com.arshadshah.nimaz.data.sync.SyncDataImporter
import com.arshadshah.nimaz.data.sync.SyncPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class SyncMode { NONE, SEND, RECEIVE }

data class SyncUiState(
    val mode: SyncMode = SyncMode.NONE,
    val connectionState: ConnectionState = ConnectionState.Idle,
    val statusMessage: String = "",
    val error: String? = null
)

sealed interface SyncEvent {
    data object StartSend : SyncEvent
    data object StartReceive : SyncEvent
    data class AcceptConnection(val endpointId: String) : SyncEvent
    data class RejectConnection(val endpointId: String) : SyncEvent
    data object Cancel : SyncEvent
}

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val connectionsManager: NearbyConnectionsManager,
    private val exporter: SyncDataExporter,
    private val importer: SyncDataImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch {
            connectionsManager.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }

                when (state) {
                    is ConnectionState.Connected -> {
                        if (_uiState.value.mode == SyncMode.SEND) {
                            sendData()
                        }
                    }
                    is ConnectionState.Completed -> {
                        _uiState.update { it.copy(statusMessage = "Sync completed successfully!") }
                    }
                    is ConnectionState.Error -> {
                        _uiState.update { it.copy(error = state.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun onEvent(event: SyncEvent) {
        when (event) {
            is SyncEvent.StartSend -> startSend()
            is SyncEvent.StartReceive -> startReceive()
            is SyncEvent.AcceptConnection -> acceptConnection(event.endpointId)
            is SyncEvent.RejectConnection -> rejectConnection(event.endpointId)
            is SyncEvent.Cancel -> cancel()
        }
    }

    private fun startSend() {
        _uiState.update { it.copy(mode = SyncMode.SEND, error = null, statusMessage = "Waiting for receiver...") }
        connectionsManager.startAdvertising(getDeviceName())
    }

    private fun startReceive() {
        _uiState.update { it.copy(mode = SyncMode.RECEIVE, error = null, statusMessage = "Searching for sender...") }

        connectionsManager.setOnDataReceived { bytes ->
            viewModelScope.launch {
                try {
                    _uiState.update { it.copy(statusMessage = "Importing data...") }
                    val payload = json.decodeFromString<SyncPayload>(String(bytes))
                    importer.import(payload)
                    _uiState.update { it.copy(statusMessage = "Sync completed successfully!") }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Import failed: ${e.message}") }
                }
            }
        }

        connectionsManager.startDiscovery(getDeviceName())
    }

    private fun sendData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(statusMessage = "Exporting data...") }
                val payload = exporter.export()
                val jsonBytes = json.encodeToString(SyncPayload.serializer(), payload).toByteArray()
                _uiState.update { it.copy(statusMessage = "Sending data...") }
                connectionsManager.sendData(jsonBytes)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    private fun acceptConnection(endpointId: String) {
        connectionsManager.acceptConnection(endpointId)
    }

    private fun rejectConnection(endpointId: String) {
        connectionsManager.rejectConnection(endpointId)
    }

    private fun cancel() {
        connectionsManager.stopAll()
        _uiState.update { SyncUiState() }
    }

    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    override fun onCleared() {
        super.onCleared()
        connectionsManager.stopAll()
    }
}
