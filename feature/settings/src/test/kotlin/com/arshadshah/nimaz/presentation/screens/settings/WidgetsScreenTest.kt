package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.FallbackLocation
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.arshadshah.nimaz.domain.prayer.PrayerTimeCalculator
import com.arshadshah.nimaz.presentation.components.atoms.ProvideNimazClock
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The widget gallery: six live previews rendered from the user's own stored location.
 *
 * "Live" is what makes this worth testing rather than looking at. Every preview is built by
 * `buildWidgetPreviewData`, which runs the **real** `PrayerTimeCalculator` against the stored
 * coordinates once a second, so a gallery that showed placeholder dashes would be indistinguishable
 * from one that worked — until someone added a widget and found it disagreed with the preview that
 * sold it to them.
 *
 * Three specific claims:
 *
 * - **The preferences come from the ViewModel's seam.** This screen used to construct its own
 *   `PreferencesDataStore(context)` — a second instance of a `@Singleton`, built outside Hilt,
 *   reading the file the injected one owns. Passing a null preference must therefore still render
 *   the gallery rather than crash it, because null is what the `WhileSubscribed` flow starts at.
 * - **A location that was never set falls back to Dublin** rather than computing prayer times for
 *   (0, 0), which is in the Atlantic and produces times nobody's day resembles.
 * - **The countdown is derived from the shared ticker**, so it can be driven from a fixed clock —
 *   which is the only way to assert that it says something specific rather than a dash.
 *
 * The clock is pinned through `ProvideNimazClock(timeSource = …)`, the seam `:core:ui` publishes
 * for exactly this. Without it the countdown is whatever the wall clock says when the test runs,
 * and the only assertable thing left is that the screen did not crash.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h4000dp")
class WidgetsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    /** A fixed instant, so the countdown and the "next prayer" are the same on every run. */
    private val fixedNow = Instant.parse("2026-08-26T09:00:00Z")

    private fun preferences(
        latitude: Double = 51.5074,
        longitude: Double = -0.1278,
        locationName: String = "London, United Kingdom",
    ) = UserPreferences(
        onboardingCompleted = true,
        themeMode = "system",
        dynamicColor = false,
        appLanguage = "en",
        calculationMethod = "MUSLIM_WORLD_LEAGUE",
        asrCalculation = "standard",
        latitude = latitude,
        longitude = longitude,
        locationName = locationName,
        prayerNotificationsEnabled = true,
        quranTranslatorId = "sahih_international",
        showTranslation = true,
    )

    private fun setContent(prefs: UserPreferences? = preferences()) {
        // The real calculator, not a mock: the previews exist to show what the widgets will
        // show, and a stubbed one would let the gallery agree with nothing.
        every { viewModel.mock.prayerTimeCalculator } returns PrayerTimeCalculator()
        viewModel.widgetPreviewPreferences.value = prefs
        composeRule.setThemedContent {
            ProvideNimazClock(timeSource = { fixedNow }) {
                WidgetsScreen(onNavigateBack = { backs++ }, viewModel = viewModel.mock)
            }
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `all six widgets are shown in the gallery`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.widget_next_prayer_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.widget_prayer_times_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.widget_hijri_date_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.widget_prayer_tracker_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.widget_hijri_calendar_title)).assertExists()
        composeRule.onAllNodesWithText(string(R.string.khatam_widget_label)).onFirst()
            .assertExists()
    }

    @Test
    fun `each widget's info row names its size, so the gallery is usable before adding one`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.widget_next_prayer_size)).assertExists()
        composeRule.onNodeWithText(string(R.string.widget_prayer_times)).assertExists()
        composeRule.onNodeWithText(string(R.string.widget_hijri_date)).assertExists()
        composeRule.onNodeWithText(string(R.string.widget_prayer_tracker)).assertExists()
        composeRule.onNodeWithText(string(R.string.widget_hijri_calendar)).assertExists()
    }

    @Test
    fun `the previews render the stored location's own name, not a placeholder`() {
        // The name is split on the first comma — "London, United Kingdom" is a location, "London"
        // is what fits in a 2x2 widget.
        setContent(preferences(locationName = "London, United Kingdom"))

        composeRule.onAllNodesWithText("London").onFirst().assertExists()
    }

    @Test
    fun `a blank stored location name falls back rather than rendering an empty widget`() {
        setContent(preferences(locationName = ""))

        composeRule.onAllNodesWithText("Dublin").onFirst().assertExists()
    }

    @Test
    fun `an unset location computes against Dublin, not against the Atlantic`() {
        // (0, 0) is in the Gulf of Guinea. `resolveLocation` exists so a user who has not set a
        // location yet still sees plausible times rather than ones nobody's day resembles.
        setContent(preferences(latitude = 0.0, longitude = 0.0, locationName = ""))

        composeRule.onAllNodesWithText("Dublin").onFirst().assertExists()
    }

    @Test
    fun `the gallery still renders before the preferences have arrived`() {
        // `widgetPreviewPreferences` is a `WhileSubscribed` flow starting at null, so this is
        // the state every cold open passes through. A non-null assumption crashes on entry.
        setContent(prefs = null)

        composeRule.onNodeWithText(string(R.string.widget_next_prayer_title)).assertExists()
        composeRule.onAllNodesWithText("—").onFirst().assertExists()
    }

    @Test
    fun `the previews show times the calculator worked out, not placeholders`() {
        // The assertion that the gallery is *computed* rather than mocked up. The exact times
        // depend on the date the suite runs on, so what is pinned is that five real clock times
        // were formatted — an unwired preview renders the em dash for every one of them, and
        // looks entirely plausible in a screenshot.
        setContent()

        val times = composeRule.onRoot().fetchSemanticsNode().renderedTexts()
            .filter { CLOCK_TIME.matches(it) }

        assertThat(times.size).isAtLeast(5)
    }

    @Test
    fun `a gallery with no preferences renders placeholders instead of times`() {
        // The other half: the em dash is correct *here*, before the flow has emitted, and the
        // two states have to be distinguishable or the test above proves nothing.
        setContent(prefs = null)

        val rendered = composeRule.onRoot().fetchSemanticsNode().renderedTexts()

        assertThat(rendered.none { CLOCK_TIME.matches(it) }).isTrue()
        assertThat(rendered).contains("—")
    }

    @Test
    fun `the previews name the five daily prayers`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.widget_prayer_short_fajr)).onFirst()
            .assertExists()
        composeRule.onAllNodesWithText(string(R.string.widget_prayer_short_maghrib)).onFirst()
            .assertExists()
    }

    @Test
    fun `the screen explains how to add a widget, since nothing on it can`() {
        // The gallery cannot place a widget — only the launcher can — so the steps are the only
        // actionable thing on the screen.
        setContent()

        composeRule.onNodeWithText(string(R.string.widgets_how_to)).assertExists()
        composeRule.onNodeWithText(string(R.string.widgets_how_to_step_1)).assertExists()
        composeRule.onNodeWithText(string(R.string.widgets_how_to_step_4)).assertExists()
        composeRule.onNodeWithText(string(R.string.widgets_intro)).assertIsDisplayed()
    }

    @Test
    fun `a location that cannot be resolved still renders the gallery`() {
        // The `try` around the preview build is deliberate — a preview is not worth taking the
        // screen down for — and coordinates outside the valid range are the input that reaches
        // it from a malformed stored value.
        setContent(preferences(latitude = 999.0, longitude = 999.0, locationName = "Nowhere"))

        composeRule.onNodeWithText(string(R.string.widget_next_prayer_title)).assertExists()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }

    /** Every string the tree renders, flattened — the previews carry no test tags of their own. */
    private fun SemanticsNode.renderedTexts(): List<String> =
        config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            children.flatMap { it.renderedTexts() }

    private companion object {
        /** What `formatWidgetTime` produces for a prayer: "3:37", "12:03". */
        val CLOCK_TIME = Regex("""\d{1,2}:\d{2}""")
    }
}
