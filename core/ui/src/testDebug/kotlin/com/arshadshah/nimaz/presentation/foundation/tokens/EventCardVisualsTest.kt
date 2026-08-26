package com.arshadshah.nimaz.presentation.foundation.tokens

import com.arshadshah.nimaz.presentation.components.organisms.EventOrnament
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The occasion → accent/icon/ornament table behind every event card.
 *
 * One `when` decides how Eid, Ramadan, Laylat al-Qadr, Arafah, Ashura, Mawlid, the Hijri new year
 * and Jumu'ah each look, and the cards it feeds are the app's most visible one-off surfaces — a
 * card that draws the wrong ornament is wrong on the one day of the year anybody sees it. The
 * mapping has no UI of its own, so nothing else in the codebase can catch an arm copy-pasted from
 * the one above it.
 *
 * These are not assertions that a colour is a particular hex: they are assertions that the
 * *distinctions the table exists to make* survive. Eid al-Fitr celebrates and the rest do not;
 * gold is structural, so Eid's icon tint and its well container are deliberately different values;
 * every occasion resolves to something.
 */
class EventCardVisualsTest {

    @Test
    fun `every occasion resolves to a complete visual treatment`() {
        EventOccasion.entries.forEach { occasion ->
            val visuals = eventCardVisualsFor(occasion)
            assertThat(visuals.accent).isNotNull()
            assertThat(visuals.containerAccent).isNotNull()
            assertThat(visuals.icon).isNotNull()
            assertThat(visuals.ornament).isNotNull()
        }
    }

    @Test
    fun `only eid al-fitr plays the celebration burst`() {
        // The burst is an animation, not a decoration — firing it on Ashura, a day of mourning,
        // is the kind of mistake a copy-pasted arm makes and no compiler catches.
        val bursting = EventOccasion.entries.filter {
            eventCardVisualsFor(it).ornament is EventOrnament.Burst
        }

        assertThat(bursting).containsExactly(EventOccasion.EID_AL_FITR)
    }

    @Test
    fun `eid al-fitr tints its icon darker than the well behind it`() {
        // Gold is structural: it is legible as a container and not as text on white, so the icon
        // takes GoldDark while the well takes Gold500. A single value for both is either an
        // unreadable icon or a washed-out well.
        val eid = eventCardVisualsFor(EventOccasion.EID_AL_FITR)

        assertThat(eid.accent).isNotEqualTo(eid.containerAccent)
    }

    @Test
    fun `every other occasion uses one accent for both roles`() {
        // The corollary. Eid is the documented exception, and a second exception appearing
        // silently means somebody duplicated the gold rule where it does not apply.
        EventOccasion.entries
            .filter { it != EventOccasion.EID_AL_FITR }
            .forEach { occasion ->
                val visuals = eventCardVisualsFor(occasion)
                assertThat(visuals.accent).isEqualTo(visuals.containerAccent)
            }
    }

    @Test
    fun `the three patterned occasions each get their own pattern`() {
        // Eid al-Adha, Ramadan and Laylat al-Qadr are the only ones with a background pattern, and
        // they must not share one — the pattern is how the card is recognised at a glance.
        val patterns = EventOccasion.entries
            .mapNotNull { eventCardVisualsFor(it).ornament as? EventOrnament.Pattern }
            .map { it.style }

        assertThat(patterns).hasSize(3)
        assertThat(patterns.toSet()).hasSize(3)
    }

    @Test
    fun `the remaining occasions fall back to the divider`() {
        val divided = EventOccasion.entries.filter {
            eventCardVisualsFor(it).ornament === EventOrnament.Divider
        }

        assertThat(divided).containsExactly(
            EventOccasion.ARAFAH,
            EventOccasion.ASHURA,
            EventOccasion.MAWLID,
            EventOccasion.HIJRI_NEW_YEAR,
            EventOccasion.JUMUAH,
            EventOccasion.GENERIC,
        )
    }

    @Test
    fun `a generic occasion is not silently the same as a named one`() {
        // GENERIC is the fallback for anything the calendar does not recognise. If it collided
        // with Jumu'ah — the other Mosque-icon occasion — an unknown event would masquerade as
        // the Friday prayer.
        assertThat(eventCardVisualsFor(EventOccasion.GENERIC).icon)
            .isNotEqualTo(eventCardVisualsFor(EventOccasion.JUMUAH).icon)
    }
}
