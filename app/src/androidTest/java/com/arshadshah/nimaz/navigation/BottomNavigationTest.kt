package com.arshadshah.nimaz.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors.NavLabel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks the five primary destinations via the bottom navigation bar and asserts each
 * target screen actually rendered — keyed off its [ScreenTags] root tag, so the
 * assertion proves navigation (not just the persistent nav bar) and is independent of
 * locale and on-screen copy.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BottomNavigationTest : BaseAppTest() {

    @Test
    fun homeIsTheStartDestination() {
        launchApp()
        assertScreen(ScreenTags.Home)
    }

    @Test
    fun quranTab_showsQuranScreen() {
        launchApp()
        tapBottomNav(NavLabel.QURAN)
        assertScreen(ScreenTags.Quran)
    }

    @Test
    fun tasbihTab_showsTasbihScreen() {
        launchApp()
        tapBottomNav(NavLabel.TASBIH)
        assertScreen(ScreenTags.Tasbih)
    }

    @Test
    fun qiblaTab_showsQiblaScreen() {
        launchApp()
        tapBottomNav(NavLabel.QIBLA)
        assertScreen(ScreenTags.QiblaNav)
    }

    @Test
    fun moreTab_showsMoreScreen() {
        launchApp()
        tapBottomNav(NavLabel.MORE)
        assertScreen(ScreenTags.More)
    }

    @Test
    fun tabsAreRestoredWhenReturningHome() {
        launchApp()
        tapBottomNav(NavLabel.QIBLA)
        assertScreen(ScreenTags.QiblaNav)

        tapBottomNav(NavLabel.HOME)
        assertScreen(ScreenTags.Home)
    }
}
