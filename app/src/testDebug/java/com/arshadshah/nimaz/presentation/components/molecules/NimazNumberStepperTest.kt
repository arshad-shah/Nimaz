package com.arshadshah.nimaz.presentation.components.molecules

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
}
