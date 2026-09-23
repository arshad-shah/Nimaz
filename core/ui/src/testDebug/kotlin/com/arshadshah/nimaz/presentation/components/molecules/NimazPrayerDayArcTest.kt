package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.presentation.components.atoms.NimazSolarArcDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The day on the sun's path, shared by the Prayer Times card and the prayer settings preview.
 *
 * The arc paints its labels, so the drawing is asserted through the one sentence it speaks and
 * the room it takes: with times on the curve it needs a second label line above the day limb and
 * below the night troughs, and without them it must stay the compact card the Prayer Times screen
 * lays out above its list. Sunrise and sunset are the only times composed as text, under the arc.
 */
@RunWith(RobolectricTestRunner::class)
class NimazPrayerDayArcTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val day = listOf(
        NimazPrayerDayPoint(0.218f, "Fajr", "5:13 AM"),
        NimazPrayerDayPoint(0.300f, "Sunrise", "7:12 AM", isHorizon = true, tone = NimazTone.ACCENT),
        NimazPrayerDayPoint(0.553f, "Dhuhr", "1:17 PM", tone = NimazTone.PROMINENT),
        NimazPrayerDayPoint(0.689f, "Asr", "4:32 PM", tone = NimazTone.WARNING, highlighted = true),
        NimazPrayerDayPoint(0.806f, "Maghrib", "7:21 PM", isHorizon = true, tone = NimazTone.WARNING),
        NimazPrayerDayPoint(0.884f, "Isha", "9:13 PM"),
    )

    private fun arc(showTimes: Boolean) = composeRule.setThemedContent {
        NimazPrayerDayArc(
            points = day,
            sunriseFraction = 0.300f,
            sunsetFraction = 0.806f,
            contentDescription = "Today: Fajr 5:13 AM, Isha 9:13 PM",
            showTimes = showTimes,
        )
    }

    @Test
    fun `the arc speaks the day as one sentence`() {
        arc(showTimes = true)
        composeRule.onNodeWithContentDescription("Today: Fajr 5:13 AM, Isha 9:13 PM").assertIsDisplayed()
    }

    @Test
    fun `sunrise and sunset are stated under the arc`() {
        // The horizon crossings are bare dots on the curve; their times live in the row below.
        arc(showTimes = true)
        composeRule.onNodeWithText("7:12 AM").assertIsDisplayed()
        composeRule.onNodeWithText("7:21 PM").assertIsDisplayed()
    }

    @Test
    fun `with times on the curve the arc makes room for a second label line`() {
        arc(showTimes = true)
        composeRule.onNodeWithContentDescription("Today: Fajr 5:13 AM, Isha 9:13 PM")
            .assertHeightIsEqualTo(NimazSolarArcDefaults.HeightWithTimes)
    }

    @Test
    fun `without times it keeps the compact height the Prayer Times card was designed around`() {
        arc(showTimes = false)
        composeRule.onNodeWithContentDescription("Today: Fajr 5:13 AM, Isha 9:13 PM")
            .assertHeightIsEqualTo(NimazSolarArcDefaults.Height)
    }
}
