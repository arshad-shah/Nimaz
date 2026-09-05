package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The scene's optional third glass-bar action.
 *
 * It exists because Prayer Times' "Today" shortcut was a [NimazBadge] positioned by hand at
 * `statusBarTop + 60.dp`, with a source comment about having to dodge these very actions. A slot
 * costs nothing here and removes the magic offset there.
 *
 * `cloudsEnabled = false` throughout: the drifting clouds are an infinite transition, and
 * `waitForIdle` never returns while one is running.
 */
@RunWith(RobolectricTestRunner::class)
class PrayerSkySceneActionTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a trailing action renders in the glass bar`() {
        composeRule.setThemedContent {
            PrayerSkyScene(
                timeOfDay = 0.5f,
                timeLabel = "14:32",
                statusLabel = "Asr in 2h 41m",
                locationName = "Dublin",
                onBack = {},
                onSettings = {},
                cloudsEnabled = false,
                trailingAction = { Text("Today") },
                modifier = Modifier.fillMaxWidth().height(260.dp),
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Today").assertIsDisplayed()
    }

    @Test
    fun `the trailing action is interactive`() {
        var taps = 0
        composeRule.setThemedContent {
            PrayerSkyScene(
                timeOfDay = 0.5f,
                timeLabel = "14:32",
                statusLabel = "Asr in 2h 41m",
                locationName = "Dublin",
                onBack = {},
                onSettings = {},
                cloudsEnabled = false,
                trailingAction = {
                    NimazBadge(text = "Today", onClick = { taps++ })
                },
                modifier = Modifier.fillMaxWidth().height(260.dp),
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Today").performClick()
        assertThat(taps).isEqualTo(1)
    }

    /** Every existing call site passes nothing, so the default must change nothing. */
    @Test
    fun `no trailing action is the default`() {
        composeRule.setThemedContent {
            PrayerSkyScene(
                timeOfDay = 0.5f,
                timeLabel = "14:32",
                statusLabel = "Asr in 2h 41m",
                locationName = "Dublin",
                onBack = {},
                onSettings = {},
                cloudsEnabled = false,
                modifier = Modifier.fillMaxWidth().height(260.dp),
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("14:32").assertIsDisplayed()
        composeRule.onNodeWithText("Dublin").assertIsDisplayed()
    }

    /**
     * The slot belongs to the top bar, so a scene without one — the Home hero, which passes no
     * location or navigation callbacks — must not start drawing a bar just because an action was
     * handed to it.
     */
    @Test
    fun `a scene with no top bar ignores the trailing action`() {
        composeRule.setThemedContent {
            PrayerSkyScene(
                timeOfDay = 0.5f,
                timeLabel = "14:32",
                statusLabel = "Asr in 2h 41m",
                cloudsEnabled = false,
                trailingAction = { Text("Today") },
                modifier = Modifier.fillMaxWidth().height(260.dp),
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("14:32").assertIsDisplayed()
        composeRule.onNodeWithText("Today").assertDoesNotExist()
    }
}
