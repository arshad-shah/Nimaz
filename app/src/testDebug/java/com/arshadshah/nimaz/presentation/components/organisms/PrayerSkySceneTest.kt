package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerSkySceneTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders time and status labels`() {
        composeRule.setThemedContent {
            PrayerSkyScene(
                timeOfDay = 0.5f,
                timeLabel = "1:15 PM",
                statusLabel = "Dhuhr soon",
                moonFraction = 0.5f,
                cloudsEnabled = false,
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("1:15 PM").assertExists()
        composeRule.onNodeWithText("Dhuhr soon").assertExists()
    }

    @Test
    fun `SkyBackground smoke renders without crashing`() {
        composeRule.setThemedContent {
            SkyBackground(
                timeOfDay = 0.92f,
                moonFraction = 0.62f,
                cloudsEnabled = false,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `MoonPhase fraction is within 0 to 1`() {
        val f = MoonPhase.fractionForEpochMillis(1_700_000_000_000L)
        assertThat(f).isAtLeast(0f)
        assertThat(f).isAtMost(1f)
    }

    @Test
    fun `MoonPhase full moon is fully illuminated`() {
        assertThat(MoonPhase.illumination(0.5f)).isWithin(0.01f).of(1f)
    }

    @Test
    fun `MoonPhase new moon is dark`() {
        assertThat(MoonPhase.illumination(0f)).isWithin(0.01f).of(0f)
    }
}
