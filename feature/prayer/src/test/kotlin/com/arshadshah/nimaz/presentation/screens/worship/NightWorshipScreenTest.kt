package com.arshadshah.nimaz.presentation.screens.worship

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorKind
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.worship.NightWorshipEvent
import com.arshadshah.nimaz.presentation.viewmodel.worship.NightWorshipUiState
import com.arshadshah.nimaz.presentation.viewmodel.worship.NightWorshipViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * The two states of the night-worship window that carry no times: still loading, and failed.
 *
 * `NightWorshipContentTest` covers the three *window* states — before, open, closed — which is
 * where the time-of-day logic lives. What it does not cover is the card before any of that:
 *
 * - **Loading is guarded on `lastThirdAt == null`, not on `isLoading` alone.** A refresh that
 *   flips `isLoading` while times are already on screen must not blank the card back to a
 *   spinner; someone watching the countdown would see it disappear for no reason.
 * - **The failure is INLINE, inside the card.** The hub's other cards — the rakah counter, the
 *   surah and dua shortcuts — are still correct when the astronomy fails, and a full-screen
 *   error would take them down with it. The retry is the only way back, so it has to dispatch.
 *
 * It also runs the `NightWorshipScreen` wrapper against a mocked ViewModel, which is the seam
 * between the two: the screen's only job is to collect state and forward `onEvent`, and a
 * wrapper that forwarded neither would still compile and still render an empty hub.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp-mdpi")
class NightWorshipScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(NightWorshipUiState())
    private val events = mutableListOf<NightWorshipEvent>()

    private val viewModel: NightWorshipViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@NightWorshipScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<NightWorshipEvent>() }
    }

    private var backs = 0

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun render() {
        composeRule.setThemedContent {
            NightWorshipScreen(
                onNavigateBack = { backs++ },
                onOpenSurah = {},
                onOpenDuaCategory = {},
                onOpenHadith = {},
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `with no times yet the window card shows a spinner rather than an empty box`() {
        // The spinner is indeterminate, so the frame clock never idles — see #604.
        composeRule.mainClock.autoAdvance = false
        state.value = NightWorshipUiState(isLoading = true)
        render()
        composeRule.mainClock.advanceTimeBy(200)

        composeRule.onNodeWithTag(NightWorshipWindowTestTag).assertExists()
        composeRule.onNodeWithText(str(R.string.night_worship_times_failed)).assertDoesNotExist()
    }

    @Test
    fun `a refresh over times already on screen does not blank the card`() {
        val now = Clock.System.now()
        state.value = NightWorshipUiState(
            isLoading = true,
            lastThirdAt = now + 2.hours,
            fajrAt = now + 5.hours,
        )
        render()
        composeRule.waitForIdle()

        // `isLoading && lastThirdAt == null` — both halves. Guarding on `isLoading` alone
        // replaces a live countdown with a spinner every time the day rolls over.
        composeRule.onNodeWithText(str(R.string.night_worship_last_third)).assertIsDisplayed()
    }

    @Test
    fun `a failed calculation is reported in the card, with a retry that dispatches`() {
        state.value = NightWorshipUiState(
            isLoading = false,
            error = UiError(
                message = R.string.night_worship_times_failed,
                kind = NimazErrorKind.LOCATION,
                details = "no location",
            ),
        )
        render()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(str(R.string.night_worship_times_failed)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.try_again)).performClick()

        assertThat(events).contains(NightWorshipEvent.Refresh)
    }

    @Test
    fun `the wrapper forwards the counter events its content raises`() {
        val now = Clock.System.now()
        state.value = NightWorshipUiState(
            isLoading = false,
            lastThirdAt = now - 1.hours,
            fajrAt = now + 2.hours,
        )
        render()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NightWorshipAddRakahTestTag).performScrollTo().performClick()

        // `viewModel::onEvent` is passed as a reference; a wrapper that swallowed it would show
        // a counter that never moves.
        assertThat(events).contains(NightWorshipEvent.AddRakahPair)
    }

    @Test
    fun `back navigates back`() {
        state.value = NightWorshipUiState(isLoading = false)
        render()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(str(R.string.cd_back)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
