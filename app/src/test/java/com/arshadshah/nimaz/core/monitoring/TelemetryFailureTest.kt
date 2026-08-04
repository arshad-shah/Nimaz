package com.arshadshah.nimaz.core.monitoring

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import org.junit.Test

/**
 * Pins the contract that a failure reaches **both** monitoring channels.
 *
 * `AppAnalytics`'s own KDoc documents the pairing: Crashlytics carries the stack
 * trace, analytics carries the frequency and the affected-user share. Across the
 * ViewModel layer only 41 catch sites cover 293 `viewModelScope.launch` calls, and
 * where a catch does exist it usually reports to one channel or neither. A single
 * `failure()` entry point makes "reported to both" the default rather than a thing
 * each call site has to remember.
 */
class TelemetryFailureTest {

    private val telemetry = RecordingTelemetry()

    @Test
    fun `a failure is recorded to Crashlytics and to analytics`() {
        val boom = IllegalStateException("no such table: quran_ayah")

        telemetry.failure(domain = "quran", type = "load_surah", throwable = boom)

        assertThat(telemetry.exceptions).containsExactly(boom)
        assertThat(telemetry.errors).containsExactly(
            TelemetryCall.Error("quran", "load_surah", "no such table: quran_ayah")
        )
    }

    @Test
    fun `both channels are used for one failure, never just one`() {
        telemetry.failure("settings", "reschedule", RuntimeException("nope"))

        // Order matters only in that both must be present; assert on kinds so the
        // implementation stays free to reorder.
        assertThat(telemetry.calls.map { it::class })
            .containsExactly(TelemetryCall.Exception::class, TelemetryCall.Error::class)
    }

    @Test
    fun `a null exception message still reports both channels`() {
        val messageless = RuntimeException()

        telemetry.failure("sync", "import", messageless)

        assertThat(telemetry.exceptions).containsExactly(messageless)
        assertThat(telemetry.errors.single().message).isNull()
    }

    @Test
    fun `cancellation is not a failure and is never reported`() {
        // Coroutine cancellation is normal control flow — a cancelled load is not a
        // crash. Reporting it would bury real failures in noise every time a user
        // navigates away mid-load, which the reader ViewModels do constantly.
        telemetry.failure("hadith", "load_chapter", CancellationException("navigated away"))

        assertThat(telemetry.calls).isEmpty()
    }
}
