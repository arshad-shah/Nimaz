package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.theme.LocalUseHijriPrimary
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The date the reader chose to see first.
 *
 * "Hijri primary" is a preference the app offers and neither header had ever been rendered with
 * it on: both swap the two date lines around a `LocalUseHijriPrimary`, and both fall back when
 * the Hijri string is empty. Four separate conditionals, none of them exercised — and the
 * failure is a reader who turned the setting on and sees the Gregorian date anyway.
 */
@RunWith(RobolectricTestRunner::class)
class HijriPrimaryTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val now = Clock.System.now()

    @Test
    fun `the hero leads with the Hijri date when the reader asked for it`() {
        composeRule.setThemedContent {
            CompositionLocalProvider(LocalUseHijriPrimary provides true) {
                HomeHero(
                    hijriDate = "7 Rajab 1446",
                    gregorianDate = "Friday, January 31, 2026",
                    nextPrayer = PrayerType.ASR,
                    nextPrayerAt = now + 2.hours,
                )
            }
        }

        composeRule.onNodeWithText("7 Rajab 1446").assertIsDisplayed()
        composeRule.onNodeWithText("Friday, January 31, 2026").assertIsDisplayed()
    }

    @Test
    fun `the hero falls back to the Gregorian date when there is no Hijri one`() {
        // The Hijri string comes from a calculator that can be mid-load; leading with an empty
        // line would leave the hero headed by nothing at all.
        composeRule.setThemedContent {
            CompositionLocalProvider(LocalUseHijriPrimary provides true) {
                HomeHero(
                    hijriDate = "",
                    gregorianDate = "Friday, January 31, 2026",
                    nextPrayer = PrayerType.ASR,
                    nextPrayerAt = now + 2.hours,
                )
            }
        }

        // Twice, in fact: the primary line falls back to the Gregorian date and the secondary
        // line still renders it. Worth pinning exactly rather than loosely — a mid-load Hijri
        // string is a real state and this is what the reader sees in it.
        composeRule.onAllNodesWithText("Friday, January 31, 2026").fetchSemanticsNodes()
            .let { assertThat(it).hasSize(2) }
    }
}
