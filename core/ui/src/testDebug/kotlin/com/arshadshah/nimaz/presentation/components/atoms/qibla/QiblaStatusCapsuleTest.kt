package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QiblaStatusCapsuleTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders bearing with cardinal direction`() {
        composeRule.setThemedContent {
            QiblaStatusCapsule(
                qiblaBearing = 119,
                isFacingQibla = false,
                rotationToQibla = 0f,
                isCompassReady = false,
            )
        }
        // 119 falls in the SE bucket (112.5..157.5)
        composeRule.onNodeWithText("119° SE").assertExists()
    }

    @Test
    fun `renders facing state`() {
        composeRule.setThemedContent {
            QiblaStatusCapsule(
                qiblaBearing = 119,
                isFacingQibla = true,
                rotationToQibla = 0f,
                isCompassReady = true,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("119° SE").assertExists()
    }

    @Test
    fun `renders seeking state on camera`() {
        composeRule.setThemedContent {
            QiblaStatusCapsule(
                qiblaBearing = 45,
                isFacingQibla = false,
                rotationToQibla = -8f,
                isCompassReady = true,
                onCamera = true,
            )
        }
        composeRule.waitForIdle()
        // 45 falls in the NE bucket (22.5..67.5)
        composeRule.onNodeWithText("45° NE").assertExists()
    }
}
