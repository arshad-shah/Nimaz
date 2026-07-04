package com.arshadshah.nimaz.behavior

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors.NavLabel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The core "read the Quran" flow: from the Quran home browse list, opening a surah
 * must land on the reader. Content (surah names) ships in the prepackaged asset DB,
 * so no seeding is needed. Read-only — writes no user data.
 *
 * NOT YET RUN on a device — verify the surah-name literal ("Al-Fatihah") matches the
 * shipped `surah_info` English name if this fails.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class QuranOpenSurahTest : BaseAppTest() {

    @Test
    fun openingASurah_showsTheReader() {
        launchApp()
        tapBottomNav(NavLabel.QURAN)
        assertScreen(ScreenTags.Quran)

        // Browse (Surah) is the default tab; scroll the tagged surah list to
        // Al-Fatihah and open it via its card's OnClick semantics.
        scrollListToAndTap(ScreenTags.QuranSurahList, "Al-Fatihah")

        assertScreen(ScreenTags.QuranReader)
    }
}
