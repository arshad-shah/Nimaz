package com.arshadshah.nimaz.data.announcement

import android.os.Bundle
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps an FCM custom-data payload (or the intent extras of a tapped tray
 * notification, which carry the same key/value pairs) to an [Announcement].
 *
 * Returns null — never throws — when a required field is missing/blank or any
 * present field is malformed, so a bad console send is silently ignored.
 */
@Singleton
class AnnouncementPayloadMapper @Inject constructor() {

    fun fromPayload(data: Map<String, String>): Announcement? {
        val id = data[KEY_ID]?.trim().orEmpty().ifEmpty { return null }
        val type = AnnouncementType.fromKey(data[KEY_TYPE]) ?: return null
        val title = data[KEY_TITLE]?.trim().orEmpty().ifEmpty { return null }
        val body = data[KEY_BODY]?.trim().orEmpty().ifEmpty { return null }

        val minVersionCode =
            data[KEY_MIN_VERSION_CODE]?.let { it.trim().toIntOrNull() ?: return null }
        val maxVersionCode =
            data[KEY_MAX_VERSION_CODE]?.let { it.trim().toIntOrNull() ?: return null }
        val expiresAtMillis = data[KEY_EXPIRES_AT]?.let { raw ->
            runCatching { Instant.parse(raw.trim()).toEpochMilli() }.getOrNull() ?: return null
        }
        val dismissable = data[KEY_DISMISSABLE]?.let {
            it.trim().lowercase().toBooleanStrictOrNull() ?: return null
        } ?: true

        return Announcement(
            id = id,
            type = type,
            title = title,
            body = body,
            ctaLabel = data[KEY_CTA_LABEL]?.trim()?.ifEmpty { null },
            route = data[KEY_ROUTE]?.trim()?.ifEmpty { null },
            minVersionCode = minVersionCode,
            maxVersionCode = maxVersionCode,
            expiresAtMillis = expiresAtMillis,
            dismissable = dismissable,
        )
    }

    /**
     * Maps the launcher intent's extras to an [Announcement]. When the app is
     * backgrounded/killed, the OS posts the tray notification itself and copies
     * the message's custom data onto the tap intent as string extras.
     */
    fun fromIntentExtras(extras: Bundle?): Announcement? {
        extras ?: return null
        val data = buildMap {
            PAYLOAD_KEYS.forEach { key -> extras.getString(key)?.let { put(key, it) } }
        }
        return fromPayload(data)
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_TYPE = "type"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_CTA_LABEL = "cta_label"
        const val KEY_ROUTE = "route"
        const val KEY_MIN_VERSION_CODE = "min_version_code"
        const val KEY_MAX_VERSION_CODE = "max_version_code"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_DISMISSABLE = "dismissable"

        val PAYLOAD_KEYS = listOf(
            KEY_ID, KEY_TYPE, KEY_TITLE, KEY_BODY, KEY_CTA_LABEL, KEY_ROUTE,
            KEY_MIN_VERSION_CODE, KEY_MAX_VERSION_CODE, KEY_EXPIRES_AT, KEY_DISMISSABLE,
        )
    }
}
