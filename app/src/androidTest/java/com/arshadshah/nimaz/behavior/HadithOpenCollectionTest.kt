package com.arshadshah.nimaz.behavior

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The "browse Hadith" drill-in: More → Hadith opens the collections, and tapping a
 * collection opens its book screen. Collections ship in the prepackaged asset DB, so
 * no seeding is needed. Read-only.
 *
 * NOT YET RUN on a device — if it fails, confirm the collection title contains
 * "Bukhari" (substring match) on the shipped Hadith home.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HadithOpenCollectionTest : BaseAppTest() {

    @Test
    fun openingACollection_showsItsBookScreen() {
        launchApp()
        openFeatureFromMore(Selectors.More.hadith, ScreenTags.HadithHome)

        // Sahih al-Bukhari ships in the asset DB; match on a substring so a
        // "Sahih al-Bukhari" / "Bukhari" label variation still resolves.
        clickText("Bukhari")

        assertScreen(ScreenTags.HadithBook)
    }
}
