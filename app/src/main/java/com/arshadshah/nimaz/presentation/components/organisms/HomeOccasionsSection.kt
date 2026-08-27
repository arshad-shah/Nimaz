package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.HomeEventCard
import com.arshadshah.nimaz.presentation.foundation.tokens.toOccasion

/** Tag on this section's carousel, so a test can scroll to today's occasion on compact Home. */
const val HomeOccasionsTestTag = "home_occasions"

/**
 * Today's Islamic occasions on Home — the Hijri-calendar events for today merged with any
 * pushed CELEBRATION announcement, as produced by `ObserveEventCardsUseCase`.
 *
 * This is a *section*, not a decoration: on 12 Rabi' al-Awwal it is the only thing on Home that
 * says "Mawlid". It went missing on phones between #528 (which replaced the compact layout's
 * event carousel with [HomeAlsoTodaySection] and dropped the celebration cards on the way) and
 * this component, so every occasion between those two points rendered on tablets only.
 *
 * Jumu'ah and the next worship reminder are deliberately **not** here: the compact layout already
 * carries both as rows inside [HomeAlsoTodaySection], and showing them again as cards is the same
 * double-render §12.6 of `docs/SUBSYSTEMS.md` splits the banner and card paths to avoid. The
 * tablet layout has no "Also today" section, so it prepends them to [occasionEventCards] itself.
 *
 * Renders nothing when there is no occasion today.
 */
@Composable
fun HomeOccasionsSection(
    cards: List<HomeEventCard>,
    onOpenRoute: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp,
) {
    if (cards.isEmpty()) return
    EventsCarousel(
        events = occasionEventCards(cards, onOpenRoute, onDismiss),
        modifier = modifier.testTag(HomeOccasionsTestTag),
        horizontalPadding = horizontalPadding,
    )
}

/**
 * Maps domain occasion cards onto the carousel's display model.
 *
 * Shared by both Home layouts on purpose. The compact layout lost its celebration cards in the
 * first place because this mapping lived inline in the tablet branch only, so there was nothing
 * to notice was missing from the other one.
 *
 * Direction A: compact card — name (eyebrow) + arabic + one body line + one action, matching the
 * Jumu'ah card's height.
 */
fun occasionEventCards(
    cards: List<HomeEventCard>,
    onOpenRoute: (String) -> Unit,
    onDismiss: () -> Unit,
): List<EventCardUi> = cards.map { c ->
    EventCardUi(
        occasion = c.event.toOccasion(),
        eyebrow = c.eyebrow,
        body = c.body,
        arabic = c.arabic,
        primaryAction = c.ctaLabel?.let { label ->
            c.route?.let { route -> EventAction(label) { onOpenRoute(route) } }
        },
        // Only a *pushed* card can be dismissed — a local calendar occasion has no announcement
        // id to remember the dismissal against, so it would come straight back.
        onDismiss = if (c.dismissable && c.announcementId != null) onDismiss else null,
    )
}
