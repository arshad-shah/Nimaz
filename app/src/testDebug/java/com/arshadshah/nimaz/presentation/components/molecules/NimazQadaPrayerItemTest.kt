package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazQadaPrayerItemTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun record(
        name: PrayerName = PrayerName.FAJR,
        date: Long = 1_700_000_000_000L
    ) = PrayerRecord(
        id = 1L,
        date = date,
        prayerName = name,
        status = PrayerStatus.MISSED,
        prayedAt = null,
        scheduledTime = date,
        isJamaah = false,
        isQadaFor = null,
        note = null,
        createdAt = date,
        updatedAt = date
    )

    @Test
    fun `renders prayer display name and default action text`() {
        composeRule.setThemedContent {
            NimazQadaPrayerItem(
                prayer = record(PrayerName.DHUHR),
                onMarkCompleted = {}
            )
        }

        composeRule.onNodeWithText("Dhuhr").assertExists()
        composeRule.onNodeWithText("Done").assertExists()
    }

    @Test
    fun `uses custom action text`() {
        composeRule.setThemedContent {
            NimazQadaPrayerItem(
                prayer = record(PrayerName.ASR),
                onMarkCompleted = {},
                actionText = "Complete"
            )
        }

        composeRule.onNodeWithText("Complete").assertExists()
    }

    @Test
    fun `onMarkCompleted fires when action clicked`() {
        var marked = false
        composeRule.setThemedContent {
            NimazQadaPrayerItem(
                prayer = record(PrayerName.ISHA),
                onMarkCompleted = { marked = true },
                actionText = "Mark"
            )
        }

        composeRule.onNodeWithText("Mark").performClick()
        assertThat(marked).isTrue()
    }

    @Test
    fun `renders formatted date for valid timestamp`() {
        composeRule.setThemedContent {
            NimazQadaPrayerItem(
                prayer = record(date = 1_700_000_000_000L),
                onMarkCompleted = {}
            )
        }

        // 1_700_000_000_000 ms -> November 2023. Year is stable across time zones.
        composeRule.onNodeWithText("2023", substring = true).assertExists()
    }
}
