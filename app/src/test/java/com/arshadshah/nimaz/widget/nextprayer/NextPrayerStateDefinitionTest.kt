package com.arshadshah.nimaz.widget.nextprayer

import androidx.datastore.core.CorruptionException
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Tests for the DataStore [androidx.datastore.core.Serializer] backing the
 * Next Prayer widget. Covers the persistence contract used by Glance:
 * writeTo/readFrom must round-trip state, and unreadable bytes must surface as
 * a CorruptionException (so DataStore can fall back to the default) rather
 * than crashing the widget.
 */
class NextPrayerStateDefinitionTest {

    private val serializer = NextPrayerStateDefinition.NextPrayerWidgetStateSerializer

    private suspend fun roundTrip(state: NextPrayerWidgetState): NextPrayerWidgetState {
        val out = ByteArrayOutputStream()
        serializer.writeTo(state, out)
        return serializer.readFrom(ByteArrayInputStream(out.toByteArray()))
    }

    @Test
    fun `default value is a success with empty data`() {
        assertThat(serializer.defaultValue)
            .isEqualTo(NextPrayerWidgetState.Success(NextPrayerData()))
    }

    @Test
    fun `writeTo then readFrom round-trips a success state`() = runTest {
        val state = NextPrayerWidgetState.Success(
            NextPrayerData(
                prayerName = "Maghrib", prayerTime = "8:30 PM", countdown = "12m 4s",
                isValid = true, nextPrayerEpochMillis = 1_700_000_000_000
            )
        )
        assertThat(roundTrip(state)).isEqualTo(state)
    }

    @Test
    fun `writeTo then readFrom round-trips loading and error states`() = runTest {
        assertThat(roundTrip(NextPrayerWidgetState.Loading))
            .isEqualTo(NextPrayerWidgetState.Loading)
        assertThat(roundTrip(NextPrayerWidgetState.Error("network")))
            .isEqualTo(NextPrayerWidgetState.Error("network"))
    }

    @Test
    fun `readFrom surfaces unreadable bytes as a CorruptionException`() = runTest {
        val garbage = ByteArrayInputStream("this is not valid json {".toByteArray())
        try {
            serializer.readFrom(garbage)
            fail("expected CorruptionException for unreadable data")
        } catch (e: CorruptionException) {
            // expected — DataStore will replace the file with defaultValue
            assertThat(e.message).contains("Could not read NextPrayer data")
        }
    }
}
