package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The timeline's three node states.
 *
 * A node is drawn one of three ways — done, happening now, still to come — and the middle one
 * is the only glanceable answer to "where am I in the day". It had never rendered: every
 * existing test builds a list where nothing is `isCurrent`, so the `when` fell to its first and
 * last arms only.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1400dp")
class TodaysProgressCardStatesTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val now = Clock.System.now()

    private fun prayers(current: PrayerType?, prayed: Set<PrayerType>) =
        PrayerType.entries.mapIndexed { index, type ->
            PrayerTimeDisplay(
                type = type,
                name = type.displayName,
                timeAt = now + (index - 2).hours,
                isPassed = type in prayed || type == current,
                isCurrent = type == current,
                isNext = false,
                prayerStatus = if (type in prayed) PrayerStatus.PRAYED else PrayerStatus.NOT_PRAYED,
            )
        }

    @Test
    fun `a prayer happening now is drawn differently from one done and one still to come`() {
        composeRule.setThemedContent {
            TodaysProgressCard(
                prayerTimes = prayers(
                    current = PrayerType.DHUHR,
                    prayed = setOf(PrayerType.FAJR),
                )
            )
        }

        // All three arms are on screen at once: Fajr done, Dhuhr current, Isha to come.
        composeRule.onNodeWithText(PrayerType.FAJR.displayName.uppercase()).assertIsDisplayed()
        composeRule.onNodeWithText(PrayerType.DHUHR.displayName.uppercase()).assertIsDisplayed()
        composeRule.onNodeWithText(PrayerType.ISHA.displayName.uppercase()).assertIsDisplayed()
    }

    @Test
    fun `a day with nothing current still draws every node`() {
        composeRule.setThemedContent {
            TodaysProgressCard(prayerTimes = prayers(current = null, prayed = emptySet()))
        }

        composeRule.onNodeWithText(PrayerType.FAJR.displayName.uppercase()).assertIsDisplayed()
    }

    @Test
    fun `a completed day draws every node as done`() {
        composeRule.setThemedContent {
            TodaysProgressCard(
                prayerTimes = prayers(
                    current = null,
                    prayed = PrayerType.entries.toSet(),
                )
            )
        }

        composeRule.onNodeWithText(PrayerType.ISHA.displayName.uppercase()).assertIsDisplayed()
    }
}
