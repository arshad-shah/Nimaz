package com.arshadshah.nimaz.domain.model

import com.arshadshah.nimaz.core.navigation.Route

/**
 * An engagement announcement delivered via FCM (topic broadcast from the
 * Firebase console). Rendered as a dismissable banner on the Home screen;
 * the OS tray shows the composer's notification title/body when the app is
 * backgrounded. See docs/SUBSYSTEMS.md — "Engagement announcements (FCM)".
 */
data class Announcement(
    /** Stable unique id from the payload — drives dedup and permanent dismissal. */
    val id: String,
    val type: AnnouncementType,
    val title: String,
    val body: String,
    /** CTA button label; the banner shows no CTA when absent. */
    val ctaLabel: String? = null,
    /** Route key (resolved against an allowlist) or an https:// URL. */
    val route: String? = null,
    /** Suppress the banner below this installed versionCode. */
    val minVersionCode: Int? = null,
    /** Suppress the banner above this installed versionCode. */
    val maxVersionCode: Int? = null,
    /** Suppress the banner after this instant (epoch millis, UTC). */
    val expiresAtMillis: Long? = null,
    val dismissable: Boolean = true,
) {
    /** True when this announcement may render for [versionCode] at [nowMillis]. */
    fun isActiveFor(versionCode: Int, nowMillis: Long): Boolean =
        (expiresAtMillis == null || nowMillis < expiresAtMillis) &&
                (minVersionCode == null || versionCode >= minVersionCode) &&
                (maxVersionCode == null || versionCode <= maxVersionCode)
}

/** Announcement category — selects the banner's icon and accent. */
enum class AnnouncementType(val key: String) {
    FEATURE("feature"),
    PRIVACY("privacy"),
    TOS("tos"),
    CHANGELOG("changelog");

    companion object {
        fun fromKey(key: String?): AnnouncementType? =
            entries.firstOrNull { it.key == key?.trim()?.lowercase() }
    }
}

/** Validated action behind an announcement's CTA / notification tap. */
sealed interface AnnouncementAction {
    /** Open an https:// URL in the browser. */
    data class OpenUrl(val url: String) : AnnouncementAction

    /** Navigate to an allowlisted in-app feature (see announcementRoute). */
    data class NavigateToFeature(val routeKey: String, val route: Route) : AnnouncementAction

    /** Unknown or missing route — the banner hides its CTA. */
    data object None : AnnouncementAction
}
