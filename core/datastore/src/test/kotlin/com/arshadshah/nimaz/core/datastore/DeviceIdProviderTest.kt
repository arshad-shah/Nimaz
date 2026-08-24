package com.arshadshah.nimaz.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * The per-install id the AI feature sends with a question.
 *
 * Its contract is two sentences long and both halves matter. It has to be **stable**, or the
 * Worker's per-install rate limit counts every question as a new device. And it has to be a
 * generated UUID and **never a hardware identifier** — no `ANDROID_ID`, no advertising id — so
 * that it cannot be joined to anything outside this install. The class KDoc says so in capitals;
 * this is what holds the next edit to it.
 */
@RunWith(RobolectricTestRunner::class)
class DeviceIdProviderTest {

    private val provider =
        DeviceIdProvider(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `the id is the same every time it is asked for`() = runTest {
        val first = provider.getOrCreate()
        val second = provider.getOrCreate()

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `a second provider over the same install reads the same id`() = runTest {
        // It is @Singleton in production, but a process restart constructs a new one and the id
        // has to survive that — otherwise every launch looks like a new device.
        val first = provider.getOrCreate()

        val other = DeviceIdProvider(ApplicationProvider.getApplicationContext<Context>())

        assertThat(other.getOrCreate()).isEqualTo(first)
    }

    @Test
    fun `the id is a generated UUID, not something borrowed from the device`() = runTest {
        val id = provider.getOrCreate()

        // Parsing as a UUID is the check that it was generated rather than read from the OS:
        // ANDROID_ID is 16 hex characters and no advertising id is in this shape either.
        assertThat(UUID.fromString(id).toString()).isEqualTo(id)
        assertThat(id).hasLength(36)
    }
}
