package com.arshadshah.nimaz.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end navigation tests that drive the real app through Hilt + Compose.
 *
 * IMPORTANT: these are NOT run in the current CI lane — there is no emulator.
 * They are compiled (`assembleDebugAndroidTest`) so the code is verified and
 * cannot bit-rot, and are ready to execute on a device or Firebase Test Lab
 * once an instrumented-test lane is added. See HiltTestRunner.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun appLaunches_bottomNavigationIsVisible() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Quran").assertIsDisplayed()
        composeRule.onNodeWithText("Qibla").assertIsDisplayed()
    }

    @Test
    fun bottomNav_navigateToQuran_thenBackToHome() {
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Quran").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun bottomNav_navigateToTasbih() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tasbih").performClick()
        composeRule.waitForIdle()
    }
}
