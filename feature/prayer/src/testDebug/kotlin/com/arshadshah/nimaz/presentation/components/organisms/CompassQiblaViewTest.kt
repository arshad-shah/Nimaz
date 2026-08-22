package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onRoot
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [com.arshadshah.nimaz.presentation.components.organisms.qibla.CompassQiblaView].
 * Despite the "compass" name this composable has NO direct sensor/camera dependency: it
 * just lays out the compass widget + status capsule + accuracy bar and picks a layout via
 * [currentWindowSizeClass] (window-metrics based, fine under Robolectric). Safe to smoke render.
 */
@RunWith(RobolectricTestRunner::class)
class CompassQiblaViewTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun rendersSeekingStateWithQiblaInfoWithoutError() {
        composeRule.setThemedContent {
            com.arshadshah.nimaz.presentation.components.organisms.qibla.CompassQiblaView(
                qiblaBearing = 119f,
                animatedAzimuth = 30f,
                isFacingQibla = false,
                rotationToQibla = 12f,
                isCompassReady = true,
                accuracy = CompassAccuracy.HIGH,
                hasQiblaInfo = true,
                onCalibrate = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun rendersFacingStateWithoutError() {
        composeRule.setThemedContent {
            com.arshadshah.nimaz.presentation.components.organisms.qibla.CompassQiblaView(
                qiblaBearing = 0f,
                animatedAzimuth = 0f,
                isFacingQibla = true,
                rotationToQibla = 0f,
                isCompassReady = true,
                accuracy = CompassAccuracy.MEDIUM,
                hasQiblaInfo = true,
                onCalibrate = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun rendersWithoutQiblaInfoWithoutError() {
        composeRule.setThemedContent {
            com.arshadshah.nimaz.presentation.components.organisms.qibla.CompassQiblaView(
                qiblaBearing = 45f,
                animatedAzimuth = 10f,
                isFacingQibla = false,
                rotationToQibla = 35f,
                isCompassReady = false,
                accuracy = CompassAccuracy.LOW,
                hasQiblaInfo = false,
                onCalibrate = {}
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
