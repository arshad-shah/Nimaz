package com.arshadshah.nimaz.presentation.viewmodel

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.sync.CancelReason
import com.arshadshah.nimaz.data.sync.ConnectionState
import com.arshadshah.nimaz.data.sync.NearbyConnectionsManager
import com.arshadshah.nimaz.data.sync.SyncDataExporter
import com.arshadshah.nimaz.data.sync.SyncDataImporter
import com.arshadshah.nimaz.data.sync.SyncPayload
import com.arshadshah.nimaz.data.sync.SyncSignal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class SyncMode { NONE, SEND, RECEIVE }

data class SyncDataSummary(
    val bookmarks: Int = 0,
    val favorites: Int = 0,
    val hasReadingProgress: Boolean = false,
    val prayerRecords: Int = 0,
    val fastRecords: Int = 0,
    val makeupFasts: Int = 0,
    val tasbihPresets: Int = 0,
    val tasbihSessions: Int = 0,
    val khatams: Int = 0,
    val khatamAyahs: Int = 0,
    val tafseerHighlights: Int = 0,
    val tafseerNotes: Int = 0,
    val zakatHistory: Int = 0,
    val hasPreferences: Boolean = false,
    val totalBytes: Long = 0,
)

data class ActivityLogEntry(val label: String, val timestamp: Long = System.currentTimeMillis())

