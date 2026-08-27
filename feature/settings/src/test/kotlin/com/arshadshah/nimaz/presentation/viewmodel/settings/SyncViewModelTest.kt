package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.data.sync.CancelReason
import com.arshadshah.nimaz.data.sync.ConnectionState
import com.arshadshah.nimaz.data.sync.NearbyConnectionsManager
import com.arshadshah.nimaz.data.sync.SyncDataExporter
import com.arshadshah.nimaz.data.sync.SyncDataImporter
import com.arshadshah.nimaz.data.sync.SyncSignal
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Sync is the highest-risk feature in the app — it moves a user's whole history between two
 * devices — and it was the last ViewModel with no test at all. That is not incidental: the
 * progress bar reaching 120% and then rewinding, and 200 characters of decoded payload going
 * to release logcat, both shipped because nothing here was pinned.
 *
 * Everything below runs on a plain JVM. `NearbyConnectionsManager` is a `@Singleton` holding
 * the Nearby transport, so it is faked; `android.util.Log` and `Build.MANUFACTURER` resolve
 * through `testOptions.unitTests.isReturnDefaultValues`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var connections: NearbyConnectionsManager
    private lateinit var exporter: SyncDataExporter
    private lateinit var importer: SyncDataImporter
    private val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        connections = mockk(relaxed = true)
        every { connections.connectionState } returns connectionState
        exporter = mockk(relaxed = true)
        importer = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SyncViewModel(connections, exporter, importer, telemetry)

    @Test
    fun `the send flow reports every step the exporter actually takes`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(SyncEvent.StartSend)
        advanceUntilIdle()

        // The count is derived from SyncDataExporter.STEP_COUNT, not written out by hand.
        // The comment that used to stand in for this said "7 export callbacks" against an
        // exporter making eleven calls, which is how the bar reached 120% and rewound.
        assertThat(vm.uiState.value.totalSteps).isEqualTo(SyncDataExporter.STEP_COUNT + 2)
        assertThat(vm.uiState.value.mode).isEqualTo(SyncMode.SEND)
    }

    @Test
    fun `the receive flow reports one step per import category plus the read`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(SyncEvent.StartReceive)
        advanceUntilIdle()

        assertThat(vm.uiState.value.totalSteps)
            .isEqualTo(SyncViewModel.IMPORT_STEP_COUNT + 1)
        assertThat(vm.uiState.value.mode).isEqualTo(SyncMode.RECEIVE)
    }

    /**
     * `connectionsManager` is a `@Singleton` and both callbacks capture this ViewModel. Only
     * stopping the transport left the singleton holding a destroyed ViewModel and its dead
     * `viewModelScope`, so the receive handler stayed armed and its `launch` was a silent
     * no-op — the leak #365 asked for.
     */
    @Test
    fun `clearing the ViewModel disarms both callbacks and the transport`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        // init arms the signal handler...
        verify { connections.setOnSignalReceived(any()) }

        vm.callOnCleared()

        verify { connections.setOnSignalReceived(null) }
        verify { connections.setOnDataReceived(null) }
        verify { connections.stopAll() }
    }

    @Test
    fun `a transport error is reported, not only logged`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        connectionState.value = ConnectionState.Error("bluetooth unavailable")
        advanceUntilIdle()

        assertThat(telemetry.errors.map { it.domain }).contains("sync")
        assertThat(vm.uiState.value.connectionState)
            .isInstanceOf(ConnectionState.Error::class.java)
    }

    // ── The connection state machine ─────────────────────────────────────────────────────────

    @Test
    fun `connecting as the sender starts the export without waiting to be asked`() {
        // The sender's job begins the moment the connection is up: signal Ready, then export.
        // A sender that waited for something would leave both devices staring at a spinner,
        // because the receiver's next move is to wait for data.
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()
            vm.onEvent(SyncEvent.StartSend)
            advanceUntilIdle()

            connectionState.value = ConnectionState.Connected("e1", "Pixel 8")
            advanceUntilIdle()

            verify { connections.sendSignal(SyncSignal.Ready) }
            coVerify { exporter.export(any()) }
        }
    }

    @Test
    fun `connecting as the receiver exports nothing and says it is waiting`() {
        // The mirror image, and the one that matters: a receiver that exported would send its
        // own data back over the connection meant to replace it.
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()
            vm.onEvent(SyncEvent.StartReceive)
            advanceUntilIdle()

            connectionState.value = ConnectionState.Connected("e1", "Pixel 8")
            advanceUntilIdle()

            coVerify(exactly = 0) { exporter.export(any()) }
            verify(exactly = 0) { connections.sendSignal(SyncSignal.Ready) }
            assertThat(vm.uiState.value.currentStep).contains("waiting")
        }
    }

    @Test
    fun `transfer progress reaches the state the bar is drawn from`() {
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()
            connectionState.value = ConnectionState.Transferring(0.42f)
            advanceUntilIdle()

            assertThat(vm.uiState.value.transferProgress).isEqualTo(0.42f)
        }
    }

    @Test
    fun `a completed send is not reported as finished until the partner has imported`() {
        // "Completed" on the transport means the bytes left this device, not that they landed
        // anywhere useful. Reporting the sync done here would let the sender walk away while
        // the import was still running — and a failed import would never be seen.
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()
            vm.onEvent(SyncEvent.StartSend)
            advanceUntilIdle()

            connectionState.value = ConnectionState.Completed(bytesReceived = 0)
            advanceUntilIdle()

            assertThat(vm.uiState.value.currentStep).contains("waiting for partner")
            assertThat(vm.uiState.value.stepsCompleted)
                .isEqualTo(vm.uiState.value.totalSteps - 1)
        }
    }

    @Test
    fun `each cancellation reason is recorded as its own event`() {
        // A sync people abandon and a sync the transport drops need different fixes, and from
        // outside they looked identical.
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()
            connectionState.value = ConnectionState.Cancelled(CancelReason.CONNECTION_LOST)
            advanceUntilIdle()

            assertThat(telemetry.featureUsages.map { it.action })
                .contains("cancelled_connection_lost")
            assertThat(vm.uiState.value.activityLog.map { it.label })
                .contains("Connection lost")
        }
    }

    @Test
    fun `a partner cancellation is distinguished from the user's own`() {
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()
            connectionState.value = ConnectionState.Cancelled(CancelReason.BY_PARTNER)
            advanceUntilIdle()

            assertThat(vm.uiState.value.activityLog.map { it.label })
                .contains("Cancelled by partner")
            assertThat(telemetry.featureUsages.map { it.action })
                .contains("cancelled_by_partner")
        }
    }

    // ── Signals from the other device ────────────────────────────────────────────────────────

    /** The handler the transport was armed with in `init`. */
    private fun signalHandler(): (SyncSignal) -> Unit {
        val slot = slot<(SyncSignal) -> Unit>()
        verify { connections.setOnSignalReceived(capture(slot)) }
        return slot.captured
    }

    @Test
    fun `a partner's cancel signal ends the sync and disconnects`() {
        // Without the disconnect the transport stays up after the other side has gone, and the
        // screen shows a live connection to nobody.
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()

            signalHandler()(SyncSignal.Cancel)
            advanceUntilIdle()

            verify { connections.disconnect() }
            assertThat(vm.uiState.value.connectionState)
                .isEqualTo(ConnectionState.Cancelled(CancelReason.BY_PARTNER))
        }
    }

    @Test
    fun `the partner's import progress is shown on the sender's screen`() {
        // The sender has nothing left to do at this point, so without the relayed progress the
        // screen sits still through the longest part of the transfer.
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()

            signalHandler()(SyncSignal.ImportStarted)
            advanceUntilIdle()
            assertThat(vm.uiState.value.connectionState)
                .isInstanceOf(ConnectionState.PartnerImporting::class.java)

            signalHandler()(
                SyncSignal.ImportProgress(step = 4, total = 12, label = "Importing khatam data...")
            )
            advanceUntilIdle()

            val state = vm.uiState.value.connectionState as ConnectionState.PartnerImporting
            assertThat(state.step).isEqualTo(4)
            assertThat(state.total).isEqualTo(12)
            assertThat(vm.uiState.value.currentStep).contains("Importing khatam data...")
        }
    }

    @Test
    fun `the partner's import completing acknowledges and finishes the sync`() {
        // The Ack is what lets the *other* device stop waiting. Completing without it leaves
        // the receiver on a spinner after a sync that succeeded.
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()

            signalHandler()(SyncSignal.ImportComplete)
            advanceUntilIdle()

            verify { connections.sendSignal(SyncSignal.Ack) }
            verify { connections.disconnect() }
            assertThat(vm.uiState.value.connectionState)
                .isInstanceOf(ConnectionState.Completed::class.java)
        }
    }

    @Test
    fun `an Ack from the partner finishes this side too`() {
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()

            signalHandler()(SyncSignal.Ack)
            advanceUntilIdle()

            assertThat(vm.uiState.value.connectionState)
                .isInstanceOf(ConnectionState.Completed::class.java)
            assertThat(vm.uiState.value.stepsCompleted)
                .isEqualTo(vm.uiState.value.totalSteps)
            verify { connections.disconnect() }
        }
    }

    @Test
    fun `a Ready signal is logged without changing the connection state`() {
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()

            signalHandler()(SyncSignal.Ready)
            advanceUntilIdle()

            assertThat(vm.uiState.value.activityLog.map { it.label }).contains("Sender is ready")
            assertThat(vm.uiState.value.connectionState).isEqualTo(ConnectionState.Idle)
        }
    }

    // ── Cancelling ───────────────────────────────────────────────────────────────────────────

    @Test
    fun `cancelling a live connection tells the partner before stopping`() {
        // Stopping without signalling leaves the other device waiting on a connection that is
        // already gone — it learns only from a timeout.
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()
            connectionState.value = ConnectionState.Transferring(0.5f)
            advanceUntilIdle()

            vm.onEvent(SyncEvent.Cancel)
            advanceUntilIdle()

            verify { connections.cancelWithSignal() }
            verify { connections.stopAll() }
        }
    }

    @Test
    fun `cancelling before anything is connected does not signal a partner there is none`() {
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()
            vm.onEvent(SyncEvent.StartSend)
            advanceUntilIdle()

            vm.onEvent(SyncEvent.Cancel)
            advanceUntilIdle()

            verify(exactly = 0) { connections.cancelWithSignal() }
            verify { connections.stopAll() }
        }
    }

    @Test
    fun `cancelling after a finished sync resets the screen rather than marking it cancelled`() {
        // This is the "Done" button's path. Marking a sync that succeeded as cancelled would
        // be the last thing the user saw about it.
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()
            signalHandler()(SyncSignal.Ack)
            advanceUntilIdle()

            vm.onEvent(SyncEvent.Cancel)
            advanceUntilIdle()

            assertThat(vm.uiState.value).isEqualTo(SyncUiState())
            verify(exactly = 0) { connections.cancelWithSignal() }
        }
    }

    @Test
    fun `cancelling an already-cancelled sync also resets rather than re-cancelling`() {
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()
            connectionState.value = ConnectionState.Cancelled(CancelReason.BY_PARTNER)
            advanceUntilIdle()

            vm.onEvent(SyncEvent.Cancel)
            advanceUntilIdle()

            assertThat(vm.uiState.value).isEqualTo(SyncUiState())
        }
    }

    @Test
    fun `accepting and rejecting reach the transport with the endpoint they name`() {
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()

            vm.onEvent(SyncEvent.AcceptConnection("endpoint-7"))
            vm.onEvent(SyncEvent.RejectConnection("endpoint-9"))
            advanceUntilIdle()

            verify { connections.acceptConnection("endpoint-7") }
            verify { connections.rejectConnection("endpoint-9") }
        }
    }

    // ── Export failure ───────────────────────────────────────────────────────────────────────

    @Test
    fun `an export that throws is reported to the user, not only swallowed`() {
        // Without the error state the sender's progress bar simply stops, and the receiver
        // waits for data that will never arrive.
        coEvery { exporter.export(any()) } throws IllegalStateException("database locked")
        val vm = viewModel()
        runTest(dispatcher) {
            advanceUntilIdle()
            vm.onEvent(SyncEvent.StartSend)
            advanceUntilIdle()

            connectionState.value = ConnectionState.Connected("e1", "Pixel 8")
            advanceUntilIdle()

            assertThat(vm.uiState.value.error).isNotNull()
            assertThat(vm.uiState.value.error!!.details).isEqualTo("database locked")
            assertThat(telemetry.errors.map { it.type }).contains("export")
            assertThat(vm.uiState.value.activityLog.map { it.label })
                .contains("Export failed: database locked")
        }
    }

    // ── Byte formatting ──────────────────────────────────────────────────────────────────────

    @Test
    fun `byte sizes are reported in the unit a person can read`() {
        // This is what the progress caption and the activity log both show. A payload reported
        // as "4194304 B" tells the reader nothing about whether to expect a wait.
        assertThat(SyncViewModel.formatBytes(512)).isEqualTo("512 B")
        assertThat(SyncViewModel.formatBytes(1024)).isEqualTo("1 KB")
        assertThat(SyncViewModel.formatBytes(2048)).isEqualTo("2 KB")
        assertThat(SyncViewModel.formatBytes(1024L * 1024)).contains("MB")
    }

    @Test
    fun `the boundary between each unit falls where the reader expects`() {
        // 1023 bytes is not "0 KB", and 1 MB minus a byte is not "0.9 MB rounded to 1024 KB".
        assertThat(SyncViewModel.formatBytes(1023)).isEqualTo("1023 B")
        assertThat(SyncViewModel.formatBytes(1024L * 1024 - 1)).isEqualTo("1023 KB")
    }
}


/** `onCleared` is protected on `ViewModel`; the test lives in the same module, not the same package. */
private fun SyncViewModel.callOnCleared() {
    val method = androidx.lifecycle.ViewModel::class.java
        .getDeclaredMethod("onCleared")
        .apply { isAccessible = true }
    method.invoke(this)
}
