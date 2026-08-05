package com.arshadshah.nimaz.presentation.viewmodel.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.data.sync.CancelReason
import com.arshadshah.nimaz.data.sync.ConnectionState
import com.arshadshah.nimaz.data.sync.NearbyConnectionsManager
import com.arshadshah.nimaz.data.sync.SyncCategory
import com.arshadshah.nimaz.data.sync.SyncDataExporter
import com.arshadshah.nimaz.data.sync.SyncDataImporter
import com.arshadshah.nimaz.data.sync.SyncPayload
import com.arshadshah.nimaz.data.sync.SyncSignal
import com.arshadshah.nimaz.data.sync.categories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject

enum class SyncMode { NONE, SEND, RECEIVE }

/**
 * Snapshot of what a [SyncPayload] contains, shown on the sync screen.
 *
 * Backed by [SyncPayload.categories] — the single source of truth — so the
 * summary can never silently miss a newly-added sync field.
 */
data class SyncDataSummary(
    val categories: List<SyncCategory> = emptyList(),
    val totalBytes: Long = 0,
)

data class ActivityLogEntry(val label: String, val timestamp: Long = System.currentTimeMillis())

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val connectionsManager: NearbyConnectionsManager,
    private val exporter: SyncDataExporter,
    private val importer: SyncDataImporter,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        debugLog("SyncViewModel init")
        connectionsManager.setOnSignalReceived { signal -> handleSignal(signal) }

        launchSafely(telemetry, AppAnalytics.Feature.SYNC, "launch") {
            connectionsManager.connectionState.collect { state ->
                debugLog("connectionState changed: $state (mode=${_uiState.value.mode})")
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
                            debugLog("SEND mode: sending Ready signal and starting export")
                            connectionsManager.sendSignal(SyncSignal.Ready)
                            addLogEntry("Preparing data for export...")
                            sendData()
                        } else {
                            debugLog("RECEIVE mode: waiting for data")
                            _uiState.update {
                                it.copy(
                                    currentStep = "Connected — waiting for data...",
                                    stepsCompleted = 0,
                                )
                            }
                        }
                    }

                    is ConnectionState.Completed -> {
                        debugLog("Completed state received (mode=${_uiState.value.mode})")
                        if (_uiState.value.mode == SyncMode.SEND) {
                            telemetry.featureUsed(AppAnalytics.Feature.SYNC, "send_completed")
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
                        // Sync is the highest-risk feature in the app and only its two
                        // exception paths were instrumented; this branch — a transport error
                        // with no throwable to record — went to `Log.e` and nowhere else.
                        telemetry.error(AppAnalytics.Feature.SYNC, "connection", state.message)
                        debugLog("Error state: ${state.message}")
                        addLogEntry("Error: ${state.message}")
                        _uiState.update { it.copy(error = state.message) }
                    }

                    is ConnectionState.Cancelled -> {
                        val reason = when (state.reason) {
                            CancelReason.BY_USER -> "Cancelled by you"
                            CancelReason.BY_PARTNER -> "Cancelled by partner"
                            CancelReason.CONNECTION_LOST -> "Connection lost"
                        }
                        // Which of the three ended it is the whole question: a sync people
                        // abandon and a sync the transport drops need different fixes, and
                        // both looked identical from outside.
                        telemetry.featureUsed(AppAnalytics.Feature.SYNC, "cancelled_${state.reason.name.lowercase()}")
                        debugLog("Cancelled: $reason")
                        addLogEntry(reason)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun handleSignal(signal: SyncSignal) {
        debugLog("handleSignal: $signal")
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
            is SyncEvent.StartSend -> {
                telemetry.featureUsed(AppAnalytics.Feature.SYNC, "start_send")
                startSend()
            }
            is SyncEvent.StartReceive -> {
                telemetry.featureUsed(AppAnalytics.Feature.SYNC, "start_receive")
                startReceive()
            }
            is SyncEvent.AcceptConnection -> acceptConnection(event.endpointId)
            is SyncEvent.RejectConnection -> rejectConnection(event.endpointId)
            is SyncEvent.Cancel -> {
                telemetry.featureUsed(AppAnalytics.Feature.SYNC, "cancel")
                cancel()
            }
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
            debugLog("onDataReceived callback invoked: ${bytes.size} bytes")
            addLogEntry("Received ${formatBytes(bytes.size.toLong())} of data")
            launchSafely(telemetry, AppAnalytics.Feature.SYNC, "start_receive") {
                try {
                    updateStep("Reading received data...", 1)
                    debugLog("Decoding JSON payload (${bytes.size} bytes)...")
                    val jsonString = String(bytes)
                    // The log that used to sit here printed the first 200 characters of the
                    // payload — real bookmark ids and prayer records — with no DEBUG guard.
                    // Deleted rather than guarded: payload content has no business in a log.
                    val payload = json.decodeFromString<SyncPayload>(jsonString)
                    debugLog(
                        "JSON decoded successfully: bookmarks=${payload.bookmarks.size}, " +
                                "favorites=${payload.favorites.size}, prayers=${payload.prayerRecords.size}"
                    )
                    val summary = buildSummaryFromPayload(payload, bytes.size.toLong())
                    _uiState.update { it.copy(dataSummary = summary) }

                    debugLog("Starting import with progress...")
                    importWithProgress(payload)

                    debugLog("Import complete! Setting Completed state.")
                    telemetry.featureUsed(AppAnalytics.Feature.SYNC, "receive_completed")
                    _uiState.update {
                        it.copy(
                            connectionState = ConnectionState.Completed(bytes.size.toLong()),
                            statusMessage = "Sync completed successfully!",
                            currentStep = "Complete",
                            stepsCompleted = RECEIVE_TOTAL_STEPS,
                        )
                    }
                } catch (e: Exception) {
                    telemetry.failure(AppAnalytics.Feature.SYNC, "import", e)
                    debugLog("Import failed: ${e.message}")
                    addLogEntry("Import failed: ${e.message}")
                    _uiState.update { it.copy(error = "Import failed: ${e.message}") }
                }
            }
        }

        connectionsManager.startDiscovery(getDeviceName())
    }

    private fun sendData() {
        debugLog("sendData: starting export")
        launchSafely(telemetry, AppAnalytics.Feature.SYNC, "send_data") {
            try {
                val payload = exporter.export { completed, _, step ->
                    addLogEntry(step)
                    _uiState.update { it.copy(currentStep = step, stepsCompleted = completed) }
                    yield()
                }

                debugLog(
                    "Export returned: bookmarks=${payload.bookmarks.size}, " +
                            "favorites=${payload.favorites.size}, prayers=${payload.prayerRecords.size}, " +
                            "khatamAyahs=${payload.khatamAyahs.size}, prefs=${payload.preferences.size}"
                )
                val summary = buildSummaryFromPayload(payload, 0)
                _uiState.update { it.copy(dataSummary = summary) }

                updateStep("Encoding data...", SyncDataExporter.STEP_COUNT + 1)
                addLogEntry("Encoding data...")
                debugLog("Starting JSON encode...")
                val startEncode = System.currentTimeMillis()
                val jsonBytes = json.encodeToString(SyncPayload.serializer(), payload).toByteArray()
                val encodeMs = System.currentTimeMillis() - startEncode
                debugLog("JSON encoded: ${jsonBytes.size} bytes in ${encodeMs}ms")

                _uiState.update {
                    it.copy(
                        dataSummary = it.dataSummary?.copy(totalBytes = jsonBytes.size.toLong())
                    )
                }

                val sizeText = formatBytes(jsonBytes.size.toLong())
                updateStep("Sending $sizeText...", SyncDataExporter.STEP_COUNT + 2)
                addLogEntry("Sending $sizeText to partner...")
                debugLog("Calling connectionsManager.sendData(${jsonBytes.size} bytes)")
                connectionsManager.sendData(jsonBytes)
                debugLog("sendData returned — transfer queued")
            } catch (e: Exception) {
                telemetry.failure(AppAnalytics.Feature.SYNC, "export", e)
                debugLog("Export/send failed: ${e.message}")
                addLogEntry("Export failed: ${e.message}")
                _uiState.update { it.copy(error = "Export failed: ${e.message}") }
            }
        }
    }

    private suspend fun importWithProgress(payload: SyncPayload) {

        connectionsManager.sendSignal(SyncSignal.ImportStarted)

        val steps: List<Pair<String, suspend () -> Unit>> = listOf(
            "Importing Quran bookmarks & favorites..." to { importer.importQuranData(payload) },
            "Importing prayer records..." to { importer.importPrayerData(payload) },
            "Importing fasting records..." to { importer.importFastingData(payload) },
            "Importing tasbih data..." to { importer.importTasbihData(payload) },
            "Importing khatam data..." to { importer.importKhatamData(payload) },
            "Importing tafseer data..." to { importer.importTafseerData(payload) },
            "Importing zakat history..." to { importer.importZakatData(payload) },
            "Importing saved names & prophets..." to { importer.importNamesData(payload) },
            "Importing hadith & dua bookmarks..." to { importer.importHadithDuaData(payload) },
            "Importing Qaida progress..." to { importer.importQaidaData(payload) },
            "Importing saved locations..." to { importer.importLocationsData(payload) },
            "Importing preferences..." to { importer.importPreferencesData(payload) },
        )

        // Both totals come from `steps` itself. They used to be two hand-maintained numbers
        // against a list of twelve: the wire signal said "12 of 8" on the partner's screen, and
        // the local caption reached "Step 13 of 10".
        val totalImportSteps = steps.size
        check(totalImportSteps == IMPORT_STEP_COUNT) {
            "import steps ($totalImportSteps) disagree with IMPORT_STEP_COUNT ($IMPORT_STEP_COUNT)"
        }
        steps.forEachIndexed { index, (label, action) ->
            val step = index + 2 // step 1 is "Reading received data..."
            connectionsManager.sendSignal(
                SyncSignal.ImportProgress(step = index + 1, total = totalImportSteps, label = label)
            )
            updateStep(label, step)
            addLogEntry(label)
            action()
            debugLog("Import step ${index + 1} completed")
        }

        debugLog("All import steps complete, sending ImportComplete signal")
        connectionsManager.sendSignal(SyncSignal.ImportComplete)
        addLogEntry("Import complete — waiting for confirmation...")
    }

    private suspend fun updateStep(step: String, completed: Int) {
        _uiState.update { it.copy(currentStep = step, stepsCompleted = completed) }
        yield() // Let the UI recompose and show this step before continuing
    }

    private fun buildSummaryFromPayload(payload: SyncPayload, bytes: Long) = SyncDataSummary(
        categories = payload.categories(),
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
        debugLog("cancel: currentState=$current, isTerminal=$isTerminal")

        if (!isTerminal) {
            if (current is ConnectionState.Connected ||
                current is ConnectionState.Transferring ||
                current is ConnectionState.WaitingForPartnerAccept ||
                current is ConnectionState.PartnerImporting
            ) {
                debugLog("cancel: sending cancel signal to partner")
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
        // `connectionsManager` is a @Singleton, and both callbacks capture this ViewModel. Only
        // stopping the transport left the singleton holding a destroyed ViewModel and its dead
        // `viewModelScope` — so the receive handler stayed armed and its `launch` was a silent
        // no-op. Clearing them is the half that was missing.
        connectionsManager.setOnSignalReceived(null)
        connectionsManager.setOnDataReceived(null)
        connectionsManager.stopAll()
    }

    companion object {
        private const val TAG = "SyncVM"

        /**
         * Every step the send flow reports: the exporter's own, then encoding, then sending.
         *
         * Derived rather than counted by hand. The comment that used to sit here said
         * "7 export callbacks" against an exporter that made **eleven** calls, which is how
         * the bar reached 120% and then rewound.
         */
        private val SEND_TOTAL_STEPS = SyncDataExporter.STEP_COUNT + 2

        /** Reading the received bytes, then one step per import category. */
        internal val RECEIVE_TOTAL_STEPS = IMPORT_STEP_COUNT + 1

        /**
         * The number of categories [importWithProgress] imports.
         *
         * Kept beside the totals so the two cannot drift the way `8` did against a list of
         * twelve; `importWithProgress` asserts the list still matches.
         */
        internal const val IMPORT_STEP_COUNT = 12

        fun formatBytes(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            // Shown in the sync progress UI, so it follows the device locale's
            // decimal separator - explicit here only to satisfy lint.
            else -> String.format(
                Locale.getDefault(),
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
            )
        }
    }
}

/**
 * Sync's verbose tracing, compiled out of release builds.
 *
 * This ViewModel is the only one in the app that uses `android.util.Log` — 29 calls — and the
 * app ships no proguard rule stripping it, so all of it reached release logcat. One of those
 * calls printed the first 200 characters of the decoded `SyncPayload`: real bookmark ids and
 * prayer records, readable by any app holding READ_LOGS on an older device or by anyone with
 * adb. That call is deleted rather than guarded; the rest are behind this.
 */
private fun debugLog(message: String) {
    if (BuildConfig.DEBUG) Log.d("SyncVM", message)
}
