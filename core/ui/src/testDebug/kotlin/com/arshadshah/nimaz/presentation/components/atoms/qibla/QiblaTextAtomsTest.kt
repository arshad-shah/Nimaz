package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QiblaTextAtomsTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `formatCoordinates uses N and W hemispheres`() {
        val result = formatCoordinates(latitude = 51.5074, longitude = -0.1278)
        assertThat(result).isEqualTo("51.5074°N, 0.1278°W")
    }

    @Test
    fun `formatCoordinates uses S and E hemispheres`() {
        val result = formatCoordinates(latitude = -33.8688, longitude = 151.2093)
        assertThat(result).isEqualTo("33.8688°S, 151.2093°E")
    }

    @Test
    fun `location label shows name and coordinates`() {
        composeRule.setThemedContent {
            QiblaLocationLabel(
                locationName = "London, UK",
                latitude = 51.5074,
                longitude = -0.1278,
                fallbackTitle = "Qibla",
            )
        }
        composeRule.onNodeWithText("London, UK").assertExists()
        composeRule.onNodeWithText("51.5074°N, 0.1278°W").assertExists()
    }

    @Test
    fun `location label falls back to title when no name`() {
        composeRule.setThemedContent {
            QiblaLocationLabel(
                locationName = null,
                latitude = null,
                longitude = null,
                fallbackTitle = "Qibla",
            )
        }
        composeRule.onNodeWithText("Qibla").assertExists()
    }

    @Test
    fun `calibration step renders number and text`() {
        composeRule.setThemedContent {
            CalibrationStep(number = "1", text = "Hold your phone away from metal")
        }
        composeRule.onNodeWithText("1").assertExists()
        composeRule.onNodeWithText("Hold your phone away from metal").assertExists()
    }
}
