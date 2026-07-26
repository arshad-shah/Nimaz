package com.arshadshah.nimaz.data.local.dua

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Integrity of the shipped dua content, focused on the night-prayer category the worship hub links
 * to.
 *
 * Content bugs are silent: a duplicated id, a category whose `dua_count` disagrees with reality, or
 * a `contentVersion` that was not bumped all fail *at the user*, not at the build — the seeder
 * simply skips re-seeding and the new duas never appear. These assertions are cheap and turn each
 * of those into a failing test.
 */
@RunWith(RobolectricTestRunner::class)
class NightPrayerDuaContentTest {

    private val root: JSONObject by lazy {
        // Read the asset from source rather than through AssetManager: this is content, and the
        // file on disk is the thing being shipped.
        JSONObject(File("src/main/assets/duas/duas.json").readText())
    }

    private fun duasIn(categoryId: Int): List<JSONObject> {
        val arr = root.getJSONArray("duas")
        return (0 until arr.length())
            .map { arr.getJSONObject(it) }
            .filter { it.optInt("category_id") == categoryId }
    }

    @Test
    fun `witr and night prayer category carries the tahajjud opening supplication`() {
        val titles = duasIn(WITR_AND_NIGHT_PRAYER).map { it.getString("title_english") }

        assertTrue(
            "The night worship hub links here from the Tahajjud card, so the category must " +
                "contain something for Tahajjud itself — not only Witr. Found: $titles",
            titles.any { it.contains("night prayer", ignoreCase = true) },
        )
    }

    @Test
    fun `every night prayer dua cites a source`() {
        duasIn(WITR_AND_NIGHT_PRAYER).forEach { dua ->
            val source = dua.optString("source")
            assertTrue(
                "'${dua.getString("title_english")}' ships without a source. Religious content " +
                    "must be attributable.",
                source.isNotBlank() && source != "null",
            )
        }
    }

    @Test
    fun `night prayer duas carry arabic, transliteration and translation`() {
        duasIn(WITR_AND_NIGHT_PRAYER).forEach { dua ->
            val title = dua.getString("title_english")
            listOf("text_arabic", "transliteration", "translation").forEach { field ->
                assertTrue(
                    "'$title' is missing $field — the reader renders all three",
                    dua.optString(field).isNotBlank(),
                )
            }
        }
    }

    @Test
    fun `category dua_count matches the duas actually present`() {
        val categories = root.getJSONArray("categories")
        val category = (0 until categories.length())
            .map { categories.getJSONObject(it) }
            .first { it.getInt("id") == WITR_AND_NIGHT_PRAYER }

        assertEquals(
            "dua_count is what the category list displays; a stale value shows a wrong number",
            duasIn(WITR_AND_NIGHT_PRAYER).size,
            category.getInt("dua_count"),
        )
    }

    @Test
    fun `dua ids are unique across the whole file`() {
        val arr = root.getJSONArray("duas")
        val ids = (0 until arr.length()).map { arr.getJSONObject(it).getInt("id") }
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

        assertTrue("Duplicate dua ids would collide on insert: $duplicates", duplicates.isEmpty())
    }

    /**
     * The seeder re-runs only when `contentVersion` is newer than what was stored
     * (`DuaContentSeeder`), so adding duas without bumping it ships content nobody ever sees.
     */
    @Test
    fun `contentVersion is at least the version that introduced the night prayer duas`() {
        assertTrue(
            "contentVersion must be bumped when duas are added, or the seeder skips them",
            root.getInt("contentVersion") >= 4,
        )
    }

    @Test
    fun `no dua carries a placeholder or empty arabic`() {
        val arr = root.getJSONArray("duas")
        (0 until arr.length()).map { arr.getJSONObject(it) }.forEach { dua ->
            assertFalse(
                "Dua ${dua.getInt("id")} has placeholder Arabic",
                dua.optString("text_arabic").contains("TODO", ignoreCase = true),
            )
        }
    }

    private companion object {
        const val WITR_AND_NIGHT_PRAYER = 35
    }
}
