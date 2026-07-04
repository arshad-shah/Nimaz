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
 * The Hadith home renders `hadith_books.name_english` behind an `isLoading` gate, so
 * the tiles appear only after the async DB load — wait for the collection before tapping.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HadithOpenCollectionTest : BaseAppTest() {

    @Test
    fun openingACollection_showsItsBookScreen() {
        launchApp()
        openFeatureFromMore(Selectors.More.hadith, ScreenTags.HadithHome)

        // The collection tiles load async behind the isLoading gate; wait for the
        // first collection (shipped as "Sahih al-Bukhari") before opening it.
        waitForText("Sahih al-Bukhari")
        clickText("Sahih al-Bukhari")

        assertScreen(ScreenTags.HadithBook)
    }
}
