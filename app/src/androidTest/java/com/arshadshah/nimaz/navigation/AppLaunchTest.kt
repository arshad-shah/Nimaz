package com.arshadshah.nimaz.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors.NavLabel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The most basic guarantee: with onboarding already complete the app cold-launches
 * straight to the home experience and renders its five-tab bottom navigation. If the
 * Hilt graph, theme, or NavGraph start-destination logic regresses, this fails first.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppLaunchTest : BaseAppTest() {

    @Test
    fun appLaunches_andShowsAllBottomNavTabs() {
        launchApp()

        NavLabel.all.forEach { label ->
            waitForText(label)
            onText(label).assertExists()
        }
    }
}
