package com.arshadshah.nimaz.core.navigation

import com.arshadshah.nimaz.domain.model.WorshipReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Every worship card must lead somewhere, and somewhere *useful*.
 *
 * A card that navigates to an unhelpful screen is worse than one that does nothing: the misfire
 * teaches you to stop tapping, which costs you the destinations that are good. So this pins the
 * whole table rather than spot-checking it — including the `when` being exhaustive, so adding a
 * twelfth reminder type fails compilation rather than silently shipping an inert card.
 */
class WorshipDestinationsTest {

    @Test
    fun `adhkar reminders open their own dua category`() {
        assertEquals(
            Route.DuaCategory("1"),
            worshipCardDestination(WorshipReminderType.ADHKAR_MORNING),
        )
        assertEquals(
            Route.DuaCategory("2"),
            worshipCardDestination(WorshipReminderType.ADHKAR_EVENING),
        )
    }

    @Test
    fun `night worship reminders open the night worship hub`() {
        assertEquals(Route.NightWorship, worshipCardDestination(WorshipReminderType.TAHAJJUD))
        assertEquals(Route.NightWorship, worshipCardDestination(WorshipReminderType.WITR))
    }

    @Test
    fun `fasting reminders open the fast tracker where the fast is logged`() {
        listOf(
            WorshipReminderType.SUHOOR,
            WorshipReminderType.IFTAR,
            WorshipReminderType.MONDAY_THURSDAY_FAST,
            WorshipReminderType.WHITE_DAYS_FAST,
            WorshipReminderType.ARAFAH_ASHURA_FAST,
        ).forEach { type ->
            assertEquals(
                "$type should open the fast tracker",
                Route.FastingTracker,
                worshipCardDestination(type),
            )
        }
    }

    @Test
    fun `ramadan-only reminders open the fasting and ramadan duas`() {
        assertEquals(
            Route.DuaCategory("37"),
            worshipCardDestination(WorshipReminderType.TARAWEEH),
        )
        assertEquals(
            Route.DuaCategory("37"),
            worshipCardDestination(WorshipReminderType.LAYLATUL_QADR),
        )
    }

    /**
     * The guard that matters most: no type may be forgotten. `worshipCardDestination` is an
     * exhaustive `when` over the enum, so a new reminder type is a compile error — this asserts
     * the runtime consequence too, in case the `when` ever grows an `else`.
     */
    @Test
    fun `every reminder type has a destination`() {
        WorshipReminderType.entries.forEach { type ->
            assertNotNull("$type has no destination", worshipCardDestination(type))
        }
    }
}
