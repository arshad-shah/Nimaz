package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaAccuracyBar
import com.arshadshah.nimaz.presentation.components.molecules.qibla.QiblaAccuracyPill
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QiblaAccuracyIndicatorTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `bar renders accuracy heading and high label`() {
        composeRule.setThemedContent {
            QiblaAccuracyBar(accuracy = CompassAccuracy.HIGH, onCalibrate = {})
        }
        composeRule.onNodeWithText("Compass Accuracy").assertExists()
        composeRule.onNodeWithText("High").assertExists()
    }

    @Test
    fun `bar hides calibrate button when accuracy is high`() {
        composeRule.setThemedContent {
            QiblaAccuracyBar(accuracy = CompassAccuracy.HIGH, onCalibrate = {})
        }
        composeRule.onNodeWithText("Calibrate").assertDoesNotExist()
    }

    @Test
    fun `bar shows calibrate button when accuracy is low`() {
        composeRule.setThemedContent {
            QiblaAccuracyBar(accuracy = CompassAccuracy.LOW, onCalibrate = {})
        }
        composeRule.onNodeWithText("Low").assertExists()
        composeRule.onNodeWithText("Calibrate").assertExists()
    }

    @Test
    fun `bar calibrate button fires callback`() {
        var fired = false
        composeRule.setThemedContent {
            QiblaAccuracyBar(accuracy = CompassAccuracy.UNRELIABLE, onCalibrate = { fired = true })
        }
        composeRule.onNodeWithText("Calibrate").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `pill renders accuracy label`() {
        composeRule.setThemedContent {
            QiblaAccuracyPill(accuracy = CompassAccuracy.HIGH)
        }
        composeRule.onNodeWithText("High").assertExists()
    }
}
