package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Typing into the stepper, rather than tapping it.
 *
 * The editable field is how somebody sets a make-up count of 47 without pressing a button 47
 * times, and it is guarded in three ways that only a typing test reaches.
 *
 * **The input is filtered as it is typed** — digits only, and a minus sign only in the first
 * position and only when the range actually goes negative. Without that a prayer-time adjustment
 * accepts "1-2-3" and commits nothing, and the user is left with a field they cannot clear.
 *
 * **The committed value is clamped.** A field is not a stepper button: nothing stops somebody
 * typing 9999 into a count whose maximum is 30, and the clamp is what keeps the stored value
 * inside the range the screen above it expects.
 *
 * **An empty or unparseable field is dropped rather than committed as zero.** Clearing the field
 * to retype is a normal thing to do mid-edit, and committing 0 on the way through would wipe the
 * value the user was editing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazNumberStepperFieldTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun field() = composeRule.onNode(hasSetTextAction())

    private fun stepper(
        value: Int = 5,
        minValue: Int = 0,
        maxValue: Int = 30,
        onValueChange: (Int) -> Unit,
    ) {
        composeRule.setThemedContent {
            NimazNumberStepper(
                value = value,
                onValueChange = onValueChange,
                minValue = minValue,
                maxValue = maxValue,
                label = "Count",
                formatValue = { it.toString() },
            )
        }
    }

    @Test
    fun `a typed number is committed when the edit is finished`() {
        val reported = mutableListOf<Int>()
        stepper(onValueChange = { reported += it })

        field().performClick()
        field().performTextClearance()
        field().performTextInput("12")
        field().performImeAction()

        assertThat(reported).contains(12)
    }

    @Test
    fun `letters and stray punctuation never reach the field`() {
        // Filtered as typed, not validated on commit — so the user never sees a character they
        // cannot remove.
        val reported = mutableListOf<Int>()
        stepper(onValueChange = { reported += it })

        field().performClick()
        field().performTextClearance()
        field().performTextInput("1a2-b")
        field().performImeAction()

        assertThat(reported).contains(12)
    }

    @Test
    fun `a value above the ceiling is clamped rather than stored`() {
        // Nothing stops somebody typing 9999 into a count whose maximum is 30.
        val reported = mutableListOf<Int>()
        stepper(onValueChange = { reported += it })

        field().performClick()
        field().performTextClearance()
        field().performTextInput("9999")
        field().performImeAction()

        assertThat(reported.last()).isEqualTo(30)
    }

    @Test
    fun `a negative sign is accepted only where the range allows one`() {
        // A prayer-time adjustment runs from -60 to +60 and a make-up count from 0 up. The minus
        // is allowed by the *range*, not by the component, and only in the first position.
        val adjustments = mutableListOf<Int>()
        composeRule.setThemedContent {
            NimazNumberStepper(
                value = 0,
                onValueChange = { adjustments += it },
                minValue = -60,
                maxValue = 60,
                label = "Adjustment",
                formatValue = { it.toString() },
            )
        }

        field().performClick()
        field().performTextClearance()
        field().performTextInput("-15")
        field().performImeAction()

        assertThat(adjustments).contains(-15)
    }

    @Test
    fun `an emptied field commits nothing`() {
        // Clearing to retype is normal mid-edit; committing 0 on the way through wipes the value
        // the user was in the middle of changing.
        val reported = mutableListOf<Int>()
        stepper(onValueChange = { reported += it })

        field().performClick()
        field().performTextClearance()
        field().performImeAction()

        assertThat(reported).isEmpty()
    }
}
