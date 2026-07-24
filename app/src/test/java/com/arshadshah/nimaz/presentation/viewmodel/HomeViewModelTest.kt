package com.arshadshah.nimaz.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.repository.AnnouncementRepository
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
}
