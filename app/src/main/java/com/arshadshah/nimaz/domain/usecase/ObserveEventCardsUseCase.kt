package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.model.HomeEventCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Merges local calendar occasion cards with a pushed CELEBRATION announcement.
 * Pushed fields win on a same-event match; otherwise both render. Capped at two.
 */
class ObserveEventCardsUseCase(
    private val local: () -> Flow<List<HomeEventCard>>,
    private val observe: () -> Flow<Announcement?>,
) {
    operator fun invoke(): Flow<List<HomeEventCard>> =
        combine(local(), observe()) { localCards, announcement ->
            val pushed = announcement
                ?.takeIf { it.type == AnnouncementType.CELEBRATION }
                ?.let(::toCard)

            val merged: List<HomeEventCard> = when {
                pushed == null -> localCards
                localCards.any { it.event == pushed.event } ->
                    localCards.map { if (it.event == pushed.event) mergePushedOver(it, pushed) else it }
                else -> listOf(pushed) + localCards
            }
            merged
                .sortedWith(compareByDescending<HomeEventCard> { it.priority }
                    .thenByDescending { it.announcementId != null }) // pushed before local on ties
                .take(2)
        }

    private fun toCard(a: Announcement) = HomeEventCard(
        event = a.event ?: CelebrationEvent.GENERIC,
        eyebrow = a.title,
        headline = a.title,
        body = a.body,
        arabic = a.arabic,
        transliteration = a.transliteration,
        proofRef = a.proofRef,
        proofText = a.proofText,
        ctaLabel = a.ctaLabel,
        route = a.route,
        cta2Label = a.cta2Label,
        route2 = a.route2,
        announcementId = a.id,
        dismissable = a.dismissable,
        priority = 100, // pushed outranks local by default
    )

    /** Pushed fields win; local fills only where pushed is null/blank. */
    private fun mergePushedOver(local: HomeEventCard, pushed: HomeEventCard) = pushed.copy(
        eyebrow = pushed.eyebrow.ifBlank { local.eyebrow },
        headline = pushed.headline.ifBlank { local.headline },
        body = pushed.body.ifBlank { local.body },
        arabic = pushed.arabic ?: local.arabic,
        transliteration = pushed.transliteration ?: local.transliteration,
        proofRef = pushed.proofRef ?: local.proofRef,
        proofText = pushed.proofText ?: local.proofText,
    )
}
