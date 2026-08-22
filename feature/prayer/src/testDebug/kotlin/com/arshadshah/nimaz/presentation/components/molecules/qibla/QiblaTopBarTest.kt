package com.arshadshah.nimaz.presentation.components.molecules.qibla

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.components.molecules.createComponentComposeRule
import com.arshadshah.nimaz.presentation.components.molecules.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [QiblaTopBar].
 * New design: title + location/accuracy subtitle + camera icon + calibrate icon.
 */
@RunWith(RobolectricTestRunner::class)
class QiblaTopBarTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun rendersLocationNameWhenProvided() {
        composeRule.setThemedContent {
            QiblaTopBar(
                locationName = "London, UK",
                accuracy = CompassAccuracy.HIGH,
                isArMode = false,
                onCameraToggle = {},
                onCalibrate = {},
            )
        }
        composeRule.onNodeWithText("London, UK").assertExists()
    }

    @Test
    fun rendersAccuracyLabel() {
        composeRule.setThemedContent {
            QiblaTopBar(
                locationName = "London, UK",
                accuracy = CompassAccuracy.MEDIUM,
                isArMode = false,
                onCameraToggle = {},
                onCalibrate = {},
            )
        }
        composeRule.onNodeWithText("Medium").assertExists()
    }

    @Test
    fun cameraToggleCallbackFires() {
        var fired = false
        composeRule.setThemedContent {
            QiblaTopBar(
                locationName = "London, UK",
                accuracy = CompassAccuracy.HIGH,
                isArMode = false,
                onCameraToggle = { fired = true },
                onCalibrate = {},
            )
        }
        composeRule.onNodeWithContentDescription("Point with the camera").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun calibrateCallbackFires() {
        var fired = false
        composeRule.setThemedContent {
            QiblaTopBar(
                locationName = null,
                accuracy = CompassAccuracy.LOW,
                isArMode = false,
                onCameraToggle = {},
                onCalibrate = { fired = true },
            )
        }
        composeRule.onNodeWithContentDescription("Calibrate Compass").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun cameraButtonShowsBackLabelInArMode() {
        composeRule.setThemedContent {
            QiblaTopBar(
                locationName = null,
                accuracy = CompassAccuracy.HIGH,
                isArMode = true,
                onCameraToggle = {},
                onCalibrate = {},
            )
        }
        composeRule.onNodeWithContentDescription("Back to the compass").assertIsDisplayed()
    }
}
