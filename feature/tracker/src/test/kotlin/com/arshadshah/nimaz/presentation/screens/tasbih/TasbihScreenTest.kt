package com.arshadshah.nimaz.presentation.screens.tasbih

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihCounterStyle
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihCounterUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihStatsUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihViewModel
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

/**
 * The counter itself — 463 lines and, until now, not one of them ever run on the JVM.
 *
 * `TasbihCounterTest` in `:app/src/androidTest` taps the circle on a device; nothing asserted
 * what the *screen* does with the state it is handed. The failures that leaves open are ones a
 * user notices immediately and cannot work around: a tap that raises no `Increment` (the count
 * is written to Room, so a lost tap is a permanently wrong total), a mode toggle that reports
 * the wrong style, and a reset control wired to the wrong event. Every one of them looks
 * identical on screen.
 *
 * The two counter modes are both pinned because they are different code paths behind one
 * `Crossfade`: classic renders a semantics-addressable circle, beads render a `Canvas` with no
 * text in it at all, and only the classic one has ever been exercised anywhere.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class TasbihScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val counterState = MutableStateFlow(TasbihCounterUiState(isActive = true))
    private val statsState = MutableStateFlow(TasbihStatsUiState(isLoading = false))
    private val events = mutableListOf<TasbihEvent>()
    private val navigations = mutableListOf<String>()

    private val viewModel: TasbihViewModel = mockk(relaxed = true) {
        every { this@mockk.counterState } returns this@TasbihScreenTest.counterState
        every { this@mockk.statsState } returns this@TasbihScreenTest.statsState
        every { onEvent(any()) } answers { events += firstArg<TasbihEvent>() }
    }

    private val preset = TasbihPreset(
        id = 7L,
        name = "SubhanAllah",
        arabicText = "سُبْحَانَ ٱللَّٰهِ",
        transliteration = "SubhanAllah",
        translation = "Glory be to Allah",
        targetCount = 33,
        category = TasbihCategory.AFTER_PRAYER,
        reference = "Sahih Muslim",
        isDefault = true,
        displayOrder = 0,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun setContent() {
        composeRule.setThemedContent {
            TasbihScreen(
                onNavigateToHistory = { navigations += "history" },
                onNavigateToChooseDhikr = { navigations += "choose" },
                onNavigateToAddPreset = { navigations += "add" },
                onNavigateToSettings = { navigations += "settings" },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the classic circle shows the running count and a tap on it increments`() {
        counterState.value = counterState.value.copy(count = 12, targetCount = 33)
        setContent()

        composeRule.onNodeWithTag(ScreenTags.TasbihCount, useUnmergedTree = true)
            .assertTextEquals("12")
        composeRule.onNodeWithTag(ScreenTags.TasbihCounter).performClick()

        // The single most consequential assertion in this file: the count is persisted, so a
        // tap that reaches no event is a permanently wrong total with no way for the user to
        // notice it happened.
        assertThat(events).containsExactly(TasbihEvent.Increment)
    }

    @Test
    fun `the circle invites a tap until a lap is done, then reports the laps`() {
        setContent()
        composeRule.onNodeWithText(string(R.string.tap_to_count)).assertExists()

        counterState.value = counterState.value.copy(count = 4, laps = 2)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.tap_to_count)).assertDoesNotExist()
        composeRule.onAllNodesWithText(
            context.resources.getQuantityString(R.plurals.laps_format, 2, 2)
        ).onFirst().assertExists()
    }

    @Test
    fun `the capsule reads count over target`() {
        counterState.value = counterState.value.copy(count = 9, targetCount = 100)
        setContent()

        composeRule.onNodeWithText("9 / 100").assertIsDisplayed()
    }

    @Test
    fun `choosing beads reports BEADS and choosing classic reports CLASSIC`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.tasbih_mode_beads)).performClick()
        assertThat(events).containsExactly(
            TasbihEvent.SetCounterStyle(TasbihCounterStyle.BEADS)
        )

        events.clear()
        composeRule.onNodeWithText(string(R.string.tasbih_mode_classic)).performClick()
        assertThat(events).containsExactly(
            TasbihEvent.SetCounterStyle(TasbihCounterStyle.CLASSIC)
        )
    }

    @Test
    fun `the design button belongs to beads mode alone`() {
        setContent()
        // Classic: the palette button would open a picker for a strand that is not on screen.
        composeRule.onNodeWithContentDescription(string(R.string.tasbih_bead_design))
            .assertDoesNotExist()

        counterState.value = counterState.value.copy(counterStyle = TasbihCounterStyle.BEADS)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(string(R.string.tasbih_bead_design))
            .assertExists()
    }

    @Test
    fun `the design button opens the picker and a choice is reported`() {
        counterState.value = counterState.value.copy(counterStyle = TasbihCounterStyle.BEADS)
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.tasbih_bead_design))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.tasbih_bead_jade)).performClick()

        assertThat(events).contains(TasbihEvent.SetBeadDesign("jade"))
    }

    @Test
    fun `handedness is set from the design sheet`() {
        counterState.value = counterState.value.copy(counterStyle = TasbihCounterStyle.BEADS)
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.tasbih_bead_design))
            .performClick()
        composeRule.waitForIdle()
        // The row's label is not the control — the switch beside it is, and it is the only
        // toggleable node in the sheet.
        composeRule.onNode(isToggleable()).performClick()

        // The strand advances the other way for a left-handed user; nothing else on the screen
        // says which way it currently goes.
        assertThat(events).contains(TasbihEvent.SetLeftHanded(true))
    }

    @Test
    fun `the three controls raise reset, sound and vibration`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.reset_action)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.toggle_sound)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.toggle_vibration)).performClick()

        // Each toggle sends the *opposite* of what is currently set — a control that echoed the
        // current value back would look live and do nothing.
        assertThat(events).containsExactly(
            TasbihEvent.Reset,
            TasbihEvent.ToggleSound(true),
            TasbihEvent.ToggleVibration(false),
        ).inOrder()
    }

    @Test
    fun `the peek card names the selected dhikr and its target`() {
        counterState.value = counterState.value.copy(selectedPreset = preset, targetCount = 33)
        setContent()

        composeRule.onNodeWithText(preset.name).assertExists()
        composeRule.onNodeWithText(
            "${preset.translation} · ${string(R.string.target_format, 33)}"
        ).assertExists()
    }

    @Test
    fun `with no dhikr chosen the peek card reads as a free count`() {
        counterState.value = counterState.value.copy(selectedPreset = null, targetCount = 100)
        setContent()

        composeRule.onNodeWithText(string(R.string.free_count_label)).assertExists()
        composeRule.onNodeWithText(string(R.string.target_format, 100)).assertExists()
    }

    @Test
    fun `a dhikr saved without Arabic or a translation still names itself`() {
        val bare = preset.copy(arabicText = null, translation = null, transliteration = null)
        counterState.value = counterState.value.copy(selectedPreset = bare, targetCount = 7)
        setContent()

        // A custom dhikr typed with only a name is the common case, and every optional line here
        // is a separate arm. An empty Arabic line renders as a gap in the peek card, which reads
        // as a rendering fault rather than as an absent field.
        composeRule.onNodeWithText(bare.name).assertExists()
        composeRule.onNodeWithText(string(R.string.target_format, 7)).assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the tablet info card also copes with a dhikr that has only a name`() {
        val bare = preset.copy(arabicText = null, translation = null, transliteration = null)
        counterState.value = counterState.value.copy(selectedPreset = bare, targetCount = 7)
        setContent()

        composeRule.onNodeWithText(bare.name).assertExists()
        composeRule.onNodeWithText(string(R.string.target_format, 7)).assertExists()
    }

    @Test
    fun `the peek card opens the current sheet, which offers a way to change dhikr`() {
        counterState.value = counterState.value.copy(selectedPreset = preset)
        setContent()

        composeRule.onNodeWithText(preset.name).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.tasbih_change_dhikr)).assertExists()
    }

    @Test
    fun `changing dhikr from the sheet closes it and navigates`() {
        counterState.value = counterState.value.copy(selectedPreset = preset)
        setContent()

        composeRule.onNodeWithText(preset.name).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.tasbih_change_dhikr)).performClick()
        composeRule.waitForIdle()

        assertThat(navigations).containsExactly("choose")
    }

    @Test
    fun `the sheet's today tile counts the session in progress, not only what is saved`() {
        // 40 already banked, plus a session standing at 2 laps of 33 and 5 more. Reading
        // `baseTotalToday` alone would under-report by the whole live session — the number
        // someone opens this sheet to see.
        statsState.value = statsState.value.copy(baseTotalToday = 40)
        counterState.value = counterState.value.copy(
            selectedPreset = preset,
            count = 5,
            laps = 2,
            targetCount = 33,
        )
        setContent()

        composeRule.onNodeWithText(preset.name).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("111").assertExists()
    }

    @Test
    fun `the history icon navigates`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.history)).performClick()

        assertThat(navigations).containsExactly("history")
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `at tablet width the dhikr card and the counter share the screen`() {
        counterState.value = counterState.value.copy(selectedPreset = preset, count = 3)
        setContent()

        // The expanded layout replaces the bottom peek with a left-hand info card, so the
        // translation appears as its own line rather than folded into the peek's subtitle.
        composeRule.onNodeWithText(preset.translation!!).assertExists()
        composeRule.onNodeWithText(string(R.string.target_format, 33)).assertExists()
        composeRule.onNodeWithTag(ScreenTags.TasbihCount, useUnmergedTree = true)
            .assertTextEquals("3")
    }
}
