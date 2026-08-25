package com.arshadshah.nimaz.presentation.screens.more

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.PinnedShortcut
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Choosing what sits above the menu — and the cap, which is the whole of the design here.
 *
 * The cap is expressed by **disabling the rows you cannot add** rather than by ignoring the tap,
 * and the exception is the part that has to hold: a *pinned* row is never disabled, whatever the
 * count. Get that wrong and reaching five pins makes the one you want to remove the one row you
 * cannot touch — a dead end with no way out except clearing app data, and one that only appears
 * for people who used the feature enough to fill it.
 *
 * Order is the other property. A new pin is **appended**, never inserted, so adding a sixth
 * favourite does not reshuffle the arrangement someone already has; the sheet hands back the
 * whole list precisely so order is part of what is being set.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class PinnedShortcutsSheetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val changes = mutableListOf<List<PinnedShortcut>>()
    private var dismissals = 0

    private fun setContent(pinned: List<PinnedShortcut>) {
        composeRule.setThemedContent {
            PinnedShortcutsSheet(
                pinned = pinned,
                onPinnedChange = { changes += it },
                onDismiss = { dismissals++ },
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun label(shortcut: PinnedShortcut): String = string(shortcut.labelRes())

    @Test
    fun `every pinnable destination is offered`() {
        setContent(PinnedShortcut.DEFAULTS)

        // A destination missing from this list can never be pinned, and nothing else would say
        // so — the pin row simply would not show it.
        PinnedShortcut.entries.forEach {
            composeRule.onNodeWithText(label(it)).assertExists()
        }
    }

    @Test
    fun `pinning appends rather than inserting`() {
        setContent(listOf(PinnedShortcut.TASBIH, PinnedShortcut.KHATAM))

        composeRule.onNodeWithText(label(PinnedShortcut.QIBLA)).performClick()

        assertThat(changes).containsExactly(
            listOf(PinnedShortcut.TASBIH, PinnedShortcut.KHATAM, PinnedShortcut.QIBLA),
        )
    }

    @Test
    fun `unpinning removes only that shortcut and keeps the rest in order`() {
        setContent(listOf(PinnedShortcut.TASBIH, PinnedShortcut.KHATAM, PinnedShortcut.QIBLA))

        composeRule.onNodeWithText(label(PinnedShortcut.KHATAM)).performClick()

        assertThat(changes).containsExactly(
            listOf(PinnedShortcut.TASBIH, PinnedShortcut.QIBLA),
        )
    }

    @Test
    fun `at the cap an unpinned row does nothing`() {
        setContent(fullList())

        composeRule.onNodeWithText(label(PinnedShortcut.QAIDA)).performClick()

        // Disabled, not silently ignored: the header says why, and the row looks unavailable.
        assertThat(changes).isEmpty()
    }

    @Test
    fun `at the cap a pinned row still unpins`() {
        // The exception that makes the cap survivable. Without it the row you want gone is the
        // one row you cannot reach.
        setContent(fullList())

        composeRule.onNodeWithText(label(PinnedShortcut.TASBIH)).performClick()

        assertThat(changes).hasSize(1)
        assertThat(changes.single()).doesNotContain(PinnedShortcut.TASBIH)
        assertThat(changes.single()).hasSize(PinnedShortcut.MAX_PINS - 1)
    }

    @Test
    fun `the header counts the pins against the cap`() {
        setContent(listOf(PinnedShortcut.TASBIH, PinnedShortcut.KHATAM))

        composeRule.onNodeWithText(string(R.string.more_pins_sheet_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.more_pins_count, 2, PinnedShortcut.MAX_PINS))
            .assertExists()
    }

    @Test
    fun `a full row says so rather than counting`() {
        setContent(fullList())

        composeRule.onNodeWithText(string(R.string.more_pins_full, PinnedShortcut.MAX_PINS))
            .assertExists()
    }

    @Test
    fun `an empty selection says nothing is pinned`() {
        setContent(emptyList())

        composeRule.onNodeWithText(string(R.string.more_pins_empty)).assertExists()
    }

    private fun fullList(): List<PinnedShortcut> =
        PinnedShortcut.entries.take(PinnedShortcut.MAX_PINS)
}
