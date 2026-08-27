package com.arshadshah.nimaz.presentation.components.molecules.qibla

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The calibration sheet: the only thing standing between an uncalibrated magnetometer and a
 * reader who trusts the needle.
 *
 * Two things here are worth pinning. The **live accuracy readout** is what tells someone whether
 * the figure-8 they are waving is working — a sheet that renders a fixed label, or the wrong
 * label for the reading it was handed, turns the whole gesture into superstition. And the sheet
 * has **two ways out** (the footer button and the header close), both wired to the same
 * `onDismiss`; a sheet whose only exit is the system back gesture is a trap on a screen the
 * reader opened by accident.
 *
 * Assertions are `assertExists` rather than `assertIsDisplayed`: with the frame clock pinned the
 * sheet's slide-in never completes, so its content is composed and attached but still parked
 * below the viewport. What is being pinned here is *what the sheet says*, not where it sits.
 *
 * **The clock is pinned manually.** `Figure8Animation` runs `rememberInfiniteTransition`, which
 * never lets the frame clock idle: with `autoAdvance` left on, `waitForIdle()` on this sheet runs
 * until the test times out. See #604's note on indeterminate indicators.
 */
@RunWith(RobolectricTestRunner::class)
class QiblaCalibrationSheetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private var dismissals = 0

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun render(accuracy: CompassAccuracy) {
        // Before `setContent`, and only in this class: the sheet always shows the figure-8.
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            QiblaCalibrationSheet(accuracy = accuracy, onDismiss = { dismissals++ })
        }
        composeRule.mainClock.advanceTimeBy(2_000)
    }

    @Test
    fun `the sheet explains the gesture and the three steps`() {
        render(CompassAccuracy.LOW)

        composeRule.onNodeWithText(str(R.string.calibrate_compass)).assertExists()
        composeRule.onNodeWithText(str(R.string.calibration_step_1)).assertExists()
        composeRule.onNodeWithText(str(R.string.calibration_step_2)).assertExists()
        composeRule.onNodeWithText(str(R.string.calibration_step_3)).assertExists()
    }

    @Test
    fun `the readout names the accuracy it was handed`() {
        render(CompassAccuracy.LOW)

        composeRule.onNodeWithText(str(R.string.accuracy_now_label)).assertExists()
        composeRule.onNodeWithText("Low").assertExists()
    }

    @Test
    fun `a high reading is reported as high, not as the same label for every state`() {
        render(CompassAccuracy.HIGH)

        // The label is the feedback loop: waving the phone and watching this word change from
        // "Unreliable" to "High" is the entire point of the sheet.
        composeRule.onNodeWithText("High").assertExists()
        composeRule.onNodeWithText("Low").assertDoesNotExist()
    }

    @Test
    fun `every accuracy the sensor can report has a word for it`() {
        // `MEDIUM` and `UNRELIABLE` take their own colour arms; a `when` that misses one does not
        // compile, but one that maps two readings to the same word ships silently.
        render(CompassAccuracy.MEDIUM)
        composeRule.onNodeWithText("Medium").assertExists()
    }

    @Test
    fun `an unreliable reading is named too`() {
        render(CompassAccuracy.UNRELIABLE)

        composeRule.onNodeWithText("Unreliable").assertExists()
    }

    @Test
    fun `the footer button closes the sheet`() {
        render(CompassAccuracy.LOW)

        composeRule.onNodeWithText(str(R.string.got_it)).performClick()

        assertThat(dismissals).isEqualTo(1)
    }
}
