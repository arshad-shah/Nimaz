package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazNumberStepperTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders label and default positive formatted value`() {
        composeRule.setThemedContent {
            NimazNumberStepper(
                label = "Fajr",
                value = 3,
                onValueChange = {}
            )
        }

        composeRule.onNodeWithText("Fajr").assertExists()
        composeRule.onNodeWithText("+3").assertExists()
    }

    @Test
    fun `renders non-positive value without plus prefix`() {
        composeRule.setThemedContent {
            NimazNumberStepper(
                label = "Offset",
                value = 0,
                onValueChange = {}
            )
        }

        composeRule.onNodeWithText("0").assertExists()
    }

    @Test
    fun `uses custom formatter when provided`() {
        composeRule.setThemedContent {
            NimazNumberStepper(
                label = "Daily Target",
                value = 20,
                onValueChange = {},
                formatValue = { "$it ayahs" }
            )
        }

        composeRule.onNodeWithText("20 ayahs").assertExists()
    }

    @Test
    fun `increment button raises value by step`() {
        var captured = -1
        composeRule.setThemedContent {
            NimazNumberStepper(
                label = "Count",
                value = 5,
                onValueChange = { captured = it },
                step = 2
            )
        }

        composeRule.onNodeWithContentDescription("Increase").performClick()
        assertThat(captured).isEqualTo(7)
    }

    @Test
    fun `decrement button lowers value by step`() {
        var captured = -1
        composeRule.setThemedContent {
            NimazNumberStepper(
                label = "Count",
                value = 5,
                onValueChange = { captured = it },
                step = 3
            )
        }

        composeRule.onNodeWithContentDescription("Decrease").performClick()
        assertThat(captured).isEqualTo(2)
    }

    @Test
    fun `repeated increments keep advancing from the latest value`() {
        // Regression: the hold-to-repeat gesture outlives recompositions; if it
        // captured a stale onStep it would step from the original value forever
        // ("stuck on first increment"). Drive real state and click three times.
        composeRule.setThemedContent {
            var v by remember { mutableStateOf(5) }
            NimazNumberStepper(
                label = "Count",
                value = v,
                onValueChange = { v = it },
                minValue = 0,
                maxValue = 100,
                step = 1
            )
        }

        val increase = composeRule.onNodeWithContentDescription("Increase")
        increase.performClick()
        increase.performClick()
        increase.performClick()

        // 5 -> 8 (INLINE shows the "+" prefix for positives)
        composeRule.onNodeWithText("+8").assertExists()
    }

    @Test
    fun `decrement disabled at min and increment disabled at max`() {
        composeRule.setThemedContent {
            NimazNumberStepper(
                label = "Bounded",
                value = 5,
                onValueChange = {},
                minValue = 5,
                maxValue = 5
            )
        }

        composeRule.onNodeWithContentDescription("Decrease").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Increase").assertIsNotEnabled()
    }

    @Test
    fun `buttons enabled within bounds`() {
        composeRule.setThemedContent {
            NimazNumberStepper(
                label = "Bounded",
                value = 5,
                onValueChange = {},
                minValue = 0,
                maxValue = 10
            )
        }

        composeRule.onNodeWithContentDescription("Decrease").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Increase").assertIsEnabled()
    }

    // --- SPREAD variant ---------------------------------------------------

    @Test
    fun `spread renders plain value and ignores label`() {
        composeRule.setThemedContent {
            NimazNumberStepper(
                value = 33,
                onValueChange = {},
                variant = NimazNumberStepperVariant.SPREAD,
                label = "ignored"
            )
        }

        // SPREAD's default formatter shows the plain number (no "+" prefix).
        composeRule.onNodeWithText("33").assertExists()
        composeRule.onNodeWithText("ignored").assertDoesNotExist()
    }

    @Test
    fun `spread buttons fire callbacks`() {
        var captured = -1
        composeRule.setThemedContent {
            NimazNumberStepper(
                value = 33,
                onValueChange = { captured = it },
                variant = NimazNumberStepperVariant.SPREAD
            )
        }

        composeRule.onNodeWithContentDescription("Increase").performClick()
        assertThat(captured).isEqualTo(34)

        composeRule.onNodeWithContentDescription("Decrease").performClick()
        assertThat(captured).isEqualTo(32)
    }

    @Test
    fun `spread respects min bound`() {
        composeRule.setThemedContent {
            NimazNumberStepper(
                value = 1,
                onValueChange = {},
                variant = NimazNumberStepperVariant.SPREAD,
                minValue = 1,
                maxValue = 9999
            )
        }

        composeRule.onNodeWithContentDescription("Decrease").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Increase").assertIsEnabled()
    }

    // --- step clamping ----------------------------------------------------

    @Test
    fun `increment coerces to maxValue when step would overshoot`() {
        var captured = -1
        composeRule.setThemedContent {
            NimazNumberStepper(
                label = "Daily Target",
                value = 198,
                onValueChange = { captured = it },
                minValue = 1,
                maxValue = 200,
                step = 5
            )
        }

        composeRule.onNodeWithContentDescription("Increase").performClick()
        assertThat(captured).isEqualTo(200)
    }

    @Test
    fun `decrement coerces to minValue when step would undershoot`() {
        var captured = -1
        composeRule.setThemedContent {
            NimazNumberStepper(
                label = "Daily Target",
                value = 3,
                onValueChange = { captured = it },
                minValue = 1,
                maxValue = 200,
                step = 5
            )
        }

        composeRule.onNodeWithContentDescription("Decrease").performClick()
        assertThat(captured).isEqualTo(1)
    }
}
