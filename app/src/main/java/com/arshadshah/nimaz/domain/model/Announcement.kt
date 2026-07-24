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
    /** Occasion behind a CELEBRATION announcement; null for other types. */
    val event: CelebrationEvent? = null,
    val arabic: String? = null,
    val transliteration: String? = null,
    val proofRef: String? = null,
    val proofText: String? = null,
    /** Secondary CTA button label; the banner shows no second CTA when absent. */
    val cta2Label: String? = null,
    /** Route key (resolved against an allowlist) or an https:// URL for the secondary CTA. */
    val route2: String? = null,
    /** Suppress the banner before this instant (epoch millis, UTC). */
    val startsAtMillis: Long? = null,
) {
    /** True when this announcement may render for [versionCode] at [nowMillis]. */
    fun isActiveFor(versionCode: Int, nowMillis: Long): Boolean =
        (startsAtMillis == null || nowMillis >= startsAtMillis) &&
                (expiresAtMillis == null || nowMillis < expiresAtMillis) &&
                (minVersionCode == null || versionCode >= minVersionCode) &&
                (maxVersionCode == null || versionCode <= maxVersionCode)
}

/** Announcement category — selects the banner's icon and accent. */
enum class AnnouncementType(val key: String) {
    FEATURE("feature"),
    PRIVACY("privacy"),
    TOS("tos"),
    CHANGELOG("changelog"),
    CELEBRATION("celebration");

    companion object {
        fun fromKey(key: String?): AnnouncementType? =
            entries.firstOrNull { it.key == key?.trim()?.lowercase() }
    }
}

/** Occasion behind a CELEBRATION announcement. Keys match IslamicEvents.events ids. */
enum class CelebrationEvent(val key: String) {
    EID_AL_FITR("eid_al_fitr"),
    EID_AL_ADHA("eid_al_adha"),
    RAMADAN_START("ramadan_start"),
    RAMADAN_END("ramadan_end"),
    LAYLAT_AL_QADR("laylat_al_qadr"),
    ARAFAH("day_of_arafah"),
    ASHURA("ashura"),
    MAWLID("mawlid"),
    HIJRI_NEW_YEAR("islamic_new_year"),
    JUMUAH("jumuah"),
    GENERIC("generic");

    companion object {
        fun fromKey(key: String?): CelebrationEvent =
            entries.firstOrNull { it.key == key?.trim()?.lowercase() } ?: GENERIC
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
