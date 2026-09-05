package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazPrayerRowTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `the row names the prayer and states its time`() {
        composeRule.setThemedContent {
            NimazPrayerRow(type = PrayerType.DHUHR, name = "Dhuhr", time = "13:22")
        }
        composeRule.onNodeWithText("Dhuhr").assertIsDisplayed()
        composeRule.onNodeWithText("13:22").assertIsDisplayed()
    }

    /**
     * The whole reason this molecule exists rather than a restyled [PrayerTimeCard].
     *
     * Prayer Times is a reference screen: it answers *when*, and the prayer tracker answers what
     * the reader did about it. A row that looked tappable would promise logging this screen no
     * longer performs — and the logging it used to perform wrote a binary PRAYED/NOT_PRAYED that
     * silently downgraded a prayer recorded as LATE on the tracker.
     */
    @Test
    fun `the row is never clickable`() {
        composeRule.setThemedContent {
            NimazPrayerRow(type = PrayerType.ASR, name = "Asr", time = "17:13")
        }
        composeRule.onNodeWithText("Asr").assertHasNoClickAction()
    }

    @Test
    fun `the next prayer is not clickable either`() {
        composeRule.setThemedContent {
            NimazPrayerRow(type = PrayerType.ASR, name = "Asr", time = "17:13", isNext = true)
        }
        composeRule.onNodeWithText("Asr").assertHasNoClickAction()
        composeRule.onNodeWithText("17:13").assertIsDisplayed()
    }

    @Test
    fun `a qualifier renders beside the name`() {
        composeRule.setThemedContent {
            NimazPrayerRow(
                type = PrayerType.DHUHR,
                name = "Dhuhr",
                time = "13:20",
                qualifier = "Jumu'ah",
            )
        }
        composeRule.onNodeWithText("Jumu'ah").assertIsDisplayed()
    }

    @Test
    fun `a passed prayer still states its name and time`() {
        composeRule.setThemedContent {
            NimazPrayerRow(type = PrayerType.FAJR, name = "Fajr", time = "05:12", isPassed = true)
        }
        composeRule.onNodeWithText("Fajr").assertIsDisplayed()
        composeRule.onNodeWithText("05:12").assertIsDisplayed()
    }

    /** Sunrise is not a salat, so it carries no Arabic name beside it. */
    @Test
    fun `arabic can be suppressed`() {
        composeRule.setThemedContent {
            NimazPrayerRow(
                type = PrayerType.SUNRISE,
                name = "Sunrise",
                time = "06:48",
                showArabic = false,
            )
        }
        composeRule.onNodeWithText("Sunrise").assertIsDisplayed()
        composeRule.onNodeWithText("06:48").assertIsDisplayed()
    }

    @Test
    fun `arabic is shown by default`() {
        composeRule.setThemedContent {
            NimazPrayerRow(type = PrayerType.MAGHRIB, name = "Maghrib", time = "20:04")
        }
        // getArabicPrayerName resolves Maghrib to its Arabic form; the row renders it under
        // the English name.
        composeRule.onNodeWithText("المغرب").assertIsDisplayed()
    }

    @Test
    fun `a missing time renders its placeholder rather than an empty row`() {
        composeRule.setThemedContent {
            NimazPrayerRow(type = PrayerType.ISHA, name = "Isha", time = "--:--")
        }
        composeRule.onNodeWithText("Isha").assertIsDisplayed()
        composeRule.onNodeWithText("--:--").assertIsDisplayed()
    }
}
