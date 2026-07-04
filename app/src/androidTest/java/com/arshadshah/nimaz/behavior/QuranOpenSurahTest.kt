package com.arshadshah.nimaz.behavior

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors
import com.arshadshah.nimaz.support.Selectors.NavLabel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The core "read the Quran" flow: from the Quran home browse list, opening a surah
 * must land on the reader. Content (surah names) ships in the prepackaged asset DB,
 * so no seeding is needed. Read-only — writes no user data.
 *
 * The shipped `surahs.name_english` for chapter 1 is "Al-Fatiha" (no trailing 'h').
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class QuranOpenSurahTest : BaseAppTest() {

    @Test
    fun openingASurah_showsTheReader() {
        launchApp()
        tapBottomNav(NavLabel.QURAN)
        assertScreen(ScreenTags.Quran)

        // The Quran home opens on the "Home" overview tab (topTab 0); the surah list
        // lives under the "Browse" tab (topTab 1), so select it first. Then scroll the
        // tagged surah list to Al-Fatiha and open it via its card's OnClick semantics.
        clickText(Selectors.str(Selectors.Quran.browseTab))
        scrollListToAndTap(ScreenTags.QuranSurahList, "Al-Fatiha")

        assertScreen(ScreenTags.QuranReader)
    }
}
