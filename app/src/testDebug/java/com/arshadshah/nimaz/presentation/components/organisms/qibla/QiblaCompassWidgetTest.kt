package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [com.arshadshah.nimaz.presentation.components.organisms.qibla.QiblaCompassWidget].
 * Pure Canvas-based compass primitives (no camera/sensors), so a smoke render is safe
 * under Robolectric. No text to assert, so we verify the tree composes without error.
 */
@RunWith(RobolectricTestRunner::class)
class QiblaCompassWidgetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun rendersSeekingStateWithoutError() {
        composeRule.setThemedContent {
            com.arshadshah.nimaz.presentation.components.organisms.qibla.QiblaCompassWidget(
                qiblaBearing = 119f,
                isFacingQibla = false,
                animatedAzimuth = 30f,
                compassSize = 300.dp
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun rendersFacingStateWithoutError() {
        composeRule.setThemedContent {
            com.arshadshah.nimaz.presentation.components.organisms.qibla.QiblaCompassWidget(
                qiblaBearing = 0f,
                isFacingQibla = true,
                animatedAzimuth = 0f,
                compassSize = 300.dp
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
