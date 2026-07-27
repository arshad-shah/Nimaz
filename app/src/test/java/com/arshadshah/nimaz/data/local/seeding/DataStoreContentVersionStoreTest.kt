package com.arshadshah.nimaz.data.local.seeding

import com.arshadshah.nimaz.data.local.dua.DuaContentSeeder
import com.arshadshah.nimaz.data.local.help.HelpContentSeeder
import com.arshadshah.nimaz.data.local.qaida.QaidaContentSeeder
import com.arshadshah.nimaz.data.local.quran.QuranLayoutSeeder
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Pins the legacy-version fallback — the part of the keyed content-version store whose failure
 * mode is silent and expensive.
 *
 * Moving from one DataStore preference per seeder to one keyed preference resets every
 * install's stored version to 0, because the new keys have never been written. Without the
 * fallback, the first launch after upgrading would re-seed **every** bundled asset — ~14k
 * Mushaf layout rows plus the whole Dua, Help and Qaida content set — on devices that already
 * hold exactly that content. Nothing would look broken; it would just do minutes of pointless
 * work and rewrite tables under the user.
 */
class DataStoreContentVersionStoreTest {

    private val indopakKey = QuranLayoutSeeder.contentKey("indopak16")

    private fun settings(
        keyed: Map<String, Int> = emptyMap(),
        indopakLegacy: Int = 0,
        duaLegacy: Int = 0,
        helpLegacy: Int = 0,
        qaidaLegacy: Int = 0
    ): SettingsRepository = mockk(relaxed = true) {
        every { getContentVersion(any()) } answers { flowOf(keyed[firstArg<String>()] ?: 0) }
        every { indopakContentVersion } returns flowOf(indopakLegacy)
        every { duaContentVersion } returns flowOf(duaLegacy)
        every { helpContentVersion } returns flowOf(helpLegacy)
        every { qaidaContentVersion } returns flowOf(qaidaLegacy)
    }

    @Test
    fun `an install that already seeded under the old key does not re-seed`() = runTest {
        // The exact upgrade path: keyed preference unset, legacy preference says v1.
        val store = DataStoreContentVersionStore(settings(indopakLegacy = 1))
        assertThat(store.get(indopakKey)).isEqualTo(1)
    }

    @Test
    fun `every migrated content type inherits its old version`() = runTest {
        val store = DataStoreContentVersionStore(
            settings(indopakLegacy = 1, duaLegacy = 3, helpLegacy = 2, qaidaLegacy = 5)
        )
        assertThat(store.get(indopakKey)).isEqualTo(1)
        assertThat(store.get(DuaContentSeeder.CONTENT_KEY)).isEqualTo(3)
        assertThat(store.get(HelpContentSeeder.CONTENT_KEY)).isEqualTo(2)
        assertThat(store.get(QaidaContentSeeder.CONTENT_KEY)).isEqualTo(5)
    }

    @Test
    fun `the keyed value wins once it has been written`() = runTest {
        // After the first seed the new key holds the truth; a stale legacy value must not
        // shadow it and force a downgrade-then-reseed loop.
        val store = DataStoreContentVersionStore(
            settings(keyed = mapOf(indopakKey to 2), indopakLegacy = 1)
        )
        assertThat(store.get(indopakKey)).isEqualTo(2)
    }

    @Test
    fun `a fresh install reports never-seeded`() = runTest {
        val store = DataStoreContentVersionStore(settings())
        assertThat(store.get(indopakKey)).isEqualTo(0)
    }

    @Test
    fun `a content key with no legacy equivalent reports never-seeded`() = runTest {
        // A layout shipped after this change has no old preference to inherit from.
        val store = DataStoreContentVersionStore(settings(indopakLegacy = 9))
        assertThat(store.get(QuranLayoutSeeder.contentKey("some_future_layout"))).isEqualTo(0)
    }

    @Test
    fun `writes always go to the keyed preference, never the legacy one`() = runTest {
        val settings = settings()
        DataStoreContentVersionStore(settings).set(indopakKey, 4)

        coVerify(exactly = 1) { settings.setContentVersion(indopakKey, 4) }
        coVerify(exactly = 0) { settings.setIndopakContentVersion(any()) }
    }
}
