package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.Khatam
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranKhatamProgressCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `active khatam renders name counts and percent`() {
        val khatam = Khatam(
            id = 7,
            name = "Ramadan Khatam",
            totalAyahsRead = 1500
        )
        composeRule.setThemedContent {
            KhatamProgressCard(
                activeKhatam = khatam,
                completedCount = 2,
                onClickActive = {},
                onClickStart = {}
            )
        }
        composeRule.onNodeWithText("Ramadan Khatam").assertExists()
        // R.string.quran_home_completed_count -> "%1$d completed"
        composeRule.onNodeWithText("2 completed").assertExists()
        // totalAyahsRead read count
        composeRule.onNodeWithText("1500").assertExists()
        composeRule.onNodeWithText("Ayahs Read").assertExists()
        // remaining = 6236 - 1500 = 4736
        composeRule.onNodeWithText("4736").assertExists()
        composeRule.onNodeWithText("Remaining").assertExists()
        // 1500/6236 = 0.2405 -> 24 percent. R.string -> "%1$d%% complete"
        composeRule.onNodeWithText("24% complete").assertExists()
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
                completedCount = 0,
                onClickActive = { clickedId = it },
                onClickStart = {}
            )
        }
        composeRule.onNodeWithText("My Khatam").performClick()
        assertThat(clickedId).isEqualTo(42L)
    }

    @Test
    fun `inactive state renders start prompt`() {
        composeRule.setThemedContent {
            KhatamProgressCard(
                activeKhatam = null,
                completedCount = 0,
                onClickActive = {},
                onClickStart = {}
            )
        }
        composeRule.onNodeWithText("Start a Khatam").assertExists()
        composeRule.onNodeWithText("Track your Quran reading progress").assertExists()
    }

    @Test
    fun `inactive state click invokes start callback`() {
        var started = false
        composeRule.setThemedContent {
            KhatamProgressCard(
                activeKhatam = null,
                completedCount = 0,
                onClickActive = {},
                onClickStart = { started = true }
            )
        }
        composeRule.onNodeWithText("Start a Khatam").performClick()
        assertThat(started).isTrue()
    }
}
