package com.arshadshah.nimaz.support

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.core.app.ActivityScenario
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.runBlocking
import org.junit.After
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

    /** The launched activity scenario; closed automatically after each test. */
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setup() {
        hiltRule.inject()
        // Make sure the app boots straight to Home, not onboarding.
        runBlocking { settings.setOnboardingCompleted(true) }
    }

    @After
    fun tearDownActivity() {
        scenario?.close()
        scenario = null
    }

    /** Launch [MainActivity] after state has been seeded, and wait for first frame. */
    protected fun launchApp(): ActivityScenario<MainActivity> {
        return ActivityScenario.launch(MainActivity::class.java).also {
            scenario = it
            compose.waitForIdle()
        }
    }

    /**
     * Press the system back button via the activity's dispatcher. Reliable on detail
     * screens where the bottom nav is hidden and back affordances vary.
     */
    protected fun pressBack() {
        scenario?.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
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

    // ── Screen-identity helpers (locale-independent, via ScreenTags) ─────────

    protected fun onTag(tag: String): SemanticsNodeInteraction =
        compose.onNodeWithTag(tag, useUnmergedTree = true)

    @OptIn(ExperimentalTestApi::class)
    protected fun waitForTag(tag: String, timeoutMs: Long = 5_000) {
        compose.waitUntilAtLeastOneExists(hasTestTag(tag), timeoutMs)
    }

    /** Wait for, then assert, that the screen tagged [tag] (from `ScreenTags`) is shown. */
    protected fun assertScreen(tag: String) {
        waitForTag(tag)
        onTag(tag).assertExists()
    }

    /**
     * Scroll the list tagged [listTag] until a node with [text] is composed, then tap it.
     *
     * Clicks coordinate-free via the row's `OnClick` semantics action rather than
     * injecting a touch: a list row scrolled just into view can sit at the viewport
     * edge / under the gesture-nav inset, where a synthetic tap is rejected with
     * "Failed to inject touch input". The menu/settings rows are Material3 `Card(onClick)`s,
     * which expose `SemanticsActions.OnClick`.
     */
    protected fun scrollListToAndTap(listTag: String, text: String) {
        // Exact match (not substring): the row titles are full strings, and substring
        // would make e.g. "Prayer Times" also match "Monthly Prayer Times".
        compose.onNodeWithTag(listTag)
            .performScrollToNode(hasText(text))
        compose.waitForIdle()
        compose.onNodeWithText(text, useUnmergedTree = false)
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    /** Convenience: scroll-and-tap within the More menu's list. */
    protected fun scrollMoreToAndTap(text: String) =
        scrollListToAndTap(ScreenTags.MoreList, text)

    /** Click the bottom-nav tab with the given [Selectors.NavLabel] label. */
    protected fun tapBottomNav(label: String) {
        // Click via the item's testTag (ScreenTags.bottomNav) rather than the label
        // Text: the click/selectable action lives on the merged nav-item node, which is
        // the node carrying the tag — clicking the inner Text node has no click action.
        compose.onNodeWithTag(ScreenTags.bottomNav(label)).performClick()
        compose.waitForIdle()
    }

    /**
     * Click an element by its (substring) text. Uses the **merged** tree so the
     * clickable row — e.g. a menu item whose title is a child Text — is the node
     * tapped, not the non-clickable Text itself.
     */
    protected fun clickText(text: String) {
        compose.onNodeWithText(text, substring = true, useUnmergedTree = false).performClick()
        compose.waitForIdle()
    }

    /** Backwards-compatible alias for [clickText]. */
    protected fun tapText(text: String) = clickText(text)

    /**
     * Click an icon button by its content description. Uses the **merged** semantics
     * tree so the node carrying the click action (the IconButton, which absorbs the
     * inner Icon's description) is the one tapped — not the non-clickable Icon.
     */
    protected fun tapContentDesc(desc: String) {
        compose.onNodeWithContentDescription(desc, useUnmergedTree = false).performClick()
        compose.waitForIdle()
    }

    /** Tap the standard "Back" affordance (contentDescription = R.string.cd_back). */
    protected fun tapBack() = tapContentDesc(Selectors.str(Selectors.Common.back))
}
