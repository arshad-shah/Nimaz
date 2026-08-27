package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression cover for a crash that took the Qur'an home screen down for any
 * reader with two or more bookmarks.
 *
 * `BookmarkEntity` has no id column — its primary key is the composite
 * `(kind, target_id)` — so `QuranRepositoryImpl.toQuranBookmark()` has nothing
 * to map into `QuranBookmark.id` and passes a literal `0`. Both bookmark lists
 * on the home screen keyed by that field, so the second bookmark reused the
 * first one's key and Compose threw:
 *
 *     IllegalArgumentException: Key "0" was already used.
 *
 * The identity is [QuranBookmark.ayahId]: the global ayah id, which is exactly
 * what `(kind = AYAH, target_id)` makes unique.
 */
@RunWith(RobolectricTestRunner::class)
class QuranBookmarkKeyTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createComposeRule()

    private fun bookmark(ayahId: Int, surah: Int, ayah: Int) = QuranBookmark(
        id = 0,
        ayahId = ayahId,
        surahNumber = surah,
        ayahNumber = ayah,
        note = null,
        color = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    /** Two real bookmarks, exactly as the live mapper produces them: both `id = 0`. */
    private val bookmarks = listOf(
        bookmark(ayahId = 1, surah = 1, ayah = 1),
        bookmark(ayahId = 3, surah = 1, ayah = 3),
    )

    @Test
    fun `distinct bookmarks produce distinct keys even though their ids collide`() {
        assertThat(bookmarks.map { it.id }.distinct()).hasSize(1)

        val keys = bookmarks.map { quranBookmarkKey(it) }
        assertThat(keys.distinct()).hasSize(bookmarks.size)
    }

    @Test
    fun `a lazy list keyed this way composes two bookmarks without throwing`() {
        composeRule.setContent {
            LazyRow {
                items(items = bookmarks, key = { quranBookmarkKey(it) }) { bookmark ->
                    Text("${bookmark.surahNumber}:${bookmark.ayahNumber}")
                }
            }
        }
        composeRule.onNodeWithText("1:1").assertExists()
        composeRule.onNodeWithText("1:3").assertExists()
    }

    @Test
    fun `the prefixed key used by the bookmarks list is also unique`() {
        val keys = bookmarks.map { "bm_${quranBookmarkKey(it)}" }
        assertThat(keys.distinct()).hasSize(bookmarks.size)
    }

    @Test
    fun `keying by id would still collide - the bug this pins`() {
        val brokenKeys = bookmarks.map { it.id }
        assertThat(brokenKeys.distinct()).hasSize(1)
    }
}
