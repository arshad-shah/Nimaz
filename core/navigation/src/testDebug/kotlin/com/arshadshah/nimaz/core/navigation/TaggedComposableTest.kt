package com.arshadshah.nimaz.core.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The wrapper every destination in the app is wired with.
 *
 * `CLAUDE.md` requires `taggedComposable<Route.X>(ScreenTags.X)` rather than a bare `composable`,
 * and `check_docs.py`'s NAV-04 fails a destination wired without one — so the *usage* is enforced
 * from two directions. What neither of them looks at is whether the helper still does its job.
 *
 * That gap matters because of where the failure would land. Every instrumented navigation test
 * asserts which screen is showing by its tag, so a `taggedComposable` that stopped tagging turns
 * into a wall of red on an emulator, in tests that are about something else entirely, with
 * nothing on the JVM having said a word. This is the cheapest place to catch it.
 *
 * The wrapper also has to stay *transparent*: it exists to add a tag, not to change what a
 * destination receives. A `Box` that swallowed the back-stack entry would break every screen
 * that reads its own arguments — which, with type-safe navigation, is most of them.
 */
@RunWith(RobolectricTestRunner::class)
class TaggedComposableTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a destination is wrapped in a node carrying its tag`() {
        composeRule.setContent {
            val controller = rememberNavController()
            NavHost(navController = controller, startDestination = Route.Home) {
                taggedComposable<Route.Home>(ScreenTags.Home) { Text("home screen") }
            }
        }

        composeRule.onNodeWithTag(ScreenTags.Home).assertExists()
        composeRule.onNodeWithText("home screen").assertIsDisplayed()
    }

    @Test
    fun `the tag belongs to the destination that is showing, and not to the others`() {
        composeRule.setContent {
            val controller = rememberNavController()
            NavHost(navController = controller, startDestination = Route.Home) {
                taggedComposable<Route.Home>(ScreenTags.Home) { Text("home screen") }
                taggedComposable<Route.Settings>(ScreenTags.Settings) { Text("settings screen") }
            }
        }

        composeRule.onNodeWithTag(ScreenTags.Home).assertExists()
        composeRule.onNodeWithTag(ScreenTags.Settings).assertDoesNotExist()
    }

    @Test
    fun `navigating swaps which tag is present`() {
        lateinit var navigate: () -> Unit
        composeRule.setContent {
            val controller = rememberNavController()
            navigate = { controller.navigate(Route.Settings) }
            NavHost(navController = controller, startDestination = Route.Home) {
                taggedComposable<Route.Home>(ScreenTags.Home) { Text("home screen") }
                taggedComposable<Route.Settings>(ScreenTags.Settings) { Text("settings screen") }
            }
        }

        composeRule.runOnIdle { navigate() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ScreenTags.Settings).assertExists()
    }

    @Test
    fun `the destination still receives its own back-stack entry`() {
        // The wrapper forwards the entry rather than consuming it; a destination that could not
        // read its own arguments would be a blank screen under type-safe navigation.
        var seenRoute: String? = null
        composeRule.setContent {
            val controller = rememberNavController()
            NavHost(navController = controller, startDestination = Route.Home) {
                taggedComposable<Route.Home>(ScreenTags.Home) { entry ->
                    seenRoute = entry.destination.route
                    Text("home screen")
                }
            }
        }
        composeRule.waitForIdle()

        assertThat(seenRoute).isNotNull()
        assertThat(seenRoute).contains("Home")
    }

    @Test
    fun `a route carrying arguments arrives with them intact`() {
        var seenSurah: Int? = null
        composeRule.setContent {
            val controller = rememberNavController()
            NavHost(navController = controller, startDestination = Route.SurahBackground(18)) {
                taggedComposable<Route.SurahBackground>(ScreenTags.SurahBackground) { entry ->
                    seenSurah = entry.arguments?.getInt("surahNumber")
                    Text("background")
                }
            }
        }
        composeRule.waitForIdle()

        assertThat(seenSurah).isEqualTo(18)
    }

    @Test
    fun `two destinations may not be given the same tag, and this is how you would notice`() {
        // Not a guard the helper enforces — it cannot — but a rendered duplicate is what a
        // copy-pasted `ScreenTags` constant looks like, and the assertion below is the shape a
        // navigation test would fail with rather than the confusing "found 2 nodes" it gets now.
        composeRule.setContent {
            val controller = rememberNavController()
            NavHost(navController = controller, startDestination = Route.Home) {
                taggedComposable<Route.Home>(ScreenTags.Home) { Text("home screen") }
            }
        }

        composeRule.onAllNodesWithTag(ScreenTags.Home).assertCountEquals(1)
    }
}
