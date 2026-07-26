package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * A single event card's display data. Jumu'ah carries the three jumuah_* fields and
 * is rendered via [JumuahCard]; all other occasions render via [EventCard].
 */
data class EventCardUi(
    val occasion: EventOccasion,
    val eyebrow: String,
    val body: String,
    val arabic: String? = null,
    val primaryAction: EventAction? = null,
    val onDismiss: (() -> Unit)? = null,
    /** Today's Dhuhr on Fridays; the card derives "passed" and its countdown from it. */
    val jumuahAt: Instant? = null,
    /** When set, this page renders the "Next Worship" card via [WorshipEventCard]. */
    val worship: WorshipCardUi? = null,
)

// Direction A: every event card is the compact Jumu'ah shape — icon well + name (eyebrow) +
// arabic + one body line + one action (or the Jumu'ah countdown). Home cards do not render
// headline/transliteration/proof/second-action, so this height matches Jumu'ah's content and
// leaves no empty space. Uniform for all cards (carousel constraint); tune here if needed.
private val EventCardPageHeight = 170.dp

/**
 * Horizontal carousel of occasion cards, reusing [NimazCarousel] (edge-peek + dots,
 * swipe-only). One fixed [pageHeight] for every page. Renders nothing when empty.
 */
@Composable
fun EventsCarousel(
    events: List<EventCardUi>,
    modifier: Modifier = Modifier,
    pageHeight: Dp = EventCardPageHeight,
    horizontalPadding: Dp = 20.dp,
) {
    if (events.isEmpty()) return
    NimazCarousel(
        count = events.size,
        modifier = modifier,
        pageHeight = pageHeight,
        horizontalPadding = horizontalPadding,
        pageSpacing = 12.dp,
    ) { pageIndex ->
        val e = events[pageIndex]
        val worship = e.worship
        if (worship != null) {
            // The card derives its own countdown/proximity from the instants in `worship`
            // via the shared ticker — no pre-formatted strings, no per-minute VM refresh.
            // onAction is not yet wired to navigation (tracked as follow-up), so the CTA
            // stays hidden until a handler is threaded through.
            WorshipEventCard(
                card = worship,
                fillHeight = true,
            )
        } else if (e.occasion == EventOccasion.JUMUAH) {
            JumuahCard(
                jumuahAt = e.jumuahAt,
                fillHeight = true,
            )
        } else {
            val v = eventCardVisualsFor(e.occasion)
            EventCard(
                accent = v.accent,
                containerAccent = v.containerAccent,
                icon = v.icon,
                ornament = v.ornament,
                eyebrow = e.eyebrow,
                arabic = e.arabic,
                body = e.body,
                primaryAction = e.primaryAction,
                onDismiss = e.onDismiss,
                fillHeight = true,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 300)
@Composable
private fun EventsCarousel_Preview() {
    NimazTheme {
        EventsCarousel(
            events = listOf(
                EventCardUi(
                    occasion = EventOccasion.EID_AL_FITR,
                    eyebrow = "Eid al-Fitr",
                    arabic = "عيد مبارك",
                    body = "Thirty days behind you. May every one be accepted.",
                    primaryAction = EventAction("Eid prayer time") {},
                ),
                EventCardUi(
                    occasion = EventOccasion.JUMUAH,
                    eyebrow = "Jumu'ah",
                    body = "\"The best day on which the sun rises is Friday.\"",
                    jumuahAt = Clock.System.now() + 3.hours,
                ),
            ),
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
