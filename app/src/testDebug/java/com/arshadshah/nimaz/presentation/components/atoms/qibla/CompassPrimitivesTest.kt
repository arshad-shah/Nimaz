package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CompassPrimitivesTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun `accent colour constants are distinct`() {
        assertThat(QiblaGold).isEqualTo(Color(0xFFEAB308))
        assertThat(QiblaGreen).isEqualTo(Color(0xFF22C55E))
        assertThat(CompassNorthColor).isEqualTo(Color(0xFFEF4444))
        assertThat(QiblaGold).isNotEqualTo(QiblaGreen)
    }

    @Test
    fun `compass rings render without crashing`() {
        composeRule.setContent {
            CompassRings(modifier = Modifier.size(280.dp))
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `compass dial face renders without crashing`() {
        composeRule.setContent {
            CompassDialFace(modifier = Modifier.size(280.dp))
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `compass needles render without crashing`() {
        composeRule.setContent {
            CompassNeedles(
                qiblaScreenAngle = 45f,
                northScreenAngle = -30f,
                isFacingQibla = false,
                modifier = Modifier.size(280.dp),
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `direction markers render without crashing`() {
        composeRule.setContent {
            DirectionMarkers(modifier = Modifier.size(280.dp))
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `center dot renders in both states`() {
        composeRule.setContent {
            CompassCenterDot(isFacingQibla = true)
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `lubber notch renders without crashing`() {
        composeRule.setContent {
            CompassLubberNotch(modifier = Modifier.size(280.dp))
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `facing glow renders when visible`() {
        composeRule.setContent {
            CompassFacingGlow(visible = true, modifier = Modifier.size(280.dp))
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
