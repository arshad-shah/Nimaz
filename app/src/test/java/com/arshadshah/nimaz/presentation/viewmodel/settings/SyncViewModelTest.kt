package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.data.sync.ConnectionState
import com.arshadshah.nimaz.data.sync.NearbyConnectionsManager
import com.arshadshah.nimaz.data.sync.SyncDataExporter
import com.arshadshah.nimaz.data.sync.SyncDataImporter
import com.google.common.truth.Truth.assertThat
import io.mockk.every
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
}

/** `onCleared` is protected on `ViewModel`; the test lives in the same module, not the same package. */
private fun SyncViewModel.callOnCleared() {
    val method = androidx.lifecycle.ViewModel::class.java
        .getDeclaredMethod("onCleared")
        .apply { isAccessible = true }
    method.invoke(this)
}
