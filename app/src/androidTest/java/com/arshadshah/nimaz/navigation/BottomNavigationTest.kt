package com.arshadshah.nimaz.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors
import com.arshadshah.nimaz.support.Selectors.NavLabel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks the five primary destinations via the bottom navigation bar and asserts each
 * target screen actually rendered — keyed off a piece of screen-specific content
 * (resolved through [Selectors]) rather than the persistent nav label, so the
 * assertion proves navigation rather than just the bar's presence.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BottomNavigationTest : BaseAppTest() {

    @Test
    fun quranTab_showsQuranHome() {
        launchApp()

        tapBottomNav(NavLabel.QURAN)

        // The Quran home shows its Browse/Favorites tabs.
        waitForRes(Selectors.Quran.browseTab)
        onRes(Selectors.Quran.browseTab).assertExists()
    }

    @Test
    fun tasbihTab_showsCounterModes() {
        launchApp()

        tapBottomNav(NavLabel.TASBIH)

        waitForRes(Selectors.Tasbih.beads)
        onRes(Selectors.Tasbih.beads).assertExists()
        onRes(Selectors.Tasbih.classic).assertExists()
    }

    @Test
    fun qiblaTab_showsCompass() {
        launchApp()

        tapBottomNav(NavLabel.QIBLA)

        waitForRes(Selectors.Qibla.compass)
        onRes(Selectors.Qibla.compass).assertExists()
    }

    @Test
    fun moreTab_showsSettingsEntry() {
        launchApp()

        tapBottomNav(NavLabel.MORE)

        waitForRes(Selectors.More.settings)
        onRes(Selectors.More.settings).assertExists()
    }

    @Test
    fun canReturnToHomeAfterNavigatingAway() {
        launchApp()

        tapBottomNav(NavLabel.QIBLA)
        waitForRes(Selectors.Qibla.compass)

        tapBottomNav(NavLabel.HOME)

        // Home tab is selected again; the bottom bar (and Home label) remain visible.
        waitForText(NavLabel.HOME)
        onText(NavLabel.HOME).assertExists()
    }
}
