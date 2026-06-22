package com.arshadshah.nimaz.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Verifies the *shipped* database. Unlike the DAO tests (which use a hermetic
 * in-memory DB), this injects the real [NimazDatabase] built by `DatabaseModule`
 * via `createFromAsset("database/nimaz_prepopulated.db", …)`. It confirms the
 * prepackaged content actually seeds — the scripture, names, and reference tables a
 * user sees on first launch — and that the production Room wiring opens at the
 * current schema version without a migration crash.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DatabaseAssetTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var db: NimazDatabase

    @Before
    fun setup() = hiltRule.inject()

    @Test
    fun quran_isFullySeeded() = runTest {
        // The Quran always has exactly 114 surahs.
        assertThat(db.quranDao().getAllSurahs().first()).hasSize(114)
    }

    @Test
    fun asmaUlHusna_has99Names() = runTest {
        assertThat(db.asmaUlHusnaDao().getAllNames().first()).hasSize(99)
    }

    @Test
    fun referenceContent_isPresent() = runTest {
        assertThat(db.hadithDao().getAllBooks().first()).isNotEmpty()
        assertThat(db.duaDao().getAllCategories().first()).isNotEmpty()
        assertThat(db.prophetDao().getAllProphets().first()).isNotEmpty()
        assertThat(db.islamicEventDao().getAllEvents().first()).isNotEmpty()
        assertThat(db.qaidaDao().lessonCount()).isGreaterThan(0)
    }
}
