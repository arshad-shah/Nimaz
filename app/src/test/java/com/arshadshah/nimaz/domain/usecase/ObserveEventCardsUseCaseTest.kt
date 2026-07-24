package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.model.HomeEventCard
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ObserveEventCardsUseCaseTest {

    private fun localOf(vararg cards: HomeEventCard) =
        ObserveLocalEventsFake(cards.toList())

    @Test
    fun `pushed celebration matching local is merged, pushed fields win`() = runBlocking {
        val local = localOf(
            HomeEventCard(CelebrationEvent.EID_AL_FITR, "Eid al-Fitr", "Eid al-Fitr", "local body", priority = 10)
        )
        val pushed = Announcement(
            id = "p1", type = AnnouncementType.CELEBRATION, title = "Eid Mubarak", body = "pushed body",
            event = CelebrationEvent.EID_AL_FITR, arabic = "تقبل الله",
        )
        val useCase = ObserveEventCardsUseCase(local::invoke, observe = { flowOf(pushed) })
        val cards = useCase().first()
        assertThat(cards).hasSize(1)
        assertThat(cards[0].body).isEqualTo("pushed body")     // pushed wins
        assertThat(cards[0].arabic).isEqualTo("تقبل الله")     // pushed fills
        assertThat(cards[0].announcementId).isEqualTo("p1")    // dismissable pushed identity
    }

    @Test
    fun `non-matching pushed celebration is added alongside local`() {
        runBlocking {
            val local = localOf(
                HomeEventCard(CelebrationEvent.ARAFAH, "Arafah", "Arafah", "b", priority = 5)
            )
            val pushed = Announcement(
                id = "p2", type = AnnouncementType.CELEBRATION, title = "Special", body = "b2",
                event = CelebrationEvent.GENERIC,
            )
            val cards = ObserveEventCardsUseCase(local::invoke, observe = { flowOf(pushed) })().first()
            assertThat(cards.map { it.event })
                .containsExactly(CelebrationEvent.GENERIC, CelebrationEvent.ARAFAH)
        }
    }

    @Test
    fun `non-celebration announcement is ignored`() {
        runBlocking {
            val local = localOf(HomeEventCard(CelebrationEvent.ASHURA, "Ashura", "Ashura", "b"))
            val feature = Announcement(id = "f", type = AnnouncementType.FEATURE, title = "t", body = "b")
            val cards = ObserveEventCardsUseCase(local::invoke, observe = { flowOf(feature) })().first()
            assertThat(cards.map { it.event }).containsExactly(CelebrationEvent.ASHURA)
        }
    }

    @Test
    fun `caps at two cards`() = runBlocking {
        val local = localOf(
            HomeEventCard(CelebrationEvent.ARAFAH, "a", "a", "b", priority = 3),
            HomeEventCard(CelebrationEvent.ASHURA, "c", "c", "b", priority = 2),
            HomeEventCard(CelebrationEvent.MAWLID, "m", "m", "b", priority = 1),
        )
        val cards = ObserveEventCardsUseCase(local::invoke, observe = { flowOf(null) })().first()
        assertThat(cards).hasSize(2)
    }
}

// Fake local use case returning a fixed list. ObserveLocalEventsUseCase is a class, so this
// helper wraps a fixed flow; the merge use case accepts the local source as a function.
private class ObserveLocalEventsFake(private val cards: List<HomeEventCard>) {
    operator fun invoke() = flowOf(cards)
}
