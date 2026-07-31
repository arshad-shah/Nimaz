package com.arshadshah.nimaz.db

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What this device's SQLite can actually do (#330, nimaz-data#7).
 *
 * The shipped search index is FTS4 rather than the FTS5 the issue proposed, and that
 * choice needs evidence rather than a docs page. FTS5 is an *optional compile-time
 * module*: `SQLITE_ENABLE_FTS5` has to be set when SQLite is built, AOSP does not set
 * it, and `CREATE VIRTUAL TABLE … USING fts5` therefore fails with **"no such module:
 * fts5"** on a stock Android. It is the reason Room shipped `@Fts3`/`@Fts4` and nothing
 * else for the whole of its 2.x life, and the reason apps that want FTS5 bundle their
 * own SQLite.
 *
 * The cost of being wrong is asymmetric and worth being explicit about. Choosing FTS4
 * when FTS5 was available means a slightly larger index. Choosing FTS5 when it is not
 * means an artifact whose search table cannot be opened at all — a crash on the first
 * search, on every affected phone, discovered in production.
 *
 * So this test records the answer per device rather than asserting a belief:
 *
 *  - [fts4IsAvailable] **must** pass. It is what the artifact ships, and a failure here
 *    means search is broken on this device.
 *  - [reportFts5Availability] never fails. Run it across the device matrix at `minSdk`;
 *    if FTS5 turns out to be present everywhere that matters, flipping
 *    `search.flavour` in the data repo's `console.yaml` is a one-line change, and this
 *    is the evidence that would justify it.
 */
@RunWith(AndroidJUnit4::class)
class SqliteFtsSupportTest {

    @Test
    fun fts4IsAvailable() {
        assertThat(createVirtualTable("fts4")).isNull()
    }

    @Test
    fun reportFts5Availability() {
        val failure = createVirtualTable("fts5")
        println(
            "SQLite FTS support on API ${Build.VERSION.SDK_INT} " +
                "(${Build.MANUFACTURER} ${Build.MODEL}): " +
                "fts4=available, fts5=${failure?.let { "UNAVAILABLE ($it)" } ?: "available"}"
        )
    }

    /** Returns null when the module exists, or the failure message when it does not. */
    private fun createVirtualTable(module: String): String? {
        val db = SQLiteDatabase.create(null)
        return try {
            db.execSQL("CREATE VIRTUAL TABLE probe USING $module(body)")
            null
        } catch (error: SQLiteException) {
            error.message
        } finally {
            db.close()
        }
    }
}
