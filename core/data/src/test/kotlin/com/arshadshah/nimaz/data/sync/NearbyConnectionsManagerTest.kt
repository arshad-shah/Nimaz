package com.arshadshah.nimaz.data.sync

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Device-to-device transfer, driven through a mocked `ConnectionsClient`.
 *
 * The two callbacks that carry all the logic — the connection lifecycle and the payload
 * callback — are private fields, but they are *handed to* the client, so capturing the
 * arguments of `startAdvertising` and `acceptConnection` gets a real reference to each and lets
 * the whole protocol be replayed without radios.
 *
 * Everything here is a defect that produces no error message:
 *
 *  - **a payload arriving on the wrong branch.** Signals, data and file transfers share one
 *    BYTES/FILE channel, and the only thing that tells data from a signal is the leading
 *    `0x1F`. Route a signal into the importer and it tries to import "cancel"; route data into
 *    the signal decoder and the transfer completes having imported nothing;
 *  - **transfer updates for a signal moving the progress bar.** Each signal generates its own
 *    IN_PROGRESS/SUCCESS pair, so without the id filter the bar jumps to 100% and the screen
 *    reports the transfer done while the real payload is still in flight;
 *  - **a disconnect overwriting a finished state.** The partner always disconnects at the end,
 *    so an unguarded `onDisconnected` rewrites a successful sync as "connection lost";
 *  - **the `@Singleton` holding a dead ViewModel.** The data callback captures a
 *    `SyncViewModel`, so without a way to clear it the destroyed ViewModel and its cancelled
 *    scope stay reachable for the life of the process.
 */
@RunWith(RobolectricTestRunner::class)
class NearbyConnectionsManagerTest {

    private lateinit var context: Context
    private lateinit var client: ConnectionsClient
    private lateinit var manager: NearbyConnectionsManager

    private val lifecycle = slot<ConnectionLifecycleCallback>()
    private val discovery = slot<EndpointDiscoveryCallback>()
    private val payloads = slot<PayloadCallback>()
    private val sent = mutableListOf<Payload>()

    private val received = mutableListOf<ByteArray>()
    private val signals = mutableListOf<SyncSignal>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        client = mockk(relaxed = true)
        mockkStatic(Nearby::class)
        every { Nearby.getConnectionsClient(any<Context>()) } returns client

        every {
            client.startAdvertising(any<String>(), any(), capture(lifecycle), any<AdvertisingOptions>())
        } returns Tasks.forResult(null)
        every {
            client.startDiscovery(any(), capture(discovery), any<DiscoveryOptions>())
        } returns Tasks.forResult(null)
        every { client.acceptConnection(any(), capture(payloads)) } returns Tasks.forResult(null)
        every { client.sendPayload(any<String>(), capture(sent)) } returns Tasks.forResult(null)

