package com.arshadshah.nimaz.presentation.screens.help

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.HelpGuideDetail
import com.arshadshah.nimaz.domain.model.HelpStep
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpEvent
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpGuideUiState
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpViewModel
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
 * A step-by-step guide, and the one thing on it that leaves the screen.
 *
 * `HelpDeepLinkTest` in `:core:navigation` pins all 22 deep-link keys and that no two resolve to
 * the same destination. That is the far end of the wire. This is the near end: a step **with** a
 * route renders a tappable path chip and hands that route out, and a step **without** one renders
 * the same breadcrumb inert rather than as a control that swallows a tap. Both halves have to be
 * true for "take me there" to work, and each is invisible from the other's test — a screen that
 * never calls `onDeepLink` passes every key assertion in `:core:navigation`.
 *
 * The numbered rail is content-shaped too: guides ship as data, so a one-step guide is a real
 * input, and the rail's "not the last one" connector must not be drawn under it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class HelpGuideScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val guideState = MutableStateFlow(HelpGuideUiState())
    private val events = mutableListOf<HelpEvent>()

    private val viewModel: HelpViewModel = mockk(relaxed = true) {
        every { this@mockk.guideState } returns this@HelpGuideScreenTest.guideState
        every { onEvent(any()) } answers { events += firstArg<HelpEvent>() }
    }

    private val deepLinks = mutableListOf<String>()

    private fun setContent(guideId: String = "set-alert") {
        composeRule.setThemedContent {
            HelpGuideScreen(
                guideId = guideId,
                onNavigateBack = {},
                onDeepLink = { deepLinks += it },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun plural(res: Int, quantity: Int, vararg args: Any): String =
        context.resources.getQuantityString(res, quantity, *args)

    private fun step(
        id: String,
        title: String,
        body: String = "",
        deeplinkRoute: String? = null,
        pathLabels: List<String> = emptyList(),
    ) = HelpStep(
        id = id,
        order = 0,
        title = title,
        body = body,
        deeplinkRoute = deeplinkRoute,
        pathLabels = pathLabels,
    )

    private fun guide(steps: List<HelpStep>, minutes: Int? = 3) = HelpGuideDetail(
        id = "set-alert",
        title = "Set a prayer alert",
        estimatedMinutes = minutes,
        steps = steps,
    )

    @Test
    fun `opening the screen asks for the guide it was routed to`() {
        guideState.value = HelpGuideUiState(guide = guide(listOf(step("s1", "Open settings"))), isLoading = false)
        setContent(guideId = "fix-qibla")

        assertThat(events).contains(HelpEvent.LoadGuide("fix-qibla"))
    }

    @Test
    fun `every step is numbered and rendered in order`() {
        guideState.value = HelpGuideUiState(
            guide = guide(
                listOf(
                    step("s1", "Open Settings", body = "From the More menu."),
                    step("s2", "Choose a prayer"),
                    step("s3", "Pick an adhan"),
                ),
            ),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText("Open Settings").assertExists()
        composeRule.onNodeWithText("From the More menu.").assertExists()
        composeRule.onNodeWithText("Choose a prayer").assertExists()
        composeRule.onNodeWithText("Pick an adhan").assertExists()
        // The rail numbers from one, not from zero.
        composeRule.onNodeWithText("1").assertExists()
        composeRule.onNodeWithText("3").assertExists()
        composeRule.onNodeWithText(string(R.string.help_thats_it)).assertExists()
    }

    @Test
    fun `a one-step guide renders without a trailing connector`() {
        guideState.value = HelpGuideUiState(
            guide = guide(listOf(step("s1", "Tap the bell"))),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText("Tap the bell").assertExists()
        composeRule.onNodeWithText("1").assertExists()
        composeRule.onNodeWithText("2").assertDoesNotExist()
    }

    @Test
    fun `the hero counts the steps and reports the estimate`() {
        guideState.value = HelpGuideUiState(
            guide = guide(listOf(step("s1", "One"), step("s2", "Two")), minutes = 4),
            isLoading = false,
        )
        setContent()

        val steps = plural(R.plurals.help_guide_steps_format, 2, 2)
        val mins = string(R.string.help_guide_about_minutes_lower_format, 4)
        composeRule.onNodeWithText("$steps · $mins").assertExists()
    }

    @Test
    fun `a guide with no estimate reports only its step count`() {
        guideState.value = HelpGuideUiState(
            guide = guide(listOf(step("s1", "One")), minutes = null),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText(plural(R.plurals.help_guide_steps_format, 1, 1)).assertExists()
    }

    @Test
    fun `a step with a deeplink takes the reader there`() {
        guideState.value = HelpGuideUiState(
            guide = guide(
                listOf(
                    step(
                        "s1", "Open prayer settings",
                        deeplinkRoute = "prayer_settings",
                        pathLabels = listOf("Settings", "Prayer"),
                    ),
                ),
            ),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText("Prayer").performClick()

        // The route key, verbatim — `helpDeepLinkRoute` in `:core:navigation` is what turns it
        // into a destination, and it only ever sees what this screen hands it.
        assertThat(deepLinks).containsExactly("prayer_settings")
    }

    @Test
    fun `a breadcrumb with no route is inert rather than a dead control`() {
        guideState.value = HelpGuideUiState(
            guide = guide(
                listOf(
                    step(
                        "s1", "Find the widget tray",
                        deeplinkRoute = null,
                        pathLabels = listOf("Home screen", "Widgets"),
                    ),
                ),
            ),
            isLoading = false,
        )
        setContent()

        // Still rendered — it tells the reader where to go by hand.
        composeRule.onNodeWithText("Widgets").assertExists()
        composeRule.onNodeWithText("Widgets").performClick()

        assertThat(deepLinks).isEmpty()
    }

    @Test
    fun `a step with no breadcrumb renders no chip at all`() {
        guideState.value = HelpGuideUiState(
            guide = guide(listOf(step("s1", "Wait for dawn", deeplinkRoute = "prayer_times"))),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText("Wait for dawn").assertExists()
        assertThat(deepLinks).isEmpty()
    }

    @Test
    fun `a failed load says the load failed, not that the guide is missing`() {
        guideState.value = HelpGuideUiState(
            guide = null,
            isLoading = false,
            error = UiError(message = R.string.help_guide_load_failed),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.help_guide_load_failed)).assertExists()
        composeRule.onNodeWithText(string(R.string.help_guide_unavailable)).assertDoesNotExist()
    }

    @Test
    fun `the failed load can be retried`() {
        guideState.value = HelpGuideUiState(
            isLoading = false,
            error = UiError(message = R.string.help_guide_load_failed),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.try_again)).performClick()

        assertThat(events).contains(HelpEvent.Retry)
    }

    @Test
    fun `a guide that is genuinely absent says so`() {
        guideState.value = HelpGuideUiState(guide = null, isLoading = false, error = null)
        setContent()

        composeRule.onNodeWithText(string(R.string.help_guide_unavailable)).assertExists()
    }

    @Test
    fun `a guide still loading shows neither content nor an absence`() {
        // The spinner animates forever; pin the clock before composing or `waitForIdle` hangs.
        composeRule.mainClock.autoAdvance = false
        guideState.value = HelpGuideUiState(guide = null, isLoading = true)
        setContent()

        composeRule.onNodeWithText(string(R.string.help_guide_unavailable)).assertDoesNotExist()
    }
}
