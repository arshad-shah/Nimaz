package com.arshadshah.nimaz.data.local.search

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery

/**
 * Reads the FTS index the content artifact ships (#330, nimaz-data#7).
 *
 * Everything here is `@RawQuery` because `search_index`, `search_docs` and
 * `search_meta` are **not Room entities and must never become ones**. Room's
 * `createFromAsset` copies the artifact as it is and passes over tables it does not
 * declare, so the index arrives already built, changes no identity hash, and costs
 * nothing at first launch. Declaring it as an entity would put it back in Room's
 * schema — and building it on the device over 200,000 rows is exactly what sank the
 * previous attempt at this.
 *
 * It also means the schema here can move without a Room migration: a new artifact
 * with a different index is just a different file.
 */
@Dao
interface SearchIndexDao {

    @RawQuery
    suspend fun rawRefs(query: SimpleSQLiteQuery): List<SearchIndexRefRow>

    @RawQuery
    suspend fun rawMeta(query: SimpleSQLiteQuery): List<SearchIndexMetaRow>
}

/** One hit: what kind of thing matched, and the key its own DAO looks it up by. */
data class SearchIndexRefRow(
    val kind: String,
    val ref: String,
    val source: String?,
)

data class SearchIndexMetaRow(
    val key: String,
    val value: String,
)
