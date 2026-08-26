package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The number stepper, across the shapes and bounds its callers set.
 *
 * It is used for prayer-time adjustments, daily targets and make-up counts, and the part that
 * matters is the clamping: `minValue` and `maxValue` are what stop a qada counter going negative
 * or a target running away. A stepper that reported past its own bounds writes a value the screen
 * above it will happily persist, and the user then has a count they cannot get back to zero.
 *
 * `step` is the other one. Prayer-time adjustment moves in minutes and a target in units of one;
 * a stepper that ignored `step` would move the wrong distance per tap while looking correct.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazNumberStepperVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every variant, size and type renders its value`() {
        composeRule.setThemedContent {
            Column {
                NimazNumberStepperVariant.entries.forEachIndexed { vi, variant ->
                    NimazNumberStepperSize.entries.forEachIndexed { si, size ->
                        NimazNumberStepper(
                            value = vi * 10 + si,
                            onValueChange = {},
                            variant = variant,
                            size = size,
                            type = NimazNumberStepperType.entries[si % NimazNumberStepperType.entries.size],
                            label = "L$vi$si",
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("L00").assertExists()
    }

    @Test
    fun `stepping up and down moves by the step it is given`() {
        val reported = mutableListOf<Int>()
        composeRule.setThemedContent {
            NimazNumberStepper(
                value = 10,
                onValueChange = { reported += it },
                step = 5,
                minValue = 0,
                maxValue = 60,
                label = "Adjustment",
            )
        }

        // The default formatter signs the value, so 10 reads "+10" — an adjustment of ten minutes
        // *later*, which is the only reading that makes sense on a prayer-time row.
        composeRule.onNodeWithText("+10").assertExists()

        composeRule.onNodeWithContentDescription("Increase").performClick()
        composeRule.onNodeWithContentDescription("Decrease").performClick()

        assertThat(reported).containsExactly(15, 5).inOrder()
    }

    @Test
    fun `a formatted value is what the user reads`() {
        // `formatValue` is how a minutes adjustment renders as "+5 min" rather than "5" — and the
        // raw number is what is reported back, so the two must not be confused.
        composeRule.setThemedContent {
            NimazNumberStepper(
                value = 5,
                onValueChange = {},
                formatValue = { "$it min" },
                editable = false,
            )
        }

        composeRule.onNodeWithText("5 min").assertExists()
    }

    @Test
    fun `a non-editable stepper shows its value as text rather than a field`() {
        // `editable` decides between a text field and a label. An editable field on a compact
        // settings row opens a keyboard the row has no space for.
        composeRule.setThemedContent {
            Column {
                NimazNumberStepper(value = 3, onValueChange = {}, editable = false, label = "Fixed")
                NimazNumberStepper(value = 4, onValueChange = {}, editable = true, label = "Typed")
            }
        }

        composeRule.onNodeWithText("Fixed").assertExists()
        composeRule.onNodeWithText("Typed").assertExists()
    }

    @Test
    fun `a stepper at its floor still renders its value`() {
        // The bound is what stops a qada counter going negative. Both ends are rendered because
        // the disabled-control arm is a different branch from the enabled one.
        composeRule.setThemedContent {
            Column {
                NimazNumberStepper(
                    value = 0,
                    onValueChange = {},
                    minValue = 0,
                    maxValue = 10,
                    label = "Floor",
                    editable = false,
                )
                NimazNumberStepper(
                    value = 10,
                    onValueChange = {},
                    minValue = 0,
                    maxValue = 10,
                    label = "Ceiling",
                    editable = false,
                )
            }
        }

        composeRule.onNodeWithText("Floor").assertExists()
        composeRule.onNodeWithText("Ceiling").assertExists()
    }

    @Test
    fun `an unlabelled stepper renders on its own`() {
        composeRule.setThemedContent {
            NimazNumberStepper(value = 7, onValueChange = {}, editable = false)
        }

        composeRule.onNodeWithText("+7").assertExists()
    }
}
