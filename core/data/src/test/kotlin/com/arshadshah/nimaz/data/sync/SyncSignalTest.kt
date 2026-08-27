package com.arshadshah.nimaz.data.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The wire protocol the two phones coordinate with.
 *
 * Signals travel over the *same* BYTES channel as the payload itself, so both ends have to be
 * able to tell one from the other and neither side can ask. Two properties matter:
 *
 *  - a signal must **decode back to itself**. `ImportProgress` is the only one with fields, and
 *    it drives the sender's progress bar while the receiver imports — decode it wrong and the
 *    sender watches a bar that never moves while the transfer is in fact working;
 *  - **anything that is not a signal must decode to null, not throw.** `onPayloadReceived` tries
 *    `decode` on every BYTES payload that is not gzip-prefixed, so a stray or truncated message
 *    reaching an exception would take the whole transfer down.
 */
class SyncSignalTest {

    private val all = listOf(
        SyncSignal.Ready,
        SyncSignal.Cancel,
        SyncSignal.ImportStarted,
        SyncSignal.ImportComplete,
        SyncSignal.Ack,
        SyncSignal.ImportProgress(step = 3, total = 11, label = "Importing khatam data..."),
    )

    @Test
    fun `every signal survives a round trip through the wire`() {
        all.forEach { signal ->
            assertThat(SyncSignal.decode(SyncSignal.encode(signal))).isEqualTo(signal)
        }
    }

    @Test
    fun `progress carries the numbers the sender's bar is drawn from`() {
        val decoded = SyncSignal.decode(
            SyncSignal.encode(SyncSignal.ImportProgress(3, 11, "Importing khatam data..."))
        ) as SyncSignal.ImportProgress

        assertThat(decoded.step).isEqualTo(3)
        assertThat(decoded.total).isEqualTo(11)
        assertThat(decoded.label).isEqualTo("Importing khatam data...")
    }

    @Test
    fun `no two signals encode to the same bytes`() {
        // Two signals that look alike on the wire would have one silently acted on as the other.
        assertThat(all.map { String(SyncSignal.encode(it)) }).containsNoDuplicates()
    }

    @Test
    fun `a payload that is not a signal decodes to nothing rather than throwing`() {
        // `onPayloadReceived` runs this on every non-gzip BYTES payload it sees.
        assertThat(SyncSignal.decode("not json at all".toByteArray())).isNull()
        assertThat(SyncSignal.decode(byteArrayOf())).isNull()
        assertThat(SyncSignal.decode(byteArrayOf(0x1F, 0x8B.toByte(), 0x08, 0x00))).isNull()
    }

    @Test
    fun `a signal from a newer app version decodes rather than being rejected`() {
        val fromTheFuture =
            """{"type":"import_progress","step":1,"total":2,"label":"x","eta":42}"""

        val decoded = SyncSignal.decode(fromTheFuture.toByteArray())

        // A phone on this version has to keep syncing with one on the next.
        assertThat(decoded).isEqualTo(SyncSignal.ImportProgress(1, 2, "x"))
    }

    @Test
    fun `a signal this version does not know decodes to nothing rather than throwing`() {
        assertThat(SyncSignal.decode("""{"type":"a_signal_from_2027"}""".toByteArray())).isNull()
    }
}
