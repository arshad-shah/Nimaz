package com.arshadshah.nimaz.presentation.viewmodel.more

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.PinnedShortcut
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonProgress
import com.arshadshah.nimaz.domain.model.WorshipReminderOccurrence
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.domain.repository.settings.MoreSettings
import com.arshadshah.nimaz.domain.usecase.GetAllHistoryUseCase
import com.arshadshah.nimaz.domain.usecase.GetHijriTodayUseCase
import com.arshadshah.nimaz.domain.usecase.GetNextWorshipUseCase
import com.arshadshah.nimaz.domain.usecase.GetPendingMakeupFastsUseCase
import com.arshadshah.nimaz.domain.usecase.GetTodayPrayerRecordsUseCase
import com.arshadshah.nimaz.domain.usecase.MoreUseCases
import com.arshadshah.nimaz.domain.usecase.ObserveHijriDayOffsetUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveKhatamRowProgressUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveQaidaRowProgressUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveZakatCurrencyUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * More's ViewModel: does it report the truth, and does it stay quiet when it cannot?
 *
 * The interesting assertions are the negative ones. `MoreUiState` deliberately has no `UiError`
 * field (spec §2.4), so "the makeup-fast query blew up" has to manifest as *that one field staying
 * null while every other field still fills in* — there is no error state to check instead. A
 * regression here does not throw and does not show a red screen; it silently takes the whole
 * screen's subtitles down, which is exactly the shape of bug that reaches production.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MoreViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val today = LocalDate.of(2026, 8, 5)
    private val zone = ZoneId.systemDefault()
    private val clock: Clock = Clock.fixed(
        today.atTime(20, 0).atZone(zone).toInstant(),
        zone,
    )

    private val telemetry = RecordingTelemetry()
    private val todayProvider = FakeTodayProvider(today)
    private val pins = MutableStateFlow(PinnedShortcut.DEFAULTS)

    private val moreSettings = object : MoreSettings {
        override val pinnedShortcuts: Flow<List<PinnedShortcut>> = pins
        var written: List<PinnedShortcut>? = null
        override suspend fun setPinnedShortcuts(shortcuts: List<PinnedShortcut>) {
            written = shortcuts
            pins.value = PinnedShortcut.decode(PinnedShortcut.encode(shortcuts))
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ── Assembly ─────────────────────────────────────────────────────────

    @Test
    fun `state reports every row from its own source`() {
        val vm = viewModel()

        val state = vm.state.value
        assertThat(state.prayersLogged).isEqualTo(3)
        // Five, not six: sunrise is in the record map and is not a prayer anyone logs, so
        // counting it would make the denominator wrong every single day.
        assertThat(state.prayersTrackable).isEqualTo(5)
        assertThat(state.pendingMakeupFasts).isEqualTo(2)
        assertThat(state.khatamJuz).isEqualTo(3)
        assertThat(state.qaidaLesson).isEqualTo(4)
        assertThat(state.qaidaTotalLessons).isEqualTo(21)
        assertThat(state.hijriToday).isNotEmpty()
        assertThat(state.zakatHistoryLoaded).isTrue()
        assertThat(state.zakatCurrency).isEqualTo("EUR")
    }

    @Test
    fun `a missed prayer counts as logged, a pending one does not`() {
        // MISSED is a deliberate record, not an absence — someone answered. Only PENDING and
        // NOT_PRAYED mean nothing has been said.
        val vm = viewModel(
            prayerRecords = flowOf(
                mapOf(
                    PrayerName.FAJR to PrayerStatus.PRAYED,
                    PrayerName.DHUHR to PrayerStatus.MISSED,
                    PrayerName.ASR to PrayerStatus.QADA,
                    PrayerName.MAGHRIB to PrayerStatus.LATE,
                    PrayerName.ISHA to PrayerStatus.PENDING,
                )
            )
        )
        assertThat(vm.state.value.prayersLogged).isEqualTo(4)
        assertThat(vm.state.value.prayersTrackable).isEqualTo(5)
    }

    @Test
    fun `zakat reports this lunar year's figure and ignores an older one`() {
        val vm = viewModel()
        // 1284.50 was saved this Hijri year; 900.0 two Gregorian years ago was not.
        assertThat(vm.state.value.zakatDueThisYear).isEqualTo(1284.50)
    }

    @Test
    fun `no calculation this year loads to a known-absent figure, not to an unknown one`() {
        // The distinction the subtitle needs: loaded-and-nothing says "not calculated this year",
        // not-loaded says nothing at all.
        val vm = viewModel(zakatHistory = flowOf(emptyList()))
        assertThat(vm.state.value.zakatHistoryLoaded).isTrue()
        assertThat(vm.state.value.zakatDueThisYear).isNull()
    }

    @Test
    fun `the next worship reminder is measured against the injected clock`() {
        // 20:00 fixed, event at 23:12 → 192 minutes. Reading the wall clock instead would make
        // this assertion depend on when the suite runs.
        val vm = viewModel()
        assertThat(vm.state.value.nextWorship).isEqualTo(WorshipReminderType.TAHAJJUD)
        assertThat(vm.state.value.minutesUntilNextWorship).isEqualTo(192)
    }

    // ── A failing source costs one subtitle ──────────────────────────────

    @Test
    fun `a source that throws leaves its own field null and fills in the rest`() {
        val vm = viewModel(
            makeupFasts = flow<List<MakeupFast>> { throw IllegalStateException("dao is angry") }
        )

        val state = vm.state.value
        // The failed row: zero pending, which the mapper renders as no subtitle at all.
        assertThat(state.pendingMakeupFasts).isEqualTo(0)
        // Everything else still reported. This is the assertion that matters: the combine must
        // not have collapsed, and the screen must not be blank.
        assertThat(state.prayersLogged).isEqualTo(3)
        assertThat(state.khatamJuz).isEqualTo(3)
        assertThat(state.hijriToday).isNotEmpty()
    }

    @Test
    fun `a failure is reported to telemetry rather than swallowed`() {
        // There is no UiError to carry it, so monitoring is the only place it can land. If this
        // stops holding, a broken subtitle becomes completely invisible.
        viewModel(makeupFasts = flow { throw IllegalStateException("dao is angry") })
        assertThat(telemetry.exceptions.map { it.message }).contains("dao is angry")
    }

    @Test
    fun `no khatam and no qaida progress is silence, not a failure`() {
        val vm = viewModel(
            khatam = flowOf(null),
            qaidaProgress = flowOf(emptyList()),
        )
        assertThat(vm.state.value.khatamJuz).isNull()
        assertThat(vm.state.value.qaidaLesson).isNull()
        assertThat(telemetry.exceptions).isEmpty()
    }

    // ── Rollover ─────────────────────────────────────────────────────────

    @Test
    fun `the day rolling over re-invokes the day-scoped sources`() {
        // todayPrayerRecords() bakes today's epoch range into the query it returns, so a
        // re-*read* is not enough — it has to be re-*invoked*. Counting calls is the only way to
        // tell the two apart from outside.
        var invocations = 0
        val vm = viewModel(prayerRecordsFactory = {
            invocations++
            flowOf(mapOf(PrayerName.FAJR to PrayerStatus.PRAYED))
        })
        assertThat(invocations).isEqualTo(1)

        todayProvider.now = today.plusDays(1)

        assertThat(invocations).isEqualTo(2)
    }

    @Test
    fun `the hijri date follows the day, not the day the screen opened`() {
        val vm = viewModel()
        val before = vm.state.value.hijriToday

        todayProvider.now = today.plusDays(1)

        assertThat(vm.state.value.hijriToday).isNotEqualTo(before)
    }

    // ── Pins ─────────────────────────────────────────────────────────────

    @Test
    fun `pins start at the defaults rather than empty`() {
        // An empty first frame would flash a blank row on every open.
        assertThat(viewModel().state.value.pinnedShortcuts).isEqualTo(PinnedShortcut.DEFAULTS)
    }

    @Test
    fun `setting pins persists them in order`() {
        val vm = viewModel()
        val chosen = listOf(PinnedShortcut.QIBLA, PinnedShortcut.ZAKAT)

        vm.onEvent(MoreEvent.SetPins(chosen))

        assertThat(moreSettings.written).isEqualTo(chosen)
        assertThat(vm.state.value.pinnedShortcuts).containsExactlyElementsIn(chosen).inOrder()
    }

    @Test
    fun `an over-long pin list is capped on the way to disk rather than rejected`() {
        val vm = viewModel()

        vm.onEvent(MoreEvent.SetPins(PinnedShortcut.entries.toList()))

        assertThat(vm.state.value.pinnedShortcuts).hasSize(PinnedShortcut.MAX_PINS)
        assertThat(vm.state.value.canPinMore).isFalse()
    }

    @Test
    fun `canPinMore is false exactly at the cap`() {
        val vm = viewModel()
        assertThat(vm.state.value.canPinMore).isTrue()

        vm.onEvent(MoreEvent.SetPins(PinnedShortcut.entries.take(PinnedShortcut.MAX_PINS)))

        assertThat(vm.state.value.canPinMore).isFalse()
    }

    @Test
    fun `pinning is recorded, because whether anyone pins is the question`() {
        viewModel().onEvent(MoreEvent.SetPins(listOf(PinnedShortcut.QIBLA)))
        assertThat(telemetry.featureUsages.map { it.feature to it.action })
            .contains("more" to "set_pins")
    }

    // ── Fixtures ─────────────────────────────────────────────────────────

    private fun viewModel(
        prayerRecords: Flow<Map<PrayerName, PrayerStatus>>? = null,
        prayerRecordsFactory: (() -> Flow<Map<PrayerName, PrayerStatus>>)? = null,
        makeupFasts: Flow<List<MakeupFast>>? = null,
        khatam: Flow<Khatam?>? = null,
        qaidaProgress: Flow<List<QaidaLessonProgress>>? = null,
        zakatHistory: Flow<List<ZakatHistoryEntry>>? = null,
    ): MoreViewModel {
        val prayerRepository = mockk<com.arshadshah.nimaz.domain.repository.PrayerRepository>()
        every { prayerRepository.getTodayPrayerRecords() } answers {
            prayerRecordsFactory?.invoke()
                ?: prayerRecords
                ?: flowOf(
                    mapOf(
                        PrayerName.FAJR to PrayerStatus.PRAYED,
                        PrayerName.SUNRISE to PrayerStatus.PENDING,
                        PrayerName.DHUHR to PrayerStatus.PRAYED,
                        PrayerName.ASR to PrayerStatus.PRAYED,
                        PrayerName.MAGHRIB to PrayerStatus.PENDING,
                        PrayerName.ISHA to PrayerStatus.PENDING,
                    )
                )
        }

        val fastingRepository = mockk<com.arshadshah.nimaz.domain.repository.FastingRepository>()
        every { fastingRepository.getPendingMakeupFasts() } returns (
            makeupFasts ?: flowOf(listOf(makeupFast(1), makeupFast(2)))
            )

        val khatamRepository = mockk<com.arshadshah.nimaz.domain.repository.KhatamRepository>()
        val activeKhatam = khatam ?: flowOf(
            Khatam(
                id = 7,
                name = "Ramadan",
                dailyTarget = 20,
                totalAyahsRead = 400,
                startedAt = today.minusDays(20).atStartOfDay(zone).toInstant().toEpochMilli(),
            )
        )
        every { khatamRepository.observeActiveKhatam() } returns activeKhatam
        every { khatamRepository.observeJuzProgress(any()) } returns flowOf(
            // Juz 1 and 2 done, juz 3 partly — the reader is *on* juz 3.
            listOf(
                JuzProgressInfo(1, totalAyahs = 148, readAyahs = 148),
                JuzProgressInfo(2, totalAyahs = 111, readAyahs = 111),
                JuzProgressInfo(3, totalAyahs = 125, readAyahs = 40),
                JuzProgressInfo(4, totalAyahs = 130, readAyahs = 0),
            )
        )

        val qaidaRepository = mockk<com.arshadshah.nimaz.domain.repository.QaidaRepository>()
        every { qaidaRepository.getLessons() } returns flowOf((1..21).map { lesson(it) })
        every { qaidaRepository.getAllProgress() } returns (
            qaidaProgress ?: flowOf(
                listOf(
                    lessonProgress(1, LessonStatus.COMPLETED),
                    lessonProgress(2, LessonStatus.COMPLETED),
                    lessonProgress(3, LessonStatus.COMPLETED),
                    lessonProgress(4, LessonStatus.IN_PROGRESS),
                    // Unlocked but never touched — not where anyone is.
                    lessonProgress(5, LessonStatus.UNLOCKED),
                )
            )
            )

        val zakatRepository = mockk<com.arshadshah.nimaz.domain.repository.ZakatRepository>()
        every { zakatRepository.getAllHistory() } returns (
            zakatHistory ?: flowOf(
                listOf(
                    zakatEntry(due = 1284.50, at = today.minusDays(30)),
                    zakatEntry(due = 900.0, at = today.minusYears(2)),
                )
            )
            )

        val resolver = mockk<com.arshadshah.nimaz.core.util.NextWorshipResolver>()
        io.mockk.coEvery { resolver.nearest(any()) } returns WorshipReminderOccurrence(
            type = WorshipReminderType.TAHAJJUD,
            triggerAt = today.atTime(23, 12),
            eventAt = today.atTime(23, 12),
        )

        val settings = mockk<com.arshadshah.nimaz.domain.repository.SettingsRepository>()
        every { settings.hijriDayOffset } returns flowOf(0)

        val zakatSettings =
            mockk<com.arshadshah.nimaz.domain.repository.settings.ZakatSettings>()
        every { zakatSettings.zakatCurrency } returns flowOf("EUR")

        return MoreViewModel(
            useCases = MoreUseCases(
                todayPrayerRecords = GetTodayPrayerRecordsUseCase(prayerRepository),
                pendingMakeupFasts = GetPendingMakeupFastsUseCase(fastingRepository),
                nextWorship = GetNextWorshipUseCase(resolver),
                khatamRowProgress = ObserveKhatamRowProgressUseCase(khatamRepository),
                qaidaRowProgress = ObserveQaidaRowProgressUseCase(qaidaRepository),
                zakatHistory = GetAllHistoryUseCase(zakatRepository),
                hijriToday = GetHijriTodayUseCase(),
                hijriDayOffset = ObserveHijriDayOffsetUseCase(settings),
                zakatCurrency = ObserveZakatCurrencyUseCase(zakatSettings),
            ),
            moreSettings = moreSettings,
            todayProvider = todayProvider,
            clock = clock,
            telemetry = telemetry,
        )
    }

    private fun makeupFast(id: Long) = MakeupFast(
        id = id,
        originalDate = today.minusDays(id).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        originalHijriDate = null,
        reason = FastStatus.NOT_FASTED.name,
        status = MakeupFastStatus.PENDING,
        completedDate = null,
        fidyaAmount = null,
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun lesson(id: Int) = QaidaLesson(
        id = id,
        lessonNumber = id,
        titleEnglish = "Lesson $id",
        titleArabic = "",
        titleTransliteration = "",
        description = "",
        conceptTags = emptyList(),
        icon = "",
        displayOrder = id,
    )

    private fun lessonProgress(id: Int, status: LessonStatus) = QaidaLessonProgress(
        lessonId = id,
        status = status,
        stars = 0,
        lastCellId = null,
        completedCells = 0,
        totalCells = 10,
        updatedAt = 0L,
    )

    private fun zakatEntry(due: Double, at: LocalDate) = ZakatHistoryEntry(
        calculatedAt = at.atStartOfDay(zone).toInstant().toEpochMilli(),
        totalAssets = 0.0,
        totalLiabilities = 0.0,
        netWorth = 0.0,
        zakatDue = due,
        nisabType = NisabType.GOLD,
        nisabValue = 0.0,
    )

    private fun instantOf(dateTime: LocalDateTime): Instant = dateTime.atZone(zone).toInstant()
}
