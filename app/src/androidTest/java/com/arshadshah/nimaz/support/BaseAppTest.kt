package com.arshadshah.nimaz.support

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.core.app.ActivityScenario
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

/**
 * Base class for end-to-end UI flow tests that drive the real [MainActivity] and its
 * [com.arshadshah.nimaz.core.navigation.NavGraph] on the full Hilt graph.
 *
 * Key design choices:
 *  - **Empty compose rule + manual launch.** [createEmptyComposeRule] lets us seed
 *    DataStore state *before* the activity is created. That matters because
 *    `NavGraph` resolves its start destination from `onboardingCompleted` exactly
 *    once; if we launched first and wrote the flag afterwards, the app would be stuck
 *    on the onboarding screen and the bottom nav would never appear. So [setup]
 *    marks onboarding complete, then each test calls [launchApp].
 *  - **Hilt injection** of the real [SettingsRepository] (the DataStore-backed impl),
 *    so the seed hits the same store the app reads.
 *
 * Subclasses get [compose] plus the text/contentDescription helpers below, all of
 * which resolve their target strings through [Selectors] rather than inline literals.
 *
 * Note: `@HiltAndroidTest` is not inherited, so it sits on each concrete subclass
 * (e.g. [com.arshadshah.nimaz.navigation.AppLaunchTest]) rather than here. The
 * [HiltAndroidRule] below still injects fields declared on this base class.
 */
abstract class BaseAppTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose: ComposeTestRule = createEmptyComposeRule()

    @Inject
    lateinit var settings: SettingsRepository

    @Before
    fun setup() {
        hiltRule.inject()
        // Make sure the app boots straight to Home, not onboarding.
        runBlocking { settings.setOnboardingCompleted(true) }
    }

    /** Launch [MainActivity] after state has been seeded, and wait for first frame. */
    protected fun launchApp(): ActivityScenario<MainActivity> {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
        return scenario
    }

    // ── Selector-aware helpers (no inline literals in test bodies) ───────────

    /** Wait until a node with the given visible [text] exists (default 5s). */
    @OptIn(ExperimentalTestApi::class)
    protected fun waitForText(text: String, timeoutMs: Long = 5_000) {
        compose.waitUntilAtLeastOneExists(hasText(text, substring = true), timeoutMs)
    }

    /** Wait for a node identified by a string resource id. */
    protected fun waitForRes(resId: Int, timeoutMs: Long = 5_000) =
        waitForText(Selectors.str(resId), timeoutMs)

    protected fun onText(text: String): SemanticsNodeInteraction =
        compose.onNodeWithText(text, substring = true, useUnmergedTree = true)

    protected fun onRes(resId: Int): SemanticsNodeInteraction =
        compose.onNodeWithText(Selectors.str(resId), substring = true, useUnmergedTree = true)

    protected fun onContentDesc(desc: String): SemanticsNodeInteraction =
        compose.onNodeWithContentDescription(desc, substring = true, useUnmergedTree = true)

    /** Click the bottom-nav tab with the given [Selectors.NavLabel] label. */
    protected fun tapBottomNav(label: String) {
        // The NavigationSuite item exposes the label as both text and the icon's
        // contentDescription; click the text node which is reliably present.
        compose.onNodeWithText(label, useUnmergedTree = true).performClick()
        compose.waitForIdle()
    }

    /** Tap a visible text element and settle. */
    protected fun tapText(text: String) {
        onText(text).performClick()
        compose.waitForIdle()
    }

    /** Tap the standard "Back" affordance (contentDescription = R.string.cd_back). */
    protected fun tapBack() {
        onContentDesc(Selectors.str(Selectors.Common.back)).performClick()
        compose.waitForIdle()
    }
}
