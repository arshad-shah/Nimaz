package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "help_topic")
data class HelpTopicEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    @ColumnInfo(name = "color_key") val colorKey: String
)

@Entity(
    tableName = "help_item",
    indices = [Index(value = ["topic_id"])]
)
data class HelpItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "topic_id") val topicId: String,
    val type: String, // "QUESTION" | "GUIDE"
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "icon_key") val iconKey: String?,
    @ColumnInfo(name = "estimated_minutes") val estimatedMinutes: Int?
)

@Entity(
    tableName = "help_step",
    indices = [Index(value = ["item_id"])]
)
data class HelpStepEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "deeplink_route") val deeplinkRoute: String?,
    @ColumnInfo(name = "path_labels") val pathLabels: String? // JSON array string
)

@Entity(
    tableName = "help_string",
    primaryKeys = ["owner_type", "owner_id", "field_key", "lang_code"],
    indices = [
        Index(value = ["owner_type", "owner_id", "lang_code"]),
        Index(value = ["lang_code"])
    ]
)
data class HelpStringEntity(
    @ColumnInfo(name = "owner_type") val ownerType: String, // "TOPIC" | "ITEM" | "STEP"
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "field_key") val fieldKey: String,   // "title","subtitle","question","answer","body"
    @ColumnInfo(name = "lang_code") val langCode: String,
    val value: String
)
