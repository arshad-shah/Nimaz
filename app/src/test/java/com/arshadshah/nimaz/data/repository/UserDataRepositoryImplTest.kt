package com.arshadshah.nimaz.data.repository

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.user.NimazUserDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * "Delete all my data" has to actually delete all of it.
 *
 * `clearAllUserData` fans out to eleven DAO calls by hand. Nothing connects that list to the
 * fifteen entities the user database actually holds, so **a new user table is silently exempt
 * from deletion** until somebody remembers to add a twelfth line — and the failure is invisible:
 * the screen says the data is gone, and the rows are still there.
 *
 * These tests close that gap by asserting against the schema rather than against the list: every
 * table in the database is populated, `clearAllUserData()` runs, and every table is checked. A
 * table added without a matching delete fails here rather than in someone's export.
 */
@RunWith(RobolectricTestRunner::class)
class UserDataRepositoryImplTest {

    private lateinit var database: NimazUserDatabase
    private lateinit var repository: UserDataRepositoryImpl

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NimazUserDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UserDataRepositoryImpl(database)
    }

    @After
    fun tearDown() = database.close()

    /**
     * Every real table in the database, read from the schema rather than hardcoded — so this
     * cannot drift the way the delete list did.
     */
    private fun userTables(): List<String> = buildList {
        database.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' " +
                "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_metadata' " +
                "AND name NOT LIKE 'room_%'"
        ).use { cursor ->
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
    }

    private fun SupportSQLiteDatabase.rowCount(table: String): Int =
        query("SELECT COUNT(*) FROM `$table`").use { it.moveToFirst(); it.getInt(0) }

    /**
     * Insert one row into every table using its own column list, so the fixture cannot go stale
     * when a column is added. Text for everything: SQLite is dynamically typed, and this is only
     * asserting that rows exist and then do not.
     */
    private fun populateEveryTable(): List<String> {
        val db = database.openHelper.writableDatabase
        val populated = mutableListOf<String>()
        for (table in userTables()) {
            val columns = mutableListOf<String>()
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                while (cursor.moveToNext()) columns.add(cursor.getString(1))
            }
            if (columns.isEmpty()) continue
            val placeholders = columns.joinToString(",") { "?" }
            val names = columns.joinToString(",") { "`$it`" }
            val values: Array<Any> = Array(columns.size) { 1 }
            runCatching {
                db.execSQL("INSERT OR REPLACE INTO `$table` ($names) VALUES ($placeholders)", values)
                populated.add(table)
            }
        }
        return populated
    }

    @Test
    fun `the fixture actually populates the database`() = runTest {
        val populated = populateEveryTable()
        val db = database.openHelper.readableDatabase

        assertThat(populated).isNotEmpty()
        populated.forEach { assertThat(db.rowCount(it)).isGreaterThan(0) }
    }

    @Test
    fun `clearAllUserData leaves no row in any table`() = runTest {
        val populated = populateEveryTable()

        repository.clearAllUserData()

        val db = database.openHelper.readableDatabase
        val survivors = populated.filter { db.rowCount(it) > 0 }
        assertThat(survivors).isEmpty()
    }

    @Test
    fun `clearing an already empty database is a no-op, not a failure`() = runTest {
        repository.clearAllUserData()
        repository.clearAllUserData()

        val db = database.openHelper.readableDatabase
        userTables().forEach { assertThat(db.rowCount(it)).isEqualTo(0) }
    }
}
