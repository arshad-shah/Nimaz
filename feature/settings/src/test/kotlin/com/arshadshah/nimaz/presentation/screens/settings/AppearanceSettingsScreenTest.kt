package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.arshadshah.nimaz.presentation.viewmodel.settings.AppTheme
import com.arshadshah.nimaz.presentation.viewmodel.settings.GeneralSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Appearance screen: the theme picker, the ornament row, and the display toggles.
 *
 * The theme control is the part worth pinning, because it is the one place in the app where the
 * stored value and the control are deliberately *not* one-to-one. "System" is a switch above two
 * cards rather than a third card, so three states are expressed through two controls and the
 * mapping has to be exact in both directions:
 *
 * - Following the device must show **neither** card as picked, or the user reads their theme as
 *   pinned when it is not.
 * - Turning following **off** must land on the theme the device currently resolves to, not on a
 *   fixed one — otherwise switching the toggle off visibly changes the app's appearance, which is
 *   the opposite of what "stop following" means.
 *
 * The ornament row is a `LazyRow`, so it composes only what fits; the swatch tests run wide enough
 * for all five. Its five labels come from a `when` over the enum, which is the shape that goes
 * stale silently when a style is added.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class AppearanceSettingsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    private fun setContent(state: GeneralSettingsUiState = GeneralSettingsUiState()) {
        viewModel.generalState.value = state
        composeRule.setThemedContent {
            AppearanceSettingsScreen(onNavigateBack = { backs++ }, viewModel = viewModel.mock)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the four sections all render`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.appearance_theme)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.appearance_pattern)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.appearance_display)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.appearance_home_screen)).assertIsDisplayed()
    }

    @Test
    fun `tapping the light card pins the light theme`() {
        setContent(GeneralSettingsUiState(theme = AppTheme.SYSTEM))

        composeRule.onNodeWithText(string(R.string.theme_light)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetTheme>().theme).isEqualTo(AppTheme.LIGHT)
    }

    @Test
    fun `tapping the dark card pins the dark theme`() {
        setContent(GeneralSettingsUiState(theme = AppTheme.SYSTEM))

        composeRule.onNodeWithText(string(R.string.theme_dark)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetTheme>().theme).isEqualTo(AppTheme.DARK)
    }

    @Test
    fun `switching follow-device on stores SYSTEM`() {
        setContent(GeneralSettingsUiState(theme = AppTheme.DARK))

        composeRule.onNodeWithText(string(R.string.theme_follow_device)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetTheme>().theme).isEqualTo(AppTheme.SYSTEM)
    }

    @Test
    fun `switching follow-device off lands on the theme the device currently resolves to`() {
        // The test runs under the light system theme, so stopping following must pin LIGHT. A
        // hardcoded `AppTheme.DARK` here would compile, pass a naive assertion, and visibly flip
        // the app dark the moment someone stopped following their device.
        setContent(GeneralSettingsUiState(theme = AppTheme.SYSTEM))

        composeRule.onNodeWithText(string(R.string.theme_follow_device)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetTheme>().theme).isEqualTo(AppTheme.LIGHT)
    }

    @Test
    fun `both theme cards are offered whichever theme is stored`() {
        // The cards are a radio pair rendered by one composable with `dark` flipped. A card that
        // rendered only its selected state would leave no way back to the other theme.
        setContent(GeneralSettingsUiState(theme = AppTheme.DARK))

        composeRule.onNodeWithText(string(R.string.theme_light)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.theme_dark)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w2000dp-h2200dp")
    fun `every ornament style is offered, including the None that turns it off`() {
        // "None" is the first swatch and doubles as the off switch — there is no separate toggle,
        // so a `when` that missed it would leave the ornament permanently on.
        setContent()

        NimazPatternStyle.entries.forEach { style ->
            val label = when (style) {
                NimazPatternStyle.NONE -> R.string.pattern_none
                NimazPatternStyle.CORNER_MEDALLION -> R.string.pattern_medallion
                NimazPatternStyle.LATTICE -> R.string.pattern_lattice
                NimazPatternStyle.STAR_FIELD -> R.string.pattern_star_field
                NimazPatternStyle.ATELIER -> R.string.pattern_atelier
            }
            composeRule.onNodeWithText(string(label)).assertExists()
        }
    }

    @Test
    fun `picking an ornament sends that style and not its neighbour`() {
        setContent(GeneralSettingsUiState(patternStyle = NimazPatternStyle.NONE))

        composeRule.onNodeWithText(string(R.string.pattern_lattice)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetPatternStyle>().style)
            .isEqualTo(NimazPatternStyle.LATTICE)
    }

    @Test
    fun `picking None is how the ornament is switched off`() {
        setContent(GeneralSettingsUiState(patternStyle = NimazPatternStyle.ATELIER))

        composeRule.onNodeWithText(string(R.string.pattern_none)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetPatternStyle>().style)
            .isEqualTo(NimazPatternStyle.NONE)
    }

    @Test
    fun `each display toggle dispatches its own event`() {
        // Three identical rows, three near-identical events. This is the crossing the module's
        // instrumented `SettingsBehaviorTest` covers for four toggles end-to-end; this is the
        // same check on the JVM, and it runs on every PR rather than on the emulator lane.
        setContent(
            GeneralSettingsUiState(
                animationsEnabled = true,
                hapticFeedback = true,
                use24HourFormat = false,
            )
        )

        composeRule.onNodeWithText(string(R.string.appearance_animations)).performClick()
        composeRule.onNodeWithText(string(R.string.appearance_haptic)).performClick()
        composeRule.onNodeWithText(string(R.string.appearance_24hour)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetAnimationsEnabled>().enabled).isFalse()
        assertThat(viewModel.only<SettingsEvent.SetHapticFeedback>().enabled).isFalse()
        assertThat(viewModel.only<SettingsEvent.Set24HourFormat>().enabled).isTrue()
    }

    @Test
    fun `the Islamic date toggle dispatches the hijri event`() {
        setContent(GeneralSettingsUiState(useHijriPrimary = false))

        composeRule.onNodeWithText(string(R.string.appearance_show_islamic_date)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetHijriPrimary>().enabled).isTrue()
    }

    @Test
    fun `the hijri offset stepper moves one day at a time`() {
        setContent(GeneralSettingsUiState(hijriDayOffset = 0))

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_increase)).onLast()
            .performClick()

        assertThat(viewModel.only<SettingsEvent.SetHijriDayOffset>().days).isEqualTo(1)
    }

    @Test
    fun `the hijri offset stepper stops at its two-day bound`() {
        // The Hijri date is corrected by at most two days either way — that is the whole range of
        // moon-sighting disagreement. The stepper expresses the bound by *disabling* the button
        // rather than clamping the value, so at +2 the tap must produce no event at all. A
        // stepper that clamped instead would still be correct arithmetically and would keep
        // firing writes on every tap at the limit.
        setContent(GeneralSettingsUiState(hijriDayOffset = 2))

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_increase)).onLast()
            .performClick()

        assertThat(viewModel.events).isEmpty()
    }

    @Test
    fun `the hijri offset stepper stops at its negative bound too`() {
        setContent(GeneralSettingsUiState(hijriDayOffset = -2))

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_decrease)).onFirst()
            .performClick()

        assertThat(viewModel.events).isEmpty()
    }

    @Test
    fun `the hijri offset stepper decrements to negative offsets`() {
        setContent(GeneralSettingsUiState(hijriDayOffset = 0))

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_decrease)).onFirst()
            .performClick()

        assertThat(viewModel.only<SettingsEvent.SetHijriDayOffset>().days).isEqualTo(-1)
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
