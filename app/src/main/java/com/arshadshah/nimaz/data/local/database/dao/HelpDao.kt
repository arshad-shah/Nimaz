package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.database.entity.HelpItemEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStepEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpTopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HelpDao {

    // ---- reads (reactive) ----
    @Query("SELECT * FROM help_topic ORDER BY display_order ASC")
    fun getTopics(): Flow<List<HelpTopicEntity>>

    @Query("SELECT * FROM help_item WHERE topic_id = :topicId ORDER BY display_order ASC")
    fun getItemsForTopic(topicId: String): Flow<List<HelpItemEntity>>

    @Query("SELECT * FROM help_item ORDER BY display_order ASC")
    fun getAllItems(): Flow<List<HelpItemEntity>>

    @Query("SELECT * FROM help_item WHERE id = :itemId")
    fun getItem(itemId: String): Flow<HelpItemEntity?>

    @Query("SELECT * FROM help_step WHERE item_id = :itemId ORDER BY display_order ASC")
    fun getStepsForItem(itemId: String): Flow<List<HelpStepEntity>>

    @Query("SELECT * FROM help_string WHERE owner_type = :ownerType AND owner_id IN (:ownerIds)")
    fun getStringsFor(ownerType: String, ownerIds: List<String>): Flow<List<HelpStringEntity>>

    // Search: match localized text in the requested language; caller adds EN fallback rows.
    @Query("SELECT * FROM help_string WHERE lang_code = :lang AND value LIKE '%' || :query || '%'")
    fun searchStrings(lang: String, query: String): Flow<List<HelpStringEntity>>

    @Query("SELECT COUNT(*) FROM help_topic")
    suspend fun topicCount(): Int

}
