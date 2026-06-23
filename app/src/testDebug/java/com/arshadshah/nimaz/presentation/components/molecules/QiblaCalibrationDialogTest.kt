package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaCalibrationDialog
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QiblaCalibrationDialogTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `dialog renders title and improvement heading`() {
        composeRule.setThemedContent {
            QiblaCalibrationDialog(accuracy = CompassAccuracy.LOW, onDismiss = {})
        }
        composeRule.onNodeWithText("Calibrate Compass").assertExists()
        composeRule.onNodeWithText("To improve compass accuracy:").assertExists()
    }

    @Test
    fun `dialog shows current accuracy text`() {
        composeRule.setThemedContent {
            QiblaCalibrationDialog(accuracy = CompassAccuracy.LOW, onDismiss = {})
        }
        composeRule.onNodeWithText("Current accuracy: Low").assertExists()
    }

    @Test
    fun `dialog confirm button renders`() {
        composeRule.setThemedContent {
            QiblaCalibrationDialog(accuracy = CompassAccuracy.UNRELIABLE, onDismiss = {})
        }
        composeRule.onNodeWithText("Got it").assertExists()
    }

    @Test
    fun `confirm button fires onDismiss`() {
        var dismissed = false
        composeRule.setThemedContent {
            QiblaCalibrationDialog(accuracy = CompassAccuracy.LOW, onDismiss = { dismissed = true })
        }
        // Invoke the button's OnClick semantics action rather than performClick():
        // inside a Dialog sub-window Robolectric lays the action row out with zero
        // height, so a geometry-based click would miss. The semantics action is
        // geometry-independent and exercises the same onClick → onDismiss wiring.
        composeRule.onNode(hasText("Got it") and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        assertThat(dismissed).isTrue()
    }
}
