package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The top bar at both ends of its morph, and past them.
 *
 * The bar draws a scrim below full progress and a hairline divider above zero, so the two
 * conditionals are on opposite sides of the transition and neither is reachable from a single
 * rendering. It also clamps its input — the caller computes progress from a scroll fraction, and
 * an out-of-range value must not draw a scrim at negative alpha.
 */
@RunWith(RobolectricTestRunner::class)
class HomeDynamicTopBarProgressTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val now = Clock.System.now()

    private fun render(progress: Float, location: String = "London") {
        composeRule.setThemedContent {
            HomeDynamicTopBar(
                transitionProgress = progress,
                locationName = location,
                nextPrayer = PrayerType.ASR,
                nextPrayerAt = now + 2.hours,
                onSettingsClick = {},
            )
        }
    }

    @Test
    fun `at rest it shows the location over the sky`() {
        render(progress = 0f)

        composeRule.onNodeWithText("London").assertIsDisplayed()
    }

    @Test
    fun `fully scrolled it shows the next prayer instead of the location`() {
        render(progress = 1f)

        composeRule.onNodeWithText(PrayerType.ASR.displayName).assertExists()
    }

    @Test
    fun `mid-transition both the scrim and the divider are drawn`() {
        // Below 1 the scrim is still there; above 0 the divider has begun. Half-way is the only
        // value at which both conditionals are true at once.
        render(progress = 0.5f)

        composeRule.onNodeWithText("London").assertExists()
    }

    @Test
    fun `a progress above one is clamped rather than drawn past the end`() {
        render(progress = 4f)

        composeRule.onNodeWithText(PrayerType.ASR.displayName).assertExists()
    }

    @Test
    fun `a negative progress is clamped rather than drawing a scrim at negative alpha`() {
        render(progress = -2f)

        composeRule.onNodeWithText("London").assertIsDisplayed()
    }
}
