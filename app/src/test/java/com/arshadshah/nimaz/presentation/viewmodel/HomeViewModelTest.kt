package com.arshadshah.nimaz.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.util.NextWorshipResolver
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.AnnouncementRepository
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class HomeViewModelTest {

    // Unconfined so coroutines launched from init{} run eagerly during
    // construction — exactly like the production crash where Room's Flow emits
    // an initial value synchronously on an unconfined dispatch.
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var prayerTimeCalculator: PrayerTimeCalculator
    private lateinit var prayerRepository: PrayerRepository
    private lateinit var fastingRepository: FastingRepository
    private lateinit var hadithRepository: HadithRepository
    private lateinit var duaRepository: DuaRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var announcementRepository: AnnouncementRepository
    private lateinit var nextWorshipResolver: NextWorshipResolver

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        context = ApplicationProvider.getApplicationContext()
        prayerTimeCalculator = mockk(relaxed = true)
        prayerRepository = mockk(relaxed = true)
        fastingRepository = mockk(relaxed = true)
        hadithRepository = mockk(relaxed = true)
        duaRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        announcementRepository = mockk(relaxed = true)
        nextWorshipResolver = mockk(relaxed = true)
        every { announcementRepository.observeCurrentAnnouncement() } returns flowOf(null)

        // The today's-records Flow emits synchronously, mirroring Room emitting an
        // initial value during ViewModel init before the rest of the constructor
        // has finished running.
        every { prayerRepository.getTodayPrayerRecords() } returns flowOf(emptyMap())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        val announcementUseCases = buildAnnouncementUseCases(announcementRepository)
        return HomeViewModel(
            context = context,
            prayerTimeCalculator = prayerTimeCalculator,
            prayerUseCases = buildPrayerUseCases(prayerRepository),
            fastingUseCases = buildFastingUseCases(fastingRepository),
            hadithUseCases = buildHadithUseCases(hadithRepository),
            duaUseCases = buildDuaUseCases(duaRepository),
            settingsRepository = settingsRepository,
            announcementUseCases = announcementUseCases,
            observeEventCards = buildObserveEventCardsUseCase(announcementUseCases),
            nextWorshipResolver = nextWorshipResolver,
        )
    }

    /**
     * Constructs the ViewModel while capturing any exception that escapes a
     * `launch {}` coroutine. In production such an exception is routed to the
     * thread's uncaught-exception handler, which crashes the app on the main
     * thread — it does NOT surface as a thrown constructor, so we must intercept
     * it the same way the platform does.
     */
    private fun captureUncaughtDuringConstruction(): Throwable? {
        val captured = AtomicReference<Throwable?>(null)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            captured.compareAndSet(null, throwable)
        }
        try {
            createViewModel()
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
        return captured.get()
    }

    @Test
    fun `init does not crash when prayer records flow emits during construction`() {
        // Regression test for a NullPointerException: loadPrayerRecords() was
        // launched from init{} and collected getTodayPrayerRecords(), calling
        // _prayerRecords.update { } before the _prayerRecords MutableStateFlow had
        // been initialized — its property was declared *after* the init block, so
        // a synchronous emission dereferenced a null field.
        val uncaught = captureUncaughtDuringConstruction()

        assertThat(uncaught).isNull()
    }

    @Test
    fun `prayer records emitted during init do not raise NullPointerException`() {
        val records = mapOf(
            PrayerName.FAJR to PrayerStatus.PRAYED,
            PrayerName.DHUHR to PrayerStatus.NOT_PRAYED
        )
        every { prayerRepository.getTodayPrayerRecords() } returns flowOf(records)

        val uncaught = captureUncaughtDuringConstruction()

        assertThat(uncaught).isNull()
    }

    /** Six prayer instants spread either side of "now", so a next prayer exists. */
    private fun samplePrayerTimes(): List<PrayerTime> {
        val base = Clock.System.now()
        return listOf(
            PrayerTime(PrayerType.FAJR, base - 5.hours),
            PrayerTime(PrayerType.SUNRISE, base - 4.hours),
            PrayerTime(PrayerType.DHUHR, base - 1.hours),
            PrayerTime(PrayerType.ASR, base + 2.hours),
            PrayerTime(PrayerType.MAGHRIB, base + 5.hours),
            PrayerTime(PrayerType.ISHA, base + 6.hours),
        )
    }

    /**
     * Runs [body] against the shared test scheduler.
     *
     * Deliberately **not** `runTest`: `HomeViewModel` keeps two endless
     * `while (isActive) { delay(...) }` loops alive in `viewModelScope`, and
     * `runTest` drains its scheduler to idle when the body returns — which with an
     * endless delay loop means advancing virtual time forever, hanging the suite.
     * Here time is advanced only in bounded steps, and the collector scope is
     * cancelled explicitly.
     */
    private fun withScheduler(body: (TestCoroutineScheduler, CoroutineScope) -> Unit) {
        val scope = CoroutineScope(testDispatcher)
        try {
            body(testDispatcher.scheduler, scope)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `refreshing over loaded data never re-emits the loading state`() =
        withScheduler { scheduler, scope ->
            // Regression test for the home screen flashing a full-screen spinner
            // once a second. calculatePrayerTimes() set isLoading = true on every
            // call, including the per-second countdown refresh. That was invisible
            // while the whole body ran without suspending, but PR #319 added the
            // worship-card resolution — real DataStore I/O — in the middle of it,
            // which let the transient `true` reach the UI on every tick.
            every {
                prayerTimeCalculator.getPrayerTimes(any(), any(), any(), any(), any(), any(), any())
            } returns samplePrayerTimes()
            // Make the worship resolution genuinely suspend, exactly as the real
            // DataStore-backed resolver does. Without this the bug is unobservable.
            coEvery { nextWorshipResolver.nearest(any()) } coAnswers {
                delay(50)
                null
            }

            val viewModel = createViewModel()
            val seen = mutableListOf<Boolean>()
            scope.launch { viewModel.state.collect { seen += it.isLoading } }

            // First load: a spinner here is legitimate — there is nothing to show.
            viewModel.onEvent(HomeEvent.RefreshPrayerTimes)
            scheduler.advanceTimeBy(1_000)
            assertThat(viewModel.state.value.prayerTimes).isNotEmpty()

            // Everything from here on is a refresh over data already on screen.
            seen.clear()
            repeat(5) {
                viewModel.onEvent(HomeEvent.RefreshPrayerTimes)
                scheduler.advanceTimeBy(1_000)
            }

            assertThat(seen).doesNotContain(true)
        }

    @Test
    fun `countdown ticks do not re-resolve the worship card every second`() =
        withScheduler { scheduler, _ ->
            // The worship card's countdown renders in whole minutes, but resolving
            // it costs ~30 sequential DataStore reads. Ticking the countdown must
            // not drag that resolution along with it once a second.
            every {
                prayerTimeCalculator.getPrayerTimes(any(), any(), any(), any(), any(), any(), any())
            } returns samplePrayerTimes()
            coEvery { nextWorshipResolver.nearest(any()) } returns null

            val viewModel = createViewModel()
            viewModel.onEvent(HomeEvent.RefreshPrayerTimes)
            scheduler.advanceTimeBy(1_000)
            clearMocks(nextWorshipResolver, answers = false)

            // Thirty seconds of countdown ticks, well inside the 60s worship loop.
            scheduler.advanceTimeBy(30_000)

            coVerify(exactly = 0) { nextWorshipResolver.nearest(any()) }
        }
}
