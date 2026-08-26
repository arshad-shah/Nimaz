package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The prayer-times hero's own surface — the labels over the sky, rather than the sky itself.
 *
 * `PrayerSkySceneArtTest` draws the scene; what it does not assert is the text, because a bitmap
 * cannot tell you whether the time reads "13:04". Both pills are `GlassPill`s over the scene's own
 * backdrop, and they are the only two things on the hero a user actually reads.
 *
 * `showTopBar` is an all-three-or-nothing condition — a location *and* both actions — and it also
 * decides whether the status-bar band is reserved. Each partial combination is a real call from a
 * screen that has a location but no navigation, and none of them may draw a half-wired bar under
 * the system clock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class PrayerSkySceneOptionsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun hero(
        locationName: String? = null,
        onBack: (() -> Unit)? = null,
        onSettings: (() -> Unit)? = null,
        statusLabel: String,
    ) = @androidx.compose.runtime.Composable {
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            PrayerSkyScene(
                timeOfDay = 0.53f,
                timeLabel = "13:04",
                statusLabel = statusLabel,
                modifier = Modifier,
                moonFraction = 0.25f,
                shape = RoundedCornerShape(4.dp),
                cloudsEnabled = false,
                sunriseFraction = 0.24f,
                sunsetFraction = 0.84f,
                locationName = locationName,
                onBack = onBack,
                onSettings = onSettings,
            )
        }
    }

    @Test
    fun `the hero reads out the time and the status`() {
        composeRule.setThemedContent {
            hero(statusLabel = "Dhuhr in 2 hours")()
        }

        composeRule.onNodeWithText("13:04").assertExists()
        composeRule.onNodeWithText("Dhuhr in 2 hours").assertExists()
    }

    @Test
    fun `the top bar appears only with a location and both actions`() {
        composeRule.setThemedContent {
            hero(
                locationName = "Abbeyleix",
                onBack = {},
                onSettings = {},
                statusLabel = "Dhuhr in 2 hours",
            )()
        }

        composeRule.onNodeWithText("Abbeyleix").assertExists()
    }

    @Test
    fun `a location with no navigation draws no bar`() {
        // The partial combinations. A bar wired to one of the three would render half a control
        // under the system clock, on a screen that deliberately has no navigation.
        composeRule.setThemedContent {
            hero(locationName = "Abbeyleix", statusLabel = "no nav")()
        }

        composeRule.onNodeWithText("Abbeyleix").assertDoesNotExist()
        composeRule.onNodeWithText("no nav").assertExists()
    }

    @Test
    fun `navigation with no location draws no bar either`() {
        composeRule.setThemedContent {
            hero(onBack = {}, onSettings = {}, statusLabel = "no location")()
        }

        composeRule.onNodeWithText("no location").assertExists()
    }
}
