package com.arshadshah.nimaz.preferences

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.domain.model.PinnedShortcut
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * The pin row through a real DataStore.
 *
 * The *rule* — cap, order, unknown keys, duplicates — is `PinnedShortcutCodecTest`, on the JVM,
 * where it runs on every commit. What only a device can prove is that the round trip actually
 * survives the store: a `stringPreferencesKey` written and read back at the same type, and the
 * flow re-emitting after a write.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PinnedShortcutsTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var settings: SettingsRepository

    @Before
    fun setup() = hiltRule.inject()

    @Test
    fun pinnedShortcuts_roundTripPreservesOrder() = runTest {
        val order = listOf(PinnedShortcut.ZAKAT, PinnedShortcut.TASBIH, PinnedShortcut.KHATAM)
        settings.setPinnedShortcuts(order)
        // Order is the whole point — a Set would lose it, which is why this is a delimited string.
        assertThat(settings.pinnedShortcuts.first()).containsExactlyElementsIn(order).inOrder()
    }

    @Test
    fun pinnedShortcuts_areCappedOnWrite() = runTest {
        settings.setPinnedShortcuts(PinnedShortcut.entries.toList())
        assertThat(settings.pinnedShortcuts.first()).hasSize(PinnedShortcut.MAX_PINS)
    }

    @Test
    fun pinnedShortcuts_emptyIsHonouredRatherThanResetToDefaults() = runTest {
        // Unpinning the last shortcut has to stick, or the row springs back to four.
        settings.setPinnedShortcuts(emptyList())
        assertThat(settings.pinnedShortcuts.first()).isEmpty()
    }

    @Test
    fun pinnedShortcuts_aSecondWriteReplacesTheFirst() = runTest {
        settings.setPinnedShortcuts(listOf(PinnedShortcut.QIBLA, PinnedShortcut.FASTING))
        settings.setPinnedShortcuts(listOf(PinnedShortcut.QAIDA))
        assertThat(settings.pinnedShortcuts.first()).containsExactly(PinnedShortcut.QAIDA)
    }
}
