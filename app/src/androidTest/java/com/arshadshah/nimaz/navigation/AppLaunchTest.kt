package com.arshadshah.nimaz.navigation

import com.arshadshah.nimaz.core.navigation.ScreenTags
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors.NavLabel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The most basic guarantee: with onboarding already complete the app cold-launches
 * straight to Home and renders its five-tab bottom navigation. Asserted by
 * [ScreenTags] (the nav item + screen-root tags) so it never flakes on duplicated
 * on-screen copy. If the Hilt graph, theme, or NavGraph start-destination logic
 * regresses, this fails first.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppLaunchTest : BaseAppTest() {

    @Test
    fun appLaunches_toHome_withAllBottomNavTabs() {
        launchApp()

        // Started on Home.
        assertScreen(ScreenTags.Home)

        // All five tabs are present (by their stable item tags).
        NavLabel.all.forEach { label ->
            onTag(ScreenTags.bottomNav(label)).assertExists()
        }
    }
}