data class SyncUiState(
    val mode: SyncMode = SyncMode.NONE,
    val connectionState: ConnectionState = ConnectionState.Idle,
    val statusMessage: String = "",
    val currentStep: String = "",
    val stepsCompleted: Int = 0,
    val totalSteps: Int = 0,
    val transferProgress: Float = 0f,
    val dataSummary: SyncDataSummary? = null,
    val error: String? = null,
    val activityLog: List<ActivityLogEntry> = emptyList(),
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
        Log.d(TAG, "SyncViewModel init")
        connectionsManager.setOnSignalReceived { signal -> handleSignal(signal) }

        viewModelScope.launch {
            connectionsManager.connectionState.collect { state ->
                Log.d(TAG, "connectionState changed: $state (mode=${_uiState.value.mode})")
                _uiState.update { current ->
                    val newState = current.copy(connectionState = state)
                    when (state) {
                        is ConnectionState.Transferring -> newState.copy(
                            transferProgress = state.progress,
                        )

                        else -> newState
                    }
                }

                when (state) {
                    is ConnectionState.WaitingForPartnerAccept -> {
                        addLogEntry("Waiting for partner to accept...")
                        _uiState.update {
                            it.copy(currentStep = "Waiting for partner to accept...")
                        }
                    }

                    is ConnectionState.Connected -> {
                        addLogEntry("Connected to ${state.endpointName}")
                        if (_uiState.value.mode == SyncMode.SEND) {
                            Log.d(TAG, "SEND mode: sending Ready signal and starting export")
                            connectionsManager.sendSignal(SyncSignal.Ready)
                            addLogEntry("Preparing data for export...")
                            sendData()
                        } else {
                            Log.d(TAG, "RECEIVE mode: waiting for data")
                            _uiState.update {
                                it.copy(
                                    currentStep = "Connected — waiting for data...",
                                    stepsCompleted = 0,
                                )
                            }
                        }
                    }

                    is ConnectionState.Completed -> {
                        Log.d(TAG, "Completed state received (mode=${_uiState.value.mode})")
                        if (_uiState.value.mode == SyncMode.SEND) {
                            addLogEntry("Data sent — waiting for partner to import...")
                            _uiState.update {
                                it.copy(
                                    currentStep = "Data sent — waiting for partner...",
                                    stepsCompleted = it.totalSteps - 1,
                                )
                            }
                        }
                    }

                    is ConnectionState.Error -> {
                        Log.e(TAG, "Error state: ${state.message}")
                        addLogEntry("Error: ${state.message}")
                        _uiState.update { it.copy(error = state.message) }
                    }

                    is ConnectionState.Cancelled -> {
                        val reason = when (state.reason) {
                            CancelReason.BY_USER -> "Cancelled by you"
                            CancelReason.BY_PARTNER -> "Cancelled by partner"
                            CancelReason.CONNECTION_LOST -> "Connection lost"
                        }
                        Log.d(TAG, "Cancelled: $reason")
                        addLogEntry(reason)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun handleSignal(signal: SyncSignal) {
        Log.d(TAG, "handleSignal: $signal")
        when (signal) {
            is SyncSignal.Cancel -> {
                addLogEntry("Partner cancelled the sync")
                connectionsManager.disconnect()
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.Cancelled(CancelReason.BY_PARTNER)
                    )
                }
            }

            is SyncSignal.Ready -> {
                addLogEntry("Sender is ready")
            }

            is SyncSignal.ImportStarted -> {
                addLogEntry("Partner started importing...")
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.PartnerImporting(0, 0, "Starting..."),
                        currentStep = "Partner is importing data..."
                    )
                }
            }

            is SyncSignal.ImportProgress -> {
                addLogEntry(signal.label)
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.PartnerImporting(
                            signal.step, signal.total, signal.label
                        ),
                        currentStep = "Partner: ${signal.label}"
                    )
                }
            }

            is SyncSignal.ImportComplete -> {
                addLogEntry("Partner finished importing — sync complete!")
                connectionsManager.sendSignal(SyncSignal.Ack)
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.Completed(
                            it.dataSummary?.totalBytes ?: 0
                        ),
                        statusMessage = "Sync completed successfully!",
                        currentStep = "Complete",
                        stepsCompleted = it.totalSteps,
                    )
                }
                connectionsManager.disconnect()
            }

            is SyncSignal.Ack -> {
                addLogEntry("Sync confirmed — disconnecting")
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.Completed(
                            it.dataSummary?.totalBytes ?: 0
                        ),
                        statusMessage = "Sync completed successfully!",
                        currentStep = "Complete",
                        stepsCompleted = it.totalSteps,
                    )
                }
                connectionsManager.disconnect()
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
        _uiState.update {
            SyncUiState(
                mode = SyncMode.SEND,
                statusMessage = "Waiting for receiver...",
                currentStep = "Advertising...",
                stepsCompleted = 0,
                totalSteps = SEND_TOTAL_STEPS,
                activityLog = listOf(ActivityLogEntry("Started sending mode"))
            )
        }
        connectionsManager.startAdvertising(getDeviceName())
    }

    private fun startReceive() {
        _uiState.update {
            SyncUiState(
                mode = SyncMode.RECEIVE,
                statusMessage = "Searching for sender...",
                currentStep = "Discovering...",
                stepsCompleted = 0,
                totalSteps = RECEIVE_TOTAL_STEPS,
                activityLog = listOf(ActivityLogEntry("Started receiving mode"))
            )
        }

        connectionsManager.setOnDataReceived { bytes ->
            Log.d(TAG, "onDataReceived callback invoked: ${bytes.size} bytes")
            addLogEntry("Received ${formatBytes(bytes.size.toLong())} of data")
            viewModelScope.launch {
                try {
                    updateStep("Reading received data...", 1)
                    Log.d(TAG, "Decoding JSON payload (${bytes.size} bytes)...")
                    val jsonString = String(bytes)
                    Log.d(
                        TAG,
                        "JSON string length: ${jsonString.length}, first 200 chars: ${
                            jsonString.take(200)
                        }"
                    )
                    val payload = json.decodeFromString<SyncPayload>(jsonString)
                    Log.d(
                        TAG, "JSON decoded successfully: bookmarks=${payload.bookmarks.size}, " +
                                "favorites=${payload.favorites.size}, prayers=${payload.prayerRecords.size}"
                    )
                    val summary = buildSummaryFromPayload(payload, bytes.size.toLong())
                    _uiState.update { it.copy(dataSummary = summary) }

                    Log.d(TAG, "Starting import with progress...")
                    importWithProgress(payload)

                    Log.d(TAG, "Import complete! Setting Completed state.")
                    _uiState.update {
                        it.copy(
                            connectionState = ConnectionState.Completed(bytes.size.toLong()),
                            statusMessage = "Sync completed successfully!",
                            currentStep = "Complete",
                            stepsCompleted = RECEIVE_TOTAL_STEPS,
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Import failed!", e)
                    addLogEntry("Import failed: ${e.message}")
                    _uiState.update { it.copy(error = "Import failed: ${e.message}") }
                }
            }
        }

        connectionsManager.startDiscovery(getDeviceName())
    }

    private fun sendData() {
        Log.d(TAG, "sendData: starting export")
        viewModelScope.launch {
            try {
                var exportStep = 0
                val payload = exporter.export { step ->
                    exportStep++
                    Log.d(TAG, "Export step $exportStep: $step")
                    addLogEntry(step)
                    _uiState.update { it.copy(currentStep = step, stepsCompleted = exportStep) }
                    yield()
                }

                Log.d(
                    TAG, "Export returned: bookmarks=${payload.bookmarks.size}, " +
                            "favorites=${payload.favorites.size}, prayers=${payload.prayerRecords.size}, " +
                            "khatamAyahs=${payload.khatamAyahs.size}, prefs=${payload.preferences.size}"
                )
                val summary = buildSummaryFromPayload(payload, 0)
                _uiState.update { it.copy(dataSummary = summary) }

                updateStep("Encoding data...", 8)
                addLogEntry("Encoding data...")
                Log.d(TAG, "Starting JSON encode...")
                val startEncode = System.currentTimeMillis()
                val jsonBytes = json.encodeToString(SyncPayload.serializer(), payload).toByteArray()
                val encodeMs = System.currentTimeMillis() - startEncode
                Log.d(TAG, "JSON encoded: ${jsonBytes.size} bytes in ${encodeMs}ms")

                _uiState.update {
                    it.copy(
                        dataSummary = it.dataSummary?.copy(totalBytes = jsonBytes.size.toLong())
                    )
                }

                val sizeText = formatBytes(jsonBytes.size.toLong())
                updateStep("Sending $sizeText...", 9)
                addLogEntry("Sending $sizeText to partner...")
                Log.d(TAG, "Calling connectionsManager.sendData(${jsonBytes.size} bytes)")
                connectionsManager.sendData(jsonBytes)
                Log.d(TAG, "sendData returned — transfer queued")
            } catch (e: Exception) {
                Log.e(TAG, "Export/send failed!", e)
                addLogEntry("Export failed: ${e.message}")
                _uiState.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    private suspend fun importWithProgress(payload: SyncPayload) {
        val totalImportSteps = 8
        Log.d(TAG, "importWithProgress: starting $totalImportSteps steps")
        connectionsManager.sendSignal(SyncSignal.ImportStarted)

        val steps: List<Pair<String, suspend () -> Unit>> = listOf(
            "Importing Quran bookmarks & favorites..." to { importer.importQuranData(payload) },
            "Importing prayer records..." to { importer.importPrayerData(payload) },
            "Importing fasting records..." to { importer.importFastingData(payload) },
            "Importing tasbih data..." to { importer.importTasbihData(payload) },
            "Importing khatam data..." to { importer.importKhatamData(payload) },
            "Importing tafseer data..." to { importer.importTafseerData(payload) },
            "Importing zakat history..." to { importer.importZakatData(payload) },
            "Importing preferences..." to { importer.importPreferencesData(payload) },
        )

        steps.forEachIndexed { index, (label, action) ->
            val step = index + 2 // step 1 is "Reading received data..."
            Log.d(TAG, "Import step ${index + 1}/$totalImportSteps: $label")
            connectionsManager.sendSignal(
                SyncSignal.ImportProgress(step = index + 1, total = totalImportSteps, label = label)
            )
            updateStep(label, step)
            addLogEntry(label)
            action()
            Log.d(TAG, "Import step ${index + 1} completed")
        }

        Log.d(TAG, "All import steps complete, sending ImportComplete signal")
        connectionsManager.sendSignal(SyncSignal.ImportComplete)
        addLogEntry("Import complete — waiting for confirmation...")
    }

    private suspend fun updateStep(step: String, completed: Int) {
        _uiState.update { it.copy(currentStep = step, stepsCompleted = completed) }
        yield() // Let the UI recompose and show this step before continuing
    }

    private fun buildSummaryFromPayload(payload: SyncPayload, bytes: Long) = SyncDataSummary(
        bookmarks = payload.bookmarks.size,
        favorites = payload.favorites.size,
        hasReadingProgress = payload.readingProgress != null,
        prayerRecords = payload.prayerRecords.size,
        fastRecords = payload.fastRecords.size,
        makeupFasts = payload.makeupFasts.size,
        tasbihPresets = payload.tasbihPresets.size,
        tasbihSessions = payload.tasbihSessions.size,
        khatams = payload.khatams.size,
        khatamAyahs = payload.khatamAyahs.size,
        tafseerHighlights = payload.tafseerHighlights.size,
        tafseerNotes = payload.tafseerNotes.size,
        zakatHistory = payload.zakatHistory.size,
        hasPreferences = payload.preferences.isNotEmpty(),
        totalBytes = bytes,
    )

    private fun acceptConnection(endpointId: String) {
        connectionsManager.acceptConnection(endpointId)
    }

    private fun rejectConnection(endpointId: String) {
        connectionsManager.rejectConnection(endpointId)
    }

    private fun cancel() {
        val current = _uiState.value.connectionState
        val isTerminal =
            current is ConnectionState.Completed || current is ConnectionState.Cancelled
        Log.d(TAG, "cancel: currentState=$current, isTerminal=$isTerminal")

        if (!isTerminal) {
            if (current is ConnectionState.Connected ||
                current is ConnectionState.Transferring ||
                current is ConnectionState.WaitingForPartnerAccept ||
                current is ConnectionState.PartnerImporting
            ) {
                Log.d(TAG, "cancel: sending cancel signal to partner")
                connectionsManager.cancelWithSignal()
            }
        }
        connectionsManager.stopAll()
        _uiState.update {
            if (isTerminal) SyncUiState() else it.copy(
                connectionState = ConnectionState.Cancelled(
                    CancelReason.BY_USER
                )
            )
        }
    }

    private fun addLogEntry(label: String) {
        _uiState.update { current ->
            current.copy(
                activityLog = current.activityLog + ActivityLogEntry(label)
            )
        }
    }

    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    override fun onCleared() {
        super.onCleared()
        connectionsManager.stopAll()
    }

    companion object {
        private const val TAG = "SyncVM"

        // 7 export callbacks + packaging + sending + complete
        private const val SEND_TOTAL_STEPS = 10

        // reading + 8 import categories + complete
        private const val RECEIVE_TOTAL_STEPS = 10

        fun formatBytes(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
