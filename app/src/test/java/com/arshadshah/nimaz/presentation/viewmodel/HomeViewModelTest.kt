package com.arshadshah.nimaz.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
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
    private lateinit var preferencesDataStore: PreferencesDataStore
    private lateinit var fastingDao: FastingDao
    private lateinit var hadithDao: HadithDao
    private lateinit var duaDao: DuaDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        context = ApplicationProvider.getApplicationContext()
        prayerTimeCalculator = mockk(relaxed = true)
        prayerRepository = mockk(relaxed = true)
        preferencesDataStore = mockk(relaxed = true)
        fastingDao = mockk(relaxed = true)
        hadithDao = mockk(relaxed = true)
        duaDao = mockk(relaxed = true)

        // The today's-records Flow emits synchronously, mirroring Room emitting an
        // initial value during ViewModel init before the rest of the constructor
        // has finished running.
        every { prayerRepository.getTodayPrayerRecords() } returns flowOf(emptyMap())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(
        context = context,
        prayerTimeCalculator = prayerTimeCalculator,
        prayerRepository = prayerRepository,
        preferencesDataStore = preferencesDataStore,
        fastingDao = fastingDao,
        hadithDao = hadithDao,
        duaDao = duaDao
    )

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
