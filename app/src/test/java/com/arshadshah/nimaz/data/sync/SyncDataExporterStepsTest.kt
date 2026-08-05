package com.arshadshah.nimaz.data.sync

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The send progress bar's arithmetic.
 *
 * `SyncViewModel` hard-coded `SEND_TOTAL_STEPS = 10` — with a comment claiming "7 export
 * callbacks" — against an exporter that reports **eleven** times, and then hand-numbered the
 * encoding and sending steps as 8 and 9. So tapping Send filled the bar to 120%, captioned it
 * "Step 11 of 10", and then visibly **rewound it to 80%**.
 *
 * The count now comes from [SyncDataExporter.STEP_COUNT], and this is what stops that constant
 * drifting away from the calls again: it counts the real callbacks of a real export.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncDataExporterStepsTest {

    private fun exporter() = SyncDataExporter(
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
    )

    @Test
    fun `the exporter reports exactly STEP_COUNT steps`() = runTest {
        val reported = mutableListOf<Triple<Int, Int, String>>()

        exporter().export { completed, total, label ->
            reported += Triple(completed, total, label)
        }

        assertThat(reported).hasSize(SyncDataExporter.STEP_COUNT)
    }

    @Test
    fun `progress is monotonic and never exceeds the total`() = runTest {
        val completedValues = mutableListOf<Int>()

        exporter().export { completed, total, _ ->
            completedValues += completed
            // The defect in one assertion: a step number larger than the total is a bar
            // past 100%, which is what shipped.
            assertThat(completed).isAtMost(total)
        }

        assertThat(completedValues).isInOrder()
        assertThat(completedValues.first()).isEqualTo(1)
        assertThat(completedValues.last()).isEqualTo(SyncDataExporter.STEP_COUNT)
    }
}
