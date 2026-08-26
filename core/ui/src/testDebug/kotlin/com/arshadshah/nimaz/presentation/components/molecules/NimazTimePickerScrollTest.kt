package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.arshadshah.nimaz.presentation.foundation.time.NimazTime
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Scrolling the wheels, which is the only way the picker ever reports anything.
 *
 * Every value the picker produces comes out of one `snapshotFlow` on the wheel's first visible
 * item — it reports **while scrolling** rather than when the fling settles, so a dialog's subtitle
 * tracks the wheel instead of jumping at the end. That is also why it has to guard against
 * reporting the index it last reported: without `index != lastReported` a single scroll emits the
 * same value on every frame the list is still on that item, and a caller writing each one to
 * DataStore would issue dozens of writes per drag.
 *
 * The conversion on the way out is the other half, and it differs by clock: a 24-hour wheel reports
 * the hour directly, a 12-hour one has to fold the meridiem back in. A wheel wired to the wrong
 * branch sets a 9 PM reminder to 9 AM.
 *
 * The wheels are driven with `performScrollToIndex` on the hour wheel's own lazy list rather than
 * with a swipe. A swipe has to land inside a 138dp-tall wheel sitting at the top of the screen,
 * and one aimed at the root's centre travels empty space below the picker and reports nothing —
 * which reads as the wheel not reporting rather than the gesture missing it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazTimePickerScrollTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `scrolling a 24-hour wheel reports an hour of the day`() {
        val reported = mutableListOf<NimazTime>()
        composeRule.setThemedContent {
            Box(Modifier.fillMaxWidth()) {
                NimazTimePicker(
                    value = NimazTime(12, 0),
                    onValueChange = { reported += it },
                    is24Hour = true,
                )
            }
        }

        composeRule.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(18)
        composeRule.waitForIdle()

        assertThat(reported).isNotEmpty()
        assertThat(reported.all { it.hour in 0..23 }).isTrue()
        assertThat(reported.all { it.minute == 0 }).isTrue()
    }

    @Test
    fun `scrolling a 12-hour wheel keeps the meridiem it started on`() {
        // The conversion branch. A wheel wired to the 24-hour arm would report the raw 1..12 value
        // and turn a 9 PM reminder into 9 AM.
        val reported = mutableListOf<NimazTime>()
        composeRule.setThemedContent {
            Box(Modifier.fillMaxWidth()) {
                NimazTimePicker(
                    value = NimazTime(21, 30),
                    onValueChange = { reported += it },
                    is24Hour = false,
                )
            }
        }

        // Index 10 is hour 11 on a 1..12 wheel; 21:30 opens on index 8, so a smaller move would
        // land where the wheel already is and report nothing.
        composeRule.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(10)
        composeRule.waitForIdle()

        assertThat(reported).isNotEmpty()
        assertThat(reported.all { it.isPm }).isTrue()
    }

    @Test
    fun `a scroll that lands where it started reports nothing`() {
        // `index != lastReported`. Without it a drag that ends on the same item still emits, and a
        // caller persisting each value would write dozens of times for one gesture.
        val reported = mutableListOf<NimazTime>()
        composeRule.setThemedContent {
            Box(Modifier.fillMaxWidth()) {
                NimazTimePicker(
                    value = NimazTime(6, 0),
                    onValueChange = { reported += it },
                    is24Hour = true,
                )
            }
        }

        composeRule.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(6)
        composeRule.waitForIdle()

        assertThat(reported).isEmpty()
    }
}
