package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimeDisplay
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerTimeCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun display(
        type: PrayerType = PrayerType.ASR,
        name: String = "Asr",
        time: String = "4:30 PM",
        isPassed: Boolean = false,
        isCurrent: Boolean = false,
        isNext: Boolean = false,
        status: PrayerStatus = PrayerStatus.NOT_PRAYED
    ) = PrayerTimeDisplay(
        type = type,
        name = name,
        time = time,
        isPassed = isPassed,
        isCurrent = isCurrent,
        isNext = isNext,
        prayerStatus = status
    )

    @Test
    fun `renders prayer name and time`() {
        composeRule.setThemedContent {
            PrayerTimeCard(
                prayer = display(name = "Asr", time = "4:30 PM"),
                isActive = false,
                onClick = {},
                onToggle = {}
            )
        }

        composeRule.onNodeWithText("Asr").assertExists()
        composeRule.onNodeWithText("4:30 PM").assertExists()
    }

    @Test
    fun `active prayer renders its name and time`() {
        composeRule.setThemedContent {
            PrayerTimeCard(
                prayer = display(isCurrent = true, isNext = true),
                isActive = true,
                onClick = {},
                onToggle = {}
            )
        }

        // The active card is conveyed by its container/accent (not a textual
        // pill); its name and time still render.
        composeRule.onNodeWithText("Asr").assertExists()
        composeRule.onNodeWithText("4:30 PM").assertExists()
    }

    @Test
    fun `card shows no textual NEXT pill`() {
        composeRule.setThemedContent {
            PrayerTimeCard(
                prayer = display(isCurrent = true, isNext = true),
                isActive = true,
                onClick = {},
                onToggle = {}
            )
        }

        composeRule.onNodeWithText("NEXT").assertDoesNotExist()
    }

    @Test
    fun `prayed prayer shows the prayed check`() {
        composeRule.setThemedContent {
            PrayerTimeCard(
                prayer = display(status = PrayerStatus.PRAYED),
                isActive = false,
                onClick = {},
                onToggle = {}
            )
        }

        // "Prayed" is the contentDescription of the check icon (R.string.prayed)
        composeRule.onNodeWithContentDescription("Prayed").assertExists()
    }

    @Test
    fun `not-prayed prayer has no prayed check icon`() {
        composeRule.setThemedContent {
            PrayerTimeCard(
                prayer = display(status = PrayerStatus.NOT_PRAYED),
                isActive = false,
                onClick = {},
                onToggle = {}
            )
        }

        // The checkbox node is always present for accessibility; when not prayed it is unchecked.
        composeRule.onNodeWithContentDescription("Prayed").assertIsOff()
    }

    @Test
    fun `passed inactive prayer renders (faded path)`() {
        composeRule.setThemedContent {
            PrayerTimeCard(
                prayer = display(name = "Fajr", isPassed = true),
                isActive = false,
                onClick = {},
                onToggle = {}
            )
        }

        composeRule.onNodeWithText("Fajr").assertExists()
    }

    @Test
    fun `onClick fires when card tapped`() {
        var clicked = false
        composeRule.setThemedContent {
            PrayerTimeCard(
                prayer = display(name = "Dhuhr"),
                isActive = false,
                onClick = { clicked = true },
                onToggle = {}
            )
        }

        composeRule.onNodeWithText("Dhuhr").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `onToggle fires when prayed toggle tapped`() {
        var toggled = false
        composeRule.setThemedContent {
            PrayerTimeCard(
                prayer = display(status = PrayerStatus.PRAYED),
                isActive = false,
                onClick = {},
                onToggle = { toggled = true }
            )
        }

        composeRule.onNodeWithContentDescription("Prayed").performClick()
        assertThat(toggled).isTrue()
    }

    @Test
    fun `sunrise renders without a toggle and without prayed check`() {
        composeRule.setThemedContent {
            PrayerTimeCard(
                prayer = display(
                    type = PrayerType.SUNRISE,
                    name = "Sunrise",
                    time = "6:45 AM"
                ),
                isActive = false,
                onClick = {},
                onToggle = {}
            )
        }

        composeRule.onNodeWithText("Sunrise").assertExists()
        composeRule.onNodeWithContentDescription("Prayed").assertDoesNotExist()
    }
}
