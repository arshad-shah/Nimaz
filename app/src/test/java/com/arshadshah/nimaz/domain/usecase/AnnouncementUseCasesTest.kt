package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.navigation.announcementRoute
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementAction
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.domain.repository.AnnouncementRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AnnouncementUseCasesTest {

    private val announcement = Announcement(
        id = "a-1",
        type = AnnouncementType.FEATURE,
        title = "t",
        body = "b",
        minVersionCode = 100,
        maxVersionCode = 200,
        expiresAtMillis = 1_000L,
    )

    private class FakeRepository : AnnouncementRepository {
        val current = MutableStateFlow<Announcement?>(null)
        val dismissed = mutableSetOf<String>()

        override fun observeCurrentAnnouncement(): Flow<Announcement?> = current

        override suspend fun setAnnouncement(announcement: Announcement) {
            current.value = announcement
        }

        override suspend fun dismiss(id: String) {
            dismissed += id
            if (current.value?.id == id) current.value = null
        }
    }

    private fun useCase(
        repository: AnnouncementRepository,
        versionCode: Int,
        now: Long,
    ) = ObserveActiveAnnouncementUseCase(repository, versionCode, { now })

    @Test
    fun `active announcement inside window is emitted`() = runTest {
        val repo = FakeRepository().apply { current.value = announcement }

        val result = useCase(repo, versionCode = 150, now = 500L).invoke().first()

        assertThat(result).isEqualTo(announcement)
    }

    @Test
    fun `expired announcement is suppressed`() = runTest {
        val repo = FakeRepository().apply { current.value = announcement }

        val result = useCase(repo, versionCode = 150, now = 1_000L).invoke().first()

        assertThat(result).isNull()
    }

    @Test
    fun `below min version is suppressed`() = runTest {
        val repo = FakeRepository().apply { current.value = announcement }

        val result = useCase(repo, versionCode = 99, now = 500L).invoke().first()

        assertThat(result).isNull()
    }

    @Test
    fun `above max version is suppressed`() = runTest {
        val repo = FakeRepository().apply { current.value = announcement }

        val result = useCase(repo, versionCode = 201, now = 500L).invoke().first()

        assertThat(result).isNull()
    }

    @Test
    fun `no bounds means always active`() = runTest {
        val unbounded = announcement.copy(
            minVersionCode = null,
            maxVersionCode = null,
            expiresAtMillis = null,
        )
        val repo = FakeRepository().apply { current.value = unbounded }

        val result = useCase(repo, versionCode = 1, now = Long.MAX_VALUE - 1).invoke().first()

        assertThat(result).isEqualTo(unbounded)
    }

    @Test
    fun `dismissed announcement never re-emits after resend of same id`() = runTest {
        val repo = FakeRepository().apply { current.value = announcement }
        val observe = useCase(repo, versionCode = 150, now = 500L)

        DismissAnnouncementUseCase(repo).invoke(announcement.id)
        assertThat(observe.invoke().first()).isNull()

        // Re-sending the same id must not resurface it (repository contract:
        // dismissed ids are filtered out of observeCurrentAnnouncement).
        assertThat(repo.dismissed).contains(announcement.id)
    }

    // helper resolver mirroring the real one
    private val resolve = ResolveAnnouncementRouteUseCase(resolveFeatureKey = ::announcementRoute)

    @Test
    fun `known feature key resolves to NavigateToFeature with route`() {
        val action = resolve("quran/surah/18")
        assertThat(action).isEqualTo(
            AnnouncementAction.NavigateToFeature("quran/surah/18", Route.QuranReader(18))
        )
    }

    @Test
    fun `https url resolves to OpenUrl`() {
        assertThat(resolve("https://nimaz.arshadshah.com/privacy"))
            .isEqualTo(AnnouncementAction.OpenUrl("https://nimaz.arshadshah.com/privacy"))
    }

    @Test
    fun `unknown key resolves to None`() {
        assertThat(resolve("brand/new")).isEqualTo(AnnouncementAction.None)
        assertThat(resolve("")).isEqualTo(AnnouncementAction.None)
        assertThat(resolve(null)).isEqualTo(AnnouncementAction.None)
    }

    @Test
    fun `resolve route classifies url, known key, unknown key and blank`() {
        assertThat(resolve("search/ask"))
            .isEqualTo(AnnouncementAction.NavigateToFeature("search/ask", Route.GlobalSearch))
        assertThat(resolve("brand/new/key")).isEqualTo(AnnouncementAction.None)
        assertThat(resolve("  ")).isEqualTo(AnnouncementAction.None)
        // http (non-TLS) is not allowlisted
        assertThat(resolve("http://example.com")).isEqualTo(AnnouncementAction.None)
    }
}