        manager = NearbyConnectionsManager(context)
        manager.setOnDataReceived { received += it }
        manager.setOnSignalReceived { signals += it }
    }

    @After
    fun tearDown() = unmockkStatic(Nearby::class)

    // ── the connection handshake ──────────────────────────────────────────────

    @Test
    fun `a device starts idle and advertises when asked`() {
        assertThat(NearbyConnectionsManager(context).connectionState.value)
            .isEqualTo(ConnectionState.Idle)

        manager.startAdvertising("Pixel")

        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Advertising)
        verify { client.startAdvertising("Pixel", any(), any(), any<AdvertisingOptions>()) }
    }

    @Test
    fun `an advertising failure is reported rather than leaving the screen advertising`() {
        every {
            client.startAdvertising(any<String>(), any(), any(), any<AdvertisingOptions>())
        } returns Tasks.forException(IllegalStateException("no wifi"))

        manager.startAdvertising("Pixel")
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertThat(manager.connectionState.value).isInstanceOf(ConnectionState.Error::class.java)
        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .contains("no wifi")
    }

    @Test
    fun `discovery requests a connection to the first endpoint it finds`() {
        manager.startDiscovery("Pixel")

        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Discovering)
        discovery.captured.onEndpointFound("E1", mockk<DiscoveredEndpointInfo>(relaxed = true))
        discovery.captured.onEndpointLost("E1")

        verify { client.requestConnection("Pixel", "E1", any()) }
    }

    @Test
    fun `a discovery failure is reported`() {
        every {
            client.startDiscovery(any(), any(), any<DiscoveryOptions>())
        } returns Tasks.forException(IllegalStateException("no permission"))

        manager.startDiscovery("Pixel")
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertThat(manager.connectionState.value).isInstanceOf(ConnectionState.Error::class.java)
    }

    @Test
    fun `an incoming connection surfaces the digits the two users compare`() {
        manager.startAdvertising("Pixel")

        lifecycle.captured.onConnectionInitiated("E1", connectionInfo("Galaxy", "1234"))

        val state = manager.connectionState.value as ConnectionState.Connecting
        assertThat(state.endpointName).isEqualTo("Galaxy")
        // The digits are the only thing standing between this and a stranger's phone.
        assertThat(state.authToken).isEqualTo("1234")
    }

    @Test
    fun `accepting waits for the partner rather than claiming to be connected`() {
        connectTo("Galaxy")
        manager.acceptConnection("E1")

        // Nearby needs *both* sides to accept; showing Connected here is a screen that lies.
        val state = manager.connectionState.value as ConnectionState.WaitingForPartnerAccept
        assertThat(state.endpointName).isEqualTo("Galaxy")
        verify { client.acceptConnection("E1", any()) }
    }

    @Test
    fun `the partner's name survives from the invitation into the connection`() {
        connectTo("Galaxy")
        manager.acceptConnection("E1")
        lifecycle.captured.onConnectionResult("E1", resolution(ConnectionsStatusCodes.STATUS_OK))

        assertThat(manager.connectionState.value)
            .isEqualTo(ConnectionState.Connected("E1", "Galaxy"))
    }

    @Test
    fun `connecting without ever seeing an invitation still connects, namelessly`() {
        manager.startAdvertising("Pixel")

        lifecycle.captured.onConnectionResult("E1", resolution(ConnectionsStatusCodes.STATUS_OK))

        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Connected("E1", ""))
    }

    @Test
    fun `a rejected connection says it was rejected, not that it failed`() {
        connectTo("Galaxy")

        lifecycle.captured.onConnectionResult(
            "E1", resolution(ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED)
        )

        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .isEqualTo("Connection rejected")
    }

    @Test
    fun `any other status is reported with the message the framework gave`() {
        connectTo("Galaxy")

        lifecycle.captured.onConnectionResult("E1", resolution(13, "radio off"))

        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .contains("radio off")
    }

    @Test
    fun `rejecting an invitation returns to idle`() {
        connectTo("Galaxy")

        manager.rejectConnection("E1")

        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Idle)
        verify { client.rejectConnection("E1") }
    }

    // ── disconnects ───────────────────────────────────────────────────────────

    @Test
    fun `a disconnect mid transfer is reported as a lost connection`() {
        connectAndAccept()

        lifecycle.captured.onDisconnected("E1")

        assertThat(manager.connectionState.value)
            .isEqualTo(ConnectionState.Cancelled(CancelReason.CONNECTION_LOST))
    }

    @Test
    fun `a disconnect after a completed transfer does not rewrite the result`() {
        connectAndAccept()
        payloads.captured.onPayloadTransferUpdate("E1", update(id = 9, status = SUCCESS, transferred = 4_096))
        val completed = manager.connectionState.value

        lifecycle.captured.onDisconnected("E1")

        // The partner always disconnects at the end; an unguarded handler turns every
        // successful sync into "connection lost".
        assertThat(completed).isEqualTo(ConnectionState.Completed(4_096))
        assertThat(manager.connectionState.value).isEqualTo(completed)
    }

    @Test
    fun `a disconnect after a cancellation does not rewrite the reason`() {
        connectAndAccept()
        manager.cancelWithSignal()

        lifecycle.captured.onDisconnected("E1")

        assertThat(manager.connectionState.value)
            .isEqualTo(ConnectionState.Cancelled(CancelReason.BY_USER))
    }

    @Test
    fun `a disconnect after an error keeps the error`() {
        connectAndAccept()
        payloads.captured.onPayloadTransferUpdate("E1", update(id = 9, status = FAILURE))

        lifecycle.captured.onDisconnected("E1")

        assertThat(manager.connectionState.value).isInstanceOf(ConnectionState.Error::class.java)
    }

    // ── sending ───────────────────────────────────────────────────────────────

    @Test
    fun `nothing is sent before a connection exists`() {
        manager.sendData("payload".toByteArray())
        manager.sendSignal(SyncSignal.Ready)

        assertThat(sent).isEmpty()
    }

    @Test
    fun `a small export travels as a gzipped BYTES payload behind its marker byte`() {
        connectAndAccept()

        manager.sendData("""{"bookmarks":[]}""".toByteArray())

        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Transferring(0f))
        val bytes = sent.single().asBytes()!!
        assertThat(bytes[0]).isEqualTo(0x1F.toByte())
        assertThat(String(gunzip(bytes.copyOfRange(1, bytes.size))))
            .isEqualTo("""{"bookmarks":[]}""")
    }

    @Test
    fun `an export too large for a BYTES payload falls back to a file`() {
        connectAndAccept()
        manager.sendData(incompressible())

        assertThat(sent.single().type).isEqualTo(Payload.Type.FILE)
        assertThat(File(context.cacheDir, "sync_export.gz").exists()).isTrue()
    }

    @Test
    fun `the temp send file is deleted once the transfer completes`() {
        connectAndAccept()
        manager.sendData(incompressible())
        val temp = File(context.cacheDir, "sync_export.gz")

        payloads.captured.onPayloadTransferUpdate("E1", update(id = 5, status = SUCCESS, transferred = 100))

        assertThat(temp.exists()).isFalse()
    }

    @Test
    fun `a signal is sent and its own transfer updates are ignored`() {
        connectAndAccept()

        manager.sendSignal(SyncSignal.ImportStarted)
        val signalId = sent.single().id
        payloads.captured.onPayloadTransferUpdate("E1", update(signalId, IN_PROGRESS, 10, 100))
        payloads.captured.onPayloadTransferUpdate("E1", update(signalId, SUCCESS, 100, 100))

        // Without the id filter each signal drives the bar to 100% and the screen calls the
        // transfer finished while the real payload is still in flight.
        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Connected("E1", "Galaxy"))
    }

    @Test
    fun `cancelling tells the partner, drops the link and records who cancelled`() {
        connectAndAccept()

        manager.cancelWithSignal()

        assertThat(SyncSignal.decode(sent.single().asBytes()!!)).isEqualTo(SyncSignal.Cancel)
        verify { client.disconnectFromEndpoint("E1") }
        assertThat(manager.connectionState.value)
            .isEqualTo(ConnectionState.Cancelled(CancelReason.BY_USER))
    }

    // ── receiving ─────────────────────────────────────────────────────────────

    @Test
    fun `a gzipped BYTES payload reaches the data handler decompressed`() {
        connectAndAccept()

        payloads.captured.onPayloadReceived(
            "E1", Payload.fromBytes(byteArrayOf(0x1F) + gzip("""{"khatams":[]}""".toByteArray()))
        )

        assertThat(String(received.single())).isEqualTo("""{"khatams":[]}""")
        assertThat(signals).isEmpty()
    }

    @Test
    fun `a corrupt gzipped payload is reported rather than handed on half read`() {
        connectAndAccept()

        payloads.captured.onPayloadReceived(
            "E1", Payload.fromBytes(byteArrayOf(0x1F, 0x8B.toByte(), 0x08, 0x00, 0x00))
        )

        assertThat(received).isEmpty()
        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .contains("decompress")
    }

    @Test
    fun `data arriving with no handler registered is reported, not dropped silently`() {
        connectAndAccept()
        manager.setOnDataReceived(null)

        payloads.captured.onPayloadReceived(
            "E1", Payload.fromBytes(byteArrayOf(0x1F) + gzip("{}".toByteArray()))
        )

        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .contains("No data handler")
    }

    @Test
    fun `a signal payload reaches the signal handler and not the data handler`() {
        connectAndAccept()

        payloads.captured.onPayloadReceived(
            "E1", Payload.fromBytes(SyncSignal.encode(SyncSignal.ImportProgress(2, 11, "Prayer")))
        )

        assertThat(signals.single()).isEqualTo(SyncSignal.ImportProgress(2, 11, "Prayer"))
        // Routing a signal into the importer means trying to import the word "cancel".
        assertThat(received).isEmpty()
    }

    @Test
    fun `a BYTES payload that is neither data nor a signal is ignored`() {
        connectAndAccept()

        payloads.captured.onPayloadReceived("E1", Payload.fromBytes("garbage".toByteArray()))

        assertThat(received).isEmpty()
        assertThat(signals).isEmpty()
        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Connected("E1", "Galaxy"))
    }

    @Test
    fun `a signal arriving with no handler registered does not break the connection`() {
        connectAndAccept()
        manager.setOnSignalReceived(null)

        payloads.captured.onPayloadReceived("E1", Payload.fromBytes(SyncSignal.encode(SyncSignal.Ack)))

        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Connected("E1", "Galaxy"))
    }

    @Test
    fun `an incoming file is read and handed on only once its transfer succeeds`() {
        connectAndAccept()
        val file = File(context.cacheDir, "incoming.gz").apply { writeBytes("the export".toByteArray()) }
        val payload = filePayload(id = 77, uri = Uri.fromFile(file))

        payloads.captured.onPayloadReceived("E1", payload)
        assertThat(received).isEmpty()

        payloads.captured.onPayloadTransferUpdate("E1", update(77, IN_PROGRESS, 50, 100))
        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Transferring(0.5f))

        payloads.captured.onPayloadTransferUpdate("E1", update(77, SUCCESS, 100, 100))
        assertThat(String(received.single())).isEqualTo("the export")
    }

    @Test
    fun `an incoming file that reads back empty is reported`() {
        connectAndAccept()
        val file = File(context.cacheDir, "empty.gz").apply { writeBytes(ByteArray(0)) }

        payloads.captured.onPayloadReceived("E1", filePayload(id = 78, uri = Uri.fromFile(file)))
        payloads.captured.onPayloadTransferUpdate("E1", update(78, SUCCESS, 0, 0))

        assertThat(received).isEmpty()
        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .contains("empty")
    }

    @Test
    fun `a file payload the framework cannot resolve reads as nothing rather than crashing`() {
        connectAndAccept()
        val payload = mockk<Payload>(relaxed = true) {
            every { id } returns 79
            every { type } returns Payload.Type.FILE
            every { asFile() } returns null
        }

        payloads.captured.onPayloadReceived("E1", payload)
        payloads.captured.onPayloadTransferUpdate("E1", update(79, SUCCESS, 0, 0))

        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .contains("empty")
    }

    @Test
    fun `a file whose uri never materialises reads as nothing rather than crashing`() {
        connectAndAccept()
        val payload = mockk<Payload>(relaxed = true) {
            every { id } returns 80
            every { type } returns Payload.Type.FILE
            every { asFile() } returns mockk(relaxed = true) { every { asUri() } returns null }
        }

        payloads.captured.onPayloadReceived("E1", payload)
        payloads.captured.onPayloadTransferUpdate("E1", update(80, SUCCESS, 0, 0))

        assertThat(received).isEmpty()
    }

    @Test
    fun `an unknown payload type is ignored rather than treated as data`() {
        connectAndAccept()
        val payload = mockk<Payload>(relaxed = true) {
            every { id } returns 81
            every { type } returns 99
        }

        payloads.captured.onPayloadReceived("E1", payload)

        assertThat(received).isEmpty()
        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Connected("E1", "Galaxy"))
    }

    @Test
    fun `a failed transfer is reported and its pending payload dropped`() {
        connectAndAccept()
        val file = File(context.cacheDir, "half.gz").apply { writeBytes("half".toByteArray()) }
        payloads.captured.onPayloadReceived("E1", filePayload(id = 82, uri = Uri.fromFile(file)))

        payloads.captured.onPayloadTransferUpdate("E1", update(82, FAILURE))
        payloads.captured.onPayloadTransferUpdate("E1", update(82, SUCCESS, 4, 4))

        // The dropped payload matters: a SUCCESS arriving afterwards must not import a file
        // whose transfer already failed.
        assertThat(received).isEmpty()
        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Completed(4))
    }

    @Test
    fun `a cancelled transfer is reported as cancelled`() {
        connectAndAccept()

        payloads.captured.onPayloadTransferUpdate("E1", update(83, CANCELED))

        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .contains("canceled")
    }

    @Test
    fun `a transfer of unknown length reports no progress rather than dividing by zero`() {
        connectAndAccept()

        payloads.captured.onPayloadTransferUpdate("E1", update(84, IN_PROGRESS, 10, 0))

        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Transferring(0f))
    }

    // ── teardown ──────────────────────────────────────────────────────────────

    @Test
    fun `stopping clears the handlers so a destroyed ViewModel is not held`() {
        connectAndAccept()
        manager.sendData(incompressible())

        manager.stopAll()

        verify { client.stopAdvertising() }
        verify { client.stopDiscovery() }
        verify { client.stopAllEndpoints() }
        assertThat(File(context.cacheDir, "sync_export.gz").exists()).isFalse()

        // The manager is a @Singleton and the callback captures the ViewModel: without this
        // the ViewModel and its cancelled scope outlive the screen for the process's life.
        payloads.captured.onPayloadReceived(
            "E1", Payload.fromBytes(byteArrayOf(0x1F) + gzip("{}".toByteArray()))
        )
        assertThat(received).isEmpty()
        // And nothing is sent afterwards, because the endpoint is forgotten too.
        sent.clear()
        manager.sendSignal(SyncSignal.Ack)
        assertThat(sent).isEmpty()
    }

    @Test
    fun `disconnecting twice is safe`() {
        connectAndAccept()

        manager.disconnect()
        manager.disconnect()

        verify(exactly = 1) { client.disconnectFromEndpoint("E1") }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun connectTo(name: String) {
        manager.startAdvertising("Pixel")
        lifecycle.captured.onConnectionInitiated("E1", connectionInfo(name, "1234"))
    }

    private fun connectAndAccept() {
        connectTo("Galaxy")
        manager.acceptConnection("E1")
        lifecycle.captured.onConnectionResult("E1", resolution(ConnectionsStatusCodes.STATUS_OK))
        sent.clear()
    }

    private fun connectionInfo(name: String, digits: String) =
        mockk<ConnectionInfo>(relaxed = true) {
            every { endpointName } returns name
            every { authenticationDigits } returns digits
        }

    private fun resolution(code: Int, message: String? = null) =
        mockk<ConnectionResolution>(relaxed = true) {
            every { status } returns Status(code, message)
        }

    private fun filePayload(id: Long, uri: Uri): Payload {
        val payloadId = id
        return mockk(relaxed = true) {
            every { this@mockk.id } returns payloadId
            every { type } returns Payload.Type.FILE
            every { asFile() } returns mockk(relaxed = true) { every { asUri() } returns uri }
        }
    }

    /** A STREAM payload whose bytes the manager reads on its own background thread. */
    private fun streamPayload(id: Long, data: ByteArray): Payload {
        val payloadId = id
        return mockk(relaxed = true) {
            every { this@mockk.id } returns payloadId
            every { type } returns Payload.Type.STREAM
            every { asStream() } returns mockk(relaxed = true) {
                every { asInputStream() } returns data.inputStream()
            }
        }
    }

    /** The stream is read off the main thread, so the assertion has to wait for it. */
    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(5)
        assertThat(condition()).isTrue()
    }

    private fun update(
        id: Long,
        status: Int,
        transferred: Long = 0,
        total: Long = 0,
    ): PayloadTransferUpdate = PayloadTransferUpdate.Builder()
        .setPayloadId(id)
        .setStatus(status)
        .setBytesTransferred(transferred)
        .setTotalBytes(total)
        .build()

    /** Bigger than the 31 KB BYTES ceiling *after* gzip, which needs real entropy. */
    private fun incompressible(): ByteArray =
        ByteArray(400_000).also { java.util.Random(42).nextBytes(it) }

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    private fun gunzip(data: ByteArray): ByteArray =
        GZIPInputStream(data.inputStream()).use { it.readBytes() }

    private companion object {
        const val IN_PROGRESS = PayloadTransferUpdate.Status.IN_PROGRESS
        const val SUCCESS = PayloadTransferUpdate.Status.SUCCESS
        const val FAILURE = PayloadTransferUpdate.Status.FAILURE
        const val CANCELED = PayloadTransferUpdate.Status.CANCELED
    }

    @Test
    fun `a partner that accepts before this device does still connects by name`() {
        connectTo("Galaxy")

        // The other side accepted first, so the result arrives while this device is still
        // showing the confirmation digits.
        lifecycle.captured.onConnectionResult("E1", resolution(ConnectionsStatusCodes.STATUS_OK))

        assertThat(manager.connectionState.value)
            .isEqualTo(ConnectionState.Connected("E1", "Galaxy"))
    }

    @Test
    fun `accepting a connection this device never saw an invitation for is nameless`() {
        manager.startAdvertising("Pixel")

        manager.acceptConnection("E1")

        assertThat(manager.connectionState.value)
            .isEqualTo(ConnectionState.WaitingForPartnerAccept("E1", ""))
    }

    // ── the stream transport ──────────────────────────────────────────────────

    @Test
    fun `a streamed export is read off the wire and handed to the importer`() {
        connectAndAccept()

        payloads.captured.onPayloadReceived("E1", streamPayload(90, "the export".toByteArray()))

        // Read on a background thread, so the transfer update below is not what delivers it.
        awaitUntil { received.isNotEmpty() }
        assertThat(String(received.single())).isEqualTo("the export")
    }

    @Test
    fun `a stream that carries nothing is reported rather than imported as empty`() {
        connectAndAccept()

        payloads.captured.onPayloadReceived("E1", streamPayload(91, ByteArray(0)))

        awaitUntil { manager.connectionState.value is ConnectionState.Error }
        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .contains("empty")
        assertThat(received).isEmpty()
    }

    @Test
    fun `a stream with no readable input is reported rather than silently dropped`() {
        connectAndAccept()
        val payload = mockk<Payload>(relaxed = true) {
            every { id } returns 92
            every { type } returns Payload.Type.STREAM
            every { asStream() } returns null
        }

        payloads.captured.onPayloadReceived("E1", payload)

        awaitUntil { manager.connectionState.value is ConnectionState.Error }
        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .contains("Failed to read data")
    }

    @Test
    fun `a stream arriving with no handler registered is reported`() {
        connectAndAccept()
        manager.setOnDataReceived(null)

        payloads.captured.onPayloadReceived("E1", streamPayload(93, "x".toByteArray()))

        awaitUntil { manager.connectionState.value is ConnectionState.Error }
        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .contains("No data handler")
    }

    @Test
    fun `a stream's transfer updates do not drive the progress bar`() {
        connectAndAccept()
        payloads.captured.onPayloadReceived("E1", streamPayload(94, "the export".toByteArray()))
        awaitUntil { received.isNotEmpty() }

        payloads.captured.onPayloadTransferUpdate("E1", update(94, IN_PROGRESS, 50, 100))
        payloads.captured.onPayloadTransferUpdate("E1", update(94, SUCCESS, 100, 100))

        // The reading thread owns completion; a Completed here would end the sync before the
        // data had been imported.
        assertThat(manager.connectionState.value).isEqualTo(ConnectionState.Connected("E1", "Galaxy"))
    }

    @Test
    fun `a stream whose transfer fails is reported`() {
        connectAndAccept()
        payloads.captured.onPayloadReceived("E1", streamPayload(95, "x".toByteArray()))
        awaitUntil { received.isNotEmpty() }

        payloads.captured.onPayloadTransferUpdate("E1", update(95, FAILURE))

        assertThat((manager.connectionState.value as ConnectionState.Error).message)
            .contains("Transfer failed")
    }

    @Test
    fun `a stream whose transfer is cancelled is reported`() {
        connectAndAccept()
        payloads.captured.onPayloadReceived("E1", streamPayload(96, "x".toByteArray()))
        awaitUntil { received.isNotEmpty() }

        payloads.captured.onPayloadTransferUpdate("E1", update(96, CANCELED))

        assertThat(manager.connectionState.value).isInstanceOf(ConnectionState.Error::class.java)
    }
}
