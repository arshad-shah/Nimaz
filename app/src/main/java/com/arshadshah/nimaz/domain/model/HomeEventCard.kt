package com.arshadshah.nimaz.domain.model

/** A resolved occasion card for the Home carousel — domain-only (no UI/lambda types). */
data class HomeEventCard(
    val event: CelebrationEvent,
    val eyebrow: String,
    val headline: String,
    val body: String,
    val arabic: String? = null,
    val transliteration: String? = null,
    val proofRef: String? = null,
    val proofText: String? = null,
    val ctaLabel: String? = null,
    val route: String? = null,
    val cta2Label: String? = null,
    val route2: String? = null,
    val announcementId: String? = null,
    val dismissable: Boolean = false,
    val priority: Int = 0,
)
