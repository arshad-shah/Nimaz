package com.arshadshah.nimaz.presentation.viewmodel.home

import com.arshadshah.nimaz.domain.repository.FakePermissionChecker
import com.arshadshah.nimaz.domain.repository.FakePowerSettings
import com.arshadshah.nimaz.core.text.FakeStringProvider
import com.arshadshah.nimaz.domain.repository.RecordingWidgetRefresher
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.worship.NextWorshipResolver
import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.HomeEventCard
import com.arshadshah.nimaz.domain.repository.AnnouncementRepository
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import com.arshadshah.nimaz.domain.usecase.buildAnnouncementUseCases
import com.arshadshah.nimaz.domain.usecase.buildDuaUseCases
import com.arshadshah.nimaz.domain.usecase.buildFastingUseCases
import com.arshadshah.nimaz.domain.usecase.buildHadithUseCases
import com.arshadshah.nimaz.domain.usecase.buildObserveEventCardsUseCase
import com.arshadshah.nimaz.domain.usecase.buildPrayerUseCases

/**
 * Home across midnight.
 *
 * `observeFastingStatus()` computed `today`, `startOfDay` and `endOfDay` once, inside its
 * coroutine, at init — then collected a Room flow bound to that fixed range forever. Open the
 * app at 23:50 while fasting and leave it: at 00:05 Home still said "fasting today" about
 * yesterday's record, and marking the new day's fast could never light it up, because the
 * collector's range did not include the new day.
 *
 * `HomeScreen` did dispatch `RefreshPrayerTimes` on rollover — but that reached only
 * `calculatePrayerTimes()`. Nothing restarted this collector.
 */
// No `runTest` here: HomeViewModel keeps an infinite `while (isActive) { delay(…) }` worship
// refresh alive, and runTest's cleanup advances virtual time until the scheduler is idle —
// which that loop never is. The unconfined dispatcher runs everything eagerly instead, which
// is what the sibling HomeViewModelTest does for the same reason.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class HomeViewModelRolloverTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val yesterday = LocalDate.of(2026, 3, 14)
    private val today = yesterday.plusDays(1)
    private val todayProvider = FakeTodayProvider(yesterday)

    private lateinit var fastingRepository: FastingRepository
    private lateinit var prayerRepository: PrayerRepository
    private lateinit var hadithRepository: HadithRepository
    private lateinit var duaRepository: DuaRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var announcementRepository: AnnouncementRepository
    private lateinit var nextWorshipResolver: NextWorshipResolver

    /** Which day's records each range query asked for, in order. */
    private val requestedRanges = mutableListOf<Long>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fastingRepository = mockk(relaxed = true)
        prayerRepository = mockk(relaxed = true)
        hadithRepository = mockk(relaxed = true)
        duaRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        announcementRepository = mockk(relaxed = true)
        nextWorshipResolver = mockk(relaxed = true)

        // `ObserveEventCardsUseCase` combines the local occasions with the announcement stream,
        // and `combine` emits nothing until *both* sides have. A relaxed mock returns a Flow that
        // never emits, which would leave `celebrationCards` empty whether or not the collector
        // re-arms — the assertion below would pass for the wrong reason.
        every { announcementRepository.observeCurrentAnnouncement() } returns flowOf(null)

        // Only *yesterday* has a fast recorded. If the collector is still bound to
        // yesterday's range after midnight, Home keeps claiming the user is fasting today.
        every { fastingRepository.getFastRecordsInRange(any(), any()) } answers {
            val start = firstArg<Long>()
            requestedRanges += start
            if (start == yesterday.toUtcMidnightMillis()) {
                flowOf(listOf(fastRecord(start)))
            } else {
                flowOf(emptyList())
            }
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /**
     * Stands in for `ObserveLocalEventsUseCase`, which reads the wall clock *inside* its `map`:
     * the flow it hands back never re-emits on its own when the day turns, so the only way Home
     * learns about the new day's occasions is the collector being re-subscribed.
     */
    private fun localEvents(): Flow<List<HomeEventCard>> = flow {
        emit(if (todayProvider.now == today) listOf(mawlid) else emptyList())
    }

    private fun viewModel(
        local: () -> Flow<List<HomeEventCard>> = { flowOf(emptyList()) },
    ): HomeViewModel {
        val announcementUseCases = buildAnnouncementUseCases(announcementRepository)
        return HomeViewModel(
            strings = FakeStringProvider(),
            permissions = FakePermissionChecker(),
            powerSettings = FakePowerSettings(),
            widgets = RecordingWidgetRefresher(),
            telemetry = RecordingTelemetry(),
            prayerUseCases = buildPrayerUseCases(prayerRepository),
            fastingUseCases = buildFastingUseCases(fastingRepository),
            hadithUseCases = buildHadithUseCases(hadithRepository),
            duaUseCases = buildDuaUseCases(duaRepository),
            locationSettings = settingsRepository,
            announcementUseCases = announcementUseCases,
            observeEventCards = buildObserveEventCardsUseCase(announcementUseCases, local),
            nextWorshipResolver = nextWorshipResolver,
            todayProvider = todayProvider,
        )
    }

    @Test
    fun `fasting today stops meaning yesterday once the day changes`() {
        val vm = viewModel()
        assertThat(vm.state.value.fastingToday).isTrue()

        todayProvider.now = today

        assertThat(vm.state.value.fastingToday).isFalse()
    }

    @Test
    fun `the fasting collector re-arms on the new day's range`() {
        viewModel()
        requestedRanges.clear()

        todayProvider.now = today

        // Without a re-arm the new day is never queried at all, so marking today's fast
        // cannot light the card up however many times Room re-emits.
        assertThat(requestedRanges).contains(today.toUtcMidnightMillis())
    }

    @Test
    fun `today's Islamic occasion follows the day across midnight`() {
        val vm = viewModel(local = ::localEvents)
        // Mawlid is tomorrow's occasion, so nothing shows on the eve.
        assertThat(vm.state.value.celebrationCards).isEmpty()

        todayProvider.now = today

        // Left open overnight, Home used to keep the eve's answer: the occasion collector was
        // started once at init and the flow behind it only re-emits when the Hijri offset moves.
        assertThat(vm.state.value.celebrationCards.map { it.eyebrow })
            .containsExactly("Mawlid an-Nabi")
    }

    @Test
    fun `a day that has not changed does not re-issue everything`() {
        viewModel()
        requestedRanges.clear()

        // The provider re-emitting the same date must not restart the collectors.
        todayProvider.now = yesterday

        assertThat(requestedRanges).isEmpty()
    }
}

private fun fastRecord(dateEpoch: Long) = FastRecord(
    id = 1L,
    date = dateEpoch,
    hijriDate = null,
    hijriMonth = null,
    hijriYear = null,
    fastType = FastType.RAMADAN,
    status = FastStatus.FASTED,
    exemptionReason = null,
    suhoorTime = null,
    iftarTime = null,
    note = null,
    createdAt = 0L,
    updatedAt = 0L,
)

private val mawlid = HomeEventCard(
    event = CelebrationEvent.MAWLID,
    eyebrow = "Mawlid an-Nabi",
    headline = "Mawlid an-Nabi",
    body = "Birth of Prophet Muhammad (PBUH)",
    arabic = "المولد النبوي",
    priority = 1,
)
