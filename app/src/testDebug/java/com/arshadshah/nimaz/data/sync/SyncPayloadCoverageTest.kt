package com.arshadshah.nimaz.data.sync

import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Modifier
import org.junit.Test

/**
 * Guards against the sync screen silently lagging behind new features.
 *
 * [SyncPayload.categories] is the single source of truth the sync UI renders
 * from. Every data-carrying field on [SyncPayload] must be represented there.
 * If someone adds a new field to [SyncPayload] (e.g. a new syncable feature)
 * without adding a matching [SyncCategory], this test fails — forcing the sync
 * screen to stay in lockstep with what is actually synced.
 *
 * Uses Java reflection (no kotlin-reflect dependency).
 */
class SyncPayloadCoverageTest {

    /** Bookkeeping fields that are not user-facing syncable categories. */
    private val metadataFields = setOf("exportedAt", "appVersion")

    @Test
    fun `every SyncPayload data field is represented in categories`() {
        val dataFields = SyncPayload::class.java.declaredFields
            .filter { field ->
                !field.isSynthetic &&
                    !Modifier.isStatic(field.modifiers) &&
                    !field.name.contains('$') &&
                    field.name !in metadataFields
            }
            .map { it.name }
            .toSet()

        val categoryKeys = SyncPayload().categories().map { it.key }.toSet()

        assertThat(categoryKeys).isEqualTo(dataFields)
    }

    @Test
    fun `category keys are unique`() {
        val keys = SyncPayload().categories().map { it.key }
        assertThat(keys).containsNoDuplicates()
    }
}
