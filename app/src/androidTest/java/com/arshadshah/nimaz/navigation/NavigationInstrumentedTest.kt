package com.arshadshah.nimaz.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.HiltTestActivity
import com.arshadshah.nimaz.core.navigation.NavGraph
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.testing.TestTags
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bottom-navigation user-flow test driven through the real [NavGraph] (selected
 * via [TestTags]). Hosted in a Hilt activity and started on Home so the
 * navigation bar is present (a cold launch lands on Onboarding, which has no
 * bottom bar).
 *
 * NOT run in CI (no emulator) — run locally / on Firebase Test Lab.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.setContent {
            navController = rememberNavController()
            NimazTheme {
                NavGraph(navController = navController)
            }
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            navController.currentDestination != null
        }
        // Land on a main screen so the bottom navigation is shown.
        composeRule.runOnUiThread { navController.navigate(Route.Home) }
        composeRule.waitForIdle()
    }

    @Test
    fun bottomNav_allSectionsAreDisplayed() {
        composeRule.onNodeWithTag(TestTags.NAV_HOME).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.NAV_QURAN).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.NAV_TASBIH).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.NAV_QIBLA).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.NAV_MORE).assertIsDisplayed()
    }

    @Test
    fun bottomNav_clickThroughEverySection() {
        composeRule.onNodeWithTag(TestTags.NAV_QURAN).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.NAV_TASBIH).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.NAV_QIBLA).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.NAV_MORE).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.NAV_HOME).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.NAV_HOME).assertIsDisplayed()
    }
}
