package com.arshadshah.nimaz.presentation.components.molecules

import com.arshadshah.nimaz.core.ui.R

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.model.KhatamPace
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The card now delegates to the shared [KhatamHeroCard], so these assert the shared
 * rendering rather than the bespoke layout this component used to own.
 */
@RunWith(RobolectricTestRunner::class)
class QuranKhatamProgressCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val insights = KhatamInsights(
        daysActive = 10,
        averagePace = 150f,
        currentStreak = 4,
        juzCompleted = 7,
        // The juz actually being read — not juzCompleted + 1, which only coincides
        // when the reader has gone strictly in order.
        currentJuz = 8,
        remainingAyahs = 4736,
        estimatedDaysRemaining = 32,
        paceStatus = KhatamPace.ON_TRACK,
    )

    @Test
    fun `active khatam renders name progress and percent`() {
        val khatam = Khatam(
            id = 7,
            name = "Ramadan Khatam",
            totalAyahsRead = 1500
        )
        composeRule.setThemedContent {
            KhatamProgressCard(
                activeKhatam = khatam,
                insights = insights,
                completedCount = 2,
                onClickActive = {},
                onClickStart = {}
            )
        }
        composeRule.onNodeWithText("Ramadan Khatam").assertExists()
        // R.string.khatam_of_ayahs_read -> "%1$d of %2$d ayahs read"
        composeRule.onNodeWithText("1500 of 6236 ayahs read").assertExists()
        // The ring clears its child semantics and announces a spoken percentage
        // instead, so assert the accessible description rather than the raw "24%".
        composeRule.onNodeWithContentDescription("24 percent complete").assertExists()
        // The pace verdict is part of the shared hero.
        composeRule.onNodeWithText("On track").assertExists()
    }

    @Test
    fun `active khatam shows the next juz and remaining days`() {
        val khatam = Khatam(id = 3, name = "My Khatam", totalAyahsRead = 1500)
        composeRule.setThemedContent {
            KhatamProgressCard(
                activeKhatam = khatam,
                insights = insights,
                completedCount = 0,
                onClickActive = {},
                onClickStart = {}
            )
        }
        composeRule.onNodeWithText("Juz 8 · ~32 days left").assertExists()
    }

    @Test
    fun `active khatam click passes khatam id`() {
        val khatam = Khatam(
            id = 42,
            name = "My Khatam",
            totalAyahsRead = 100
        )
        var clickedId: Long? = null
        composeRule.setThemedContent {
            KhatamProgressCard(
                activeKhatam = khatam,
                insights = insights,
                completedCount = 0,
                onClickActive = { clickedId = it },
                onClickStart = {}
            )
        }
        composeRule.onNodeWithText("My Khatam").performClick()
        assertThat(clickedId).isEqualTo(42L)
    }

    @Test
    fun `null insights still render without crashing`() {
        // Home may emit the khatam a frame before its insights arrive.
        val khatam = Khatam(id = 1, name = "Pending", totalAyahsRead = 0)
        composeRule.setThemedContent {
            KhatamProgressCard(
                activeKhatam = khatam,
                insights = null,
                completedCount = 0,
                onClickActive = {},
                onClickStart = {}
            )
        }
        composeRule.onNodeWithText("Pending").assertExists()
        composeRule.onNodeWithContentDescription("0 percent complete").assertExists()
    }

    @Test
    fun `inactive state renders start prompt`() {
        composeRule.setThemedContent {
            KhatamProgressCard(
                activeKhatam = null,
                insights = null,
                completedCount = 0,
                onClickActive = {},
                onClickStart = {}
            )
        }
        composeRule.onNodeWithText("No Khatam Started").assertExists()
        composeRule.onNodeWithText("Start your journey to complete the Quran").assertExists()
    }

    @Test
    fun `inactive state mentions lifetime completions when there are any`() {
        composeRule.setThemedContent {
            KhatamProgressCard(
                activeKhatam = null,
                insights = null,
                completedCount = 3,
                onClickActive = {},
                onClickStart = {}
            )
        }
        composeRule.onNodeWithText("3 Khatams completed").assertExists()
    }

    @Test
    fun `inactive state click invokes start callback`() {
        var started = false
        composeRule.setThemedContent {
            KhatamProgressCard(
                activeKhatam = null,
                insights = null,
                completedCount = 0,
                onClickActive = {},
                onClickStart = { started = true }
            )
        }
        composeRule.onNodeWithText("Start New Khatam").performClick()
        assertThat(started).isTrue()
    }
}
