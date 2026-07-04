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
 * The Hadith home renders `hadith_books.name_english` behind an `isLoading` gate, and the
 * collections grid sits below the tall stats + hadith-of-the-day cards in the scrolling
 * list — so the tiles both load async and start off-screen (uncomposed). Scroll the tagged
 * list to the collection before tapping it.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HadithOpenCollectionTest : BaseAppTest() {

    @Test
    fun openingACollection_showsItsBookScreen() {
        launchApp()
        openFeatureFromMore(Selectors.More.hadith, ScreenTags.HadithHome)

        // The collection tiles load async behind the isLoading gate and start below the
        // fold; scroll the tagged list to the first collection (shipped as
        // "Sahih al-Bukhari") and open it via its card's OnClick semantics.
        scrollListToAndTap(ScreenTags.HadithBookList, "Sahih al-Bukhari")

        assertScreen(ScreenTags.HadithBook)
    }
}
