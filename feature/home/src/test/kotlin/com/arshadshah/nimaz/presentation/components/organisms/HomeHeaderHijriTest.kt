package com.arshadshah.nimaz.presentation.components.organisms

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
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

/** The tablet header's half of the same swap, plus the two placeholders it falls back to. */
@RunWith(RobolectricTestRunner::class)
class HomeHeaderHijriTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val now = Clock.System.now()

    @Test
    fun `the header leads with the Hijri date when the reader asked for it`() {
        composeRule.setThemedContent {
            CompositionLocalProvider(LocalUseHijriPrimary provides true) {
                HomeHeader(
                    locationName = "London",
                    hijriDate = "7 Rajab 1446",
                    gregorianDate = "Friday, January 31, 2026",
                    nextPrayer = PrayerType.ASR,
                    nextPrayerAt = now + 2.hours,
                    onSettingsClick = {},
                )
            }
        }

        composeRule.onNodeWithText("7 Rajab 1446").assertIsDisplayed()
    }

    @Test
    fun `an unset location says so rather than showing a blank pill`() {
        // The empty string is what a fresh install has, and a header with a gap where the
        // place should be reads as a rendering bug rather than as "you have not set one".
        composeRule.setThemedContent {
            HomeHeader(
                locationName = "",
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = null,
                nextPrayerAt = null,
                onSettingsClick = {},
            )
        }

        composeRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<Application>()
                .getString(R.string.location)
        ).assertIsDisplayed()
    }

    @Test
    fun `no next prayer shows a placeholder rather than the word null`() {
        composeRule.setThemedContent {
            HomeHeader(
                locationName = "London",
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = null,
                nextPrayerAt = null,
                onSettingsClick = {},
            )
        }

        composeRule.onNodeWithText("—").assertIsDisplayed()
    }
}
