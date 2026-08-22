package com.arshadshah.nimaz.presentation.screens.worship

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.worship.NightWorshipEvent
import com.arshadshah.nimaz.presentation.viewmodel.worship.NightWorshipUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * The night worship hub's UI.
 *
 * The interesting behaviour is entirely about **when you open it**. The window has three states,
 * and the wrong one shown is a real failure — a hub reached from a card that just said "it is time"
 * must not read "tonight's window has passed", and one opened at 3pm must not claim to be open. All
 * three are exercised here as data rather than by waiting for the right hour, which is the whole
 * reason the content composable is split from its ViewModel.
 */
@RunWith(RobolectricTestRunner::class)
class NightWorshipContentTest {

    @get:Rule
    @Suppress("DEPRECATION")
    val composeRule = createComposeRule()

    private val context: android.content.Context = ApplicationProvider.getApplicationContext()
    private fun str(id: Int) = context.getString(id)

    private val now: Instant get() = Clock.System.now()

    private fun render(
        state: NightWorshipUiState,
        onEvent: (NightWorshipEvent) -> Unit = {},
        onOpenSurah: (Int) -> Unit = {},
        onOpenDuaCategory: (String) -> Unit = {},
        onOpenHadith: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                NightWorshipContent(
                    state = state,
                    onEvent = onEvent,
                    onNavigateBack = {},
                    onOpenSurah = onOpenSurah,
                    onOpenDuaCategory = onOpenDuaCategory,
                    onOpenHadith = onOpenHadith,
                )
            }
        }
    }

    @Test
    fun `before the window opens it counts down rather than claiming to be open`() {
        render(
            NightWorshipUiState(
                isLoading = false,
                lastThirdAt = now + 4.hours,
                fajrAt = now + 6.hours,
            )
        )

        composeRule.onNodeWithText(str(R.string.night_worship_opens_in), substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `inside the window it reads as open`() {
        render(
            NightWorshipUiState(
                isLoading = false,
                lastThirdAt = now - 1.hours,
                fajrAt = now + 2.hours,
            )
        )

        composeRule.onNodeWithText(str(R.string.night_worship_open_now), substring = true)
            .assertIsDisplayed()
    }

    /**
     * Opened in the afternoon, after Fajr has passed. It must say so plainly instead of rendering a
     * stale or negative countdown — the hub is reachable from anywhere at any hour.
     */
    @Test
    fun `after fajr it says the window has passed`() {
        render(
            NightWorshipUiState(
                isLoading = false,
                lastThirdAt = now - 12.hours,
                fajrAt = now - 9.hours,
            )
        )

        composeRule.onNodeWithText(str(R.string.night_worship_closed), substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `without a location it asks for one instead of showing an empty window`() {
        render(NightWorshipUiState(isLoading = false, lastThirdAt = null, fajrAt = null))

        composeRule.onNodeWithText(str(R.string.night_worship_times_unavailable), substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `tapping add rakah emits the pair event`() {
        val events = mutableListOf<NightWorshipEvent>()
        render(
            state = NightWorshipUiState(isLoading = false, lastThirdAt = now, fajrAt = now + 3.hours),
            onEvent = { events += it },
        )

        composeRule.onNodeWithTag(NightWorshipAddRakahTestTag).performClick()

        assertEquals(listOf(NightWorshipEvent.AddRakahPair), events)
    }

    @Test
    fun `the counter shows the current tally`() {
        render(
            NightWorshipUiState(
                isLoading = false,
                lastThirdAt = now,
                fajrAt = now + 3.hours,
                rakahCount = 8,
            )
        )

        composeRule.onNodeWithTag(NightWorshipCountTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("8").assertIsDisplayed()
    }

    @Test
    fun `the recitation row opens Al-Mulk`() {
        var openedSurah: Int? = null
        render(
            state = NightWorshipUiState(isLoading = false, lastThirdAt = now, fajrAt = now + 3.hours),
            onOpenSurah = { openedSurah = it },
        )

        composeRule.onNodeWithTag(NightWorshipReciteRowTestTag).performScrollTo().performClick()

        assertEquals("Al-Mulk is surah 67", 67, openedSurah)
    }

    @Test
    fun `the duas row opens the witr and night prayer category`() {
        var openedCategory: String? = null
        render(
            state = NightWorshipUiState(isLoading = false, lastThirdAt = now, fajrAt = now + 3.hours),
            onOpenDuaCategory = { openedCategory = it },
        )

        composeRule.onNodeWithTag(NightWorshipDuasRowTestTag).performScrollTo().performClick()

        assertEquals("Witr & Night Prayer is seeded category 35", "35", openedCategory)
    }

    @Test
    fun `the why-pray-at-night row opens the narration it rests on`() {
        var openedHadith: String? = null
        render(
            state = NightWorshipUiState(isLoading = false, lastThirdAt = now, fajrAt = now + 3.hours),
            onOpenHadith = { openedHadith = it },
        )

        composeRule.onNodeWithTag(NightWorshipWhyRowTestTag).performScrollTo().performClick()

        assertEquals("Bukhari 1145 is row 1149 in the seeded corpus", "1149", openedHadith)
    }
}
