package com.arshadshah.nimaz.presentation.viewmodel.home

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.text.FakeStringProvider
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.domain.model.DailyDuaSelection
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.AnnouncementRepository
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.domain.repository.FakePermissionChecker
import com.arshadshah.nimaz.domain.repository.FakePowerSettings
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.RecordingWidgetRefresher
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.buildAnnouncementUseCases
import com.arshadshah.nimaz.domain.usecase.buildDuaUseCases
import com.arshadshah.nimaz.domain.usecase.buildFastingUseCases
import com.arshadshah.nimaz.domain.usecase.buildHadithUseCases
import com.arshadshah.nimaz.domain.usecase.buildObserveEventCardsUseCase
import com.arshadshah.nimaz.domain.usecase.buildPrayerUseCases
import com.arshadshah.nimaz.domain.usecase.prayerCalculationSettings
import com.arshadshah.nimaz.domain.worship.NextWorshipResolver
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What the Home ViewModel does when the reader touches it.
 *
 * `HomeViewModelTest` is a construction-safety suite — it exists because the ViewModel used to
 * throw during `init`. This is the other half: the loaders that fill the dashboard, the two
 * prayer-status writes, and the announcement banner's two records. Between them roughly half
 * the file had never run.
 *
 * The banner tests subscribe to `announcement` first. It is a `WhileSubscribed` flow, so with
 * no collector `announcement.value` is the default and `dismissAnnouncement` returns at its
 * first line — a green test over a ViewModel that did nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class HomeViewModelBehaviourTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var prayerRepository: PrayerRepository
    private lateinit var fastingRepository: FastingRepository
    private lateinit var hadithRepository: HadithRepository
    private lateinit var duaRepository: DuaRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var announcementRepository: AnnouncementRepository
    private lateinit var nextWorshipResolver: NextWorshipResolver
    private lateinit var widgets: RecordingWidgetRefresher
    private lateinit var telemetry: RecordingTelemetry

    private val announcements = MutableStateFlow<Announcement?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ApplicationProvider.getApplicationContext<Application>()

        prayerRepository = mockk(relaxed = true)
        fastingRepository = mockk(relaxed = true)
        hadithRepository = mockk(relaxed = true)
        duaRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        announcementRepository = mockk(relaxed = true)
        nextWorshipResolver = mockk(relaxed = true)
        widgets = RecordingWidgetRefresher()
        telemetry = RecordingTelemetry()

        every { announcementRepository.observeCurrentAnnouncement() } returns announcements
        every { prayerRepository.getTodayPrayerRecords() } returns flowOf(emptyMap())
        every { prayerRepository.observeCalculationSettings() } returns
            flowOf(prayerCalculationSettings())
        every { prayerRepository.getDaySchedule(any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): HomeViewModel {
        val announcementUseCases = buildAnnouncementUseCases(announcementRepository)
        return HomeViewModel(
            strings = FakeStringProvider(),
            permissions = FakePermissionChecker(),
            powerSettings = FakePowerSettings(),
            widgets = widgets,
            telemetry = telemetry,
            prayerUseCases = buildPrayerUseCases(prayerRepository),
            fastingUseCases = buildFastingUseCases(fastingRepository),
            hadithUseCases = buildHadithUseCases(hadithRepository),
            duaUseCases = buildDuaUseCases(duaRepository),
            locationSettings = settingsRepository,
            announcementUseCases = announcementUseCases,
            observeEventCards = buildObserveEventCardsUseCase(announcementUseCases),
            nextWorshipResolver = nextWorshipResolver,
            todayProvider = FakeTodayProvider(java.time.LocalDate.now()),
        )
    }

    /**
     * A scope for the collectors these tests need, **not** `runTest`.
     *
     * `HomeViewModel` keeps endless `while (isActive) { delay(…) }` loops alive in
     * `viewModelScope`, and `runTest` drains its scheduler to idle when the body returns —
     * which against an endless delay loop means advancing virtual time forever and hanging
     * the whole suite. `HomeViewModelTest` records the same constraint; this is the same
     * shape without the virtual-time stepping those tests need.
     */
    private fun withScope(body: CoroutineScope.() -> Unit) {
        val scope = CoroutineScope(testDispatcher)
        try {
            scope.body()
        } finally {
            scope.cancel()
        }
    }

    // ── The daily hadith card ───────────────────────────────────────────────────

    @Test
    fun `the daily hadith is trimmed so the card does not become a wall of text`() = withScope {
        coEvery { hadithRepository.getHadithCount() } returns 1
        coEvery { hadithRepository.getHadithByOffset(any()) } returns
            hadith(textEnglish = "x".repeat(400))

        val state = viewModel().state.value

        assertThat(state.dailyHadith!!.length).isAtMost(151)
        assertThat(state.dailyHadith).endsWith("…")
    }

    @Test
    fun `a short hadith is shown whole, with no ellipsis`() = withScope {
        coEvery { hadithRepository.getHadithCount() } returns 1
        coEvery { hadithRepository.getHadithByOffset(any()) } returns
            hadith(textEnglish = "Actions are by intentions.")

        assertThat(viewModel().state.value.dailyHadith).isEqualTo("Actions are by intentions.")
    }

    @Test
    fun `the hadith carries its id, so tapping the card opens that exact hadith`() = withScope {
        coEvery { hadithRepository.getHadithCount() } returns 1
        coEvery { hadithRepository.getHadithByOffset(any()) } returns hadith(id = "bukhari-1")

        assertThat(viewModel().state.value.dailyHadithId).isEqualTo("bukhari-1")
    }

    @Test
    fun `a blank reference is dropped rather than rendering an empty chip`() = withScope {
        coEvery { hadithRepository.getHadithCount() } returns 1
        coEvery { hadithRepository.getHadithByOffset(any()) } returns hadith(reference = "   ")

        assertThat(viewModel().state.value.dailyHadithReference).isNull()
    }

    @Test
    fun `every grade the corpus carries gets its own chip label`() = withScope {
        // An unknown or absent grade must produce no chip at all rather than the word "null".
        val labels = HadithGrade.entries.associateWith { grade ->
            coEvery { hadithRepository.getHadithCount() } returns 1
            coEvery { hadithRepository.getHadithByOffset(any()) } returns hadith(grade = grade)
            viewModel().state.value.dailyHadithGrade
        }

        assertThat(labels[HadithGrade.SAHIH]).isNotNull()
        assertThat(labels[HadithGrade.HASAN]).isNotNull()
        assertThat(labels[HadithGrade.DAIF]).isNotNull()
        assertThat(labels[HadithGrade.MAWDU]).isNotNull()

        coEvery { hadithRepository.getHadithByOffset(any()) } returns hadith(grade = null)
        assertThat(viewModel().state.value.dailyHadithGrade).isNull()
    }

    @Test
    fun `no hadith corpus leaves the card absent rather than blank`() = withScope {
        coEvery { hadithRepository.getHadithCount() } returns 0

        assertThat(viewModel().state.value.dailyHadith).isNull()
    }

    @Test
    fun `a hadith lookup that throws does not take the rest of the dashboard with it`() = withScope {
        coEvery { hadithRepository.getHadithCount() } throws IllegalStateException("db closed")

        val state = viewModel().state.value

        assertThat(state.dailyHadith).isNull()
        assertThat(state.error).isNull()
    }

    // ── The daily dua card ──────────────────────────────────────────────────────

    @Test
    fun `the daily dua carries its category label and icon for the card chip`() = withScope {
        coEvery { duaRepository.getCategoryById(any()) } returns category()
        coEvery { duaRepository.getDuasByCategoryOnce(any()) } returns listOf(dua())

        val daily = viewModel().state.value.dailyDua!!

        assertThat(daily.duaId).isEqualTo("dua-1")
        assertThat(daily.title).isEqualTo("Morning remembrance")
        assertThat(daily.categoryLabel).isEqualTo("Morning adhkar")
        assertThat(daily.categoryIcon).isEqualTo("sun")
    }

    @Test
    fun `a dua with no reference gets an empty source rather than a null one`() = withScope {
        coEvery { duaRepository.getCategoryById(any()) } returns category()
        coEvery { duaRepository.getDuasByCategoryOnce(any()) } returns listOf(dua(reference = null))

        assertThat(viewModel().state.value.dailyDua!!.source).isEmpty()
    }

    @Test
    fun `a category with no duas leaves the card absent`() = withScope {
        coEvery { duaRepository.getCategoryById(any()) } returns category()
        coEvery { duaRepository.getDuasByCategoryOnce(any()) } returns emptyList()

        assertThat(viewModel().state.value.dailyDua).isNull()
    }

    @Test
    fun `a dua lookup that throws leaves the rest of the dashboard alone`() = withScope {
        coEvery { duaRepository.getCategoryById(any()) } throws IllegalStateException("db closed")

        assertThat(viewModel().state.value.dailyDua).isNull()
    }

    // ── Tracking a prayer from the dashboard ────────────────────────────────────

    @Test
    fun `toggling an untracked prayer records it as prayed and refreshes the widget`() = withScope {
        // The widget is a separate process reading the same rows; without the refresh it keeps
        // showing yesterday's tick until something else happens to update it.
        val viewModel = viewModel()

        viewModel.onEvent(HomeEvent.TogglePrayerStatus(PrayerType.DHUHR))

        coVerify {
            prayerRepository.updatePrayerStatus(any(), PrayerName.DHUHR, PrayerStatus.PRAYED, any(), false)
        }
        assertThat(widgets.refreshCount).isAtLeast(1)
    }

    @Test
    fun `toggling a prayer that is already prayed takes it back off`() = withScope {
        every { prayerRepository.getTodayPrayerRecords() } returns
            flowOf(mapOf(PrayerName.ASR to PrayerStatus.PRAYED))
        val viewModel = viewModel()

        viewModel.onEvent(HomeEvent.TogglePrayerStatus(PrayerType.ASR))

        coVerify {
            prayerRepository.updatePrayerStatus(
                any(), PrayerName.ASR, PrayerStatus.NOT_PRAYED, null, false
            )
        }
    }

    @Test
    fun `sunrise cannot be tracked, and tapping it records nothing at all`() = withScope {
        // It is the end of Fajr's window, not a prayer. Logging the tap here is what made the
        // engagement dashboard count toggles that never happened.
        val viewModel = viewModel()

        viewModel.onEvent(HomeEvent.TogglePrayerStatus(PrayerType.SUNRISE))

        coVerify(exactly = 0) {
            prayerRepository.updatePrayerStatus(any(), any(), any(), any(), any())
        }
        assertThat(telemetry.prayersTracked).isEmpty()
    }

    @Test
    fun `setting an explicit status writes that status, not a toggle of it`() = withScope {
        val viewModel = viewModel()

        viewModel.onEvent(HomeEvent.SetPrayerStatus(PrayerType.ISHA, PrayerStatus.LATE))

        coVerify {
            prayerRepository.updatePrayerStatus(any(), PrayerName.ISHA, PrayerStatus.LATE, any(), false)
        }
    }

    @Test
    fun `a status that is not prayed records no prayed-at time`() = withScope {
        val viewModel = viewModel()

        viewModel.onEvent(HomeEvent.SetPrayerStatus(PrayerType.FAJR, PrayerStatus.MISSED))

        coVerify {
            prayerRepository.updatePrayerStatus(
                any(), PrayerName.FAJR, PrayerStatus.MISSED, null, false
            )
        }
    }

    @Test
    fun `sunrise cannot be given an explicit status either`() = withScope {
        val viewModel = viewModel()

        viewModel.onEvent(HomeEvent.SetPrayerStatus(PrayerType.SUNRISE, PrayerStatus.PRAYED))

        coVerify(exactly = 0) {
            prayerRepository.updatePrayerStatus(any(), any(), any(), any(), any())
        }
    }

    // ── The announcement banner ─────────────────────────────────────────────────

    @Test
    fun `dismissing an announcement dismisses it once and records that it happened`() = withScope {
        val viewModel = viewModel()
        val collector = launch { viewModel.announcement.collect { } }
        announcements.value = announcement()

        viewModel.onEvent(HomeEvent.DismissAnnouncement)

        coVerify { announcementRepository.dismiss("a1") }
        assertThat(
            telemetry.calls.filterIsInstance<com.arshadshah.nimaz.core.monitoring.TelemetryCall.AnnouncementDismissed>()
                .map { it.id }
        ).contains("a1")
        collector.cancel()
    }

    @Test
    fun `dismissing with nothing on screen is a no-op rather than a crash`() = withScope {
        val viewModel = viewModel()

        viewModel.onEvent(HomeEvent.DismissAnnouncement)

        coVerify(exactly = 0) { announcementRepository.dismiss(any<String>()) }
    }

    @Test
    fun `the CTA is recorded here, but navigation stays the screen's job`() = withScope {
        // The ViewModel owns no NavController by design; recording the click and navigating
        // are deliberately two different owners, and only one of them is testable here.
        val viewModel = viewModel()
        val collector = launch { viewModel.announcement.collect { } }
        announcements.value = announcement(ctaLabel = "Read it", route = "settings")

        viewModel.onEvent(HomeEvent.AnnouncementCtaClicked)

        assertThat(
            telemetry.calls.filterIsInstance<com.arshadshah.nimaz.core.monitoring.TelemetryCall.AnnouncementCtaClicked>()
        ).isNotEmpty()
        collector.cancel()
    }

    @Test
    fun `a CTA click with nothing on screen records nothing`() = withScope {
        viewModel().onEvent(HomeEvent.AnnouncementCtaClicked)

        assertThat(
            telemetry.calls.filterIsInstance<com.arshadshah.nimaz.core.monitoring.TelemetryCall.AnnouncementCtaClicked>()
        ).isEmpty()
    }

    @Test
    fun `an announcement is recorded as shown once, not on every re-emission`() = withScope {
        val viewModel = viewModel()
        val collector = launch { viewModel.announcement.collect { } }

        announcements.value = announcement()
        announcements.value = announcement()
        announcements.value = announcement()

        assertThat(telemetry.announcementsShown.count { it.id == "a1" }).isEqualTo(1)
        collector.cancel()
    }

    @Test
    fun `a celebration is kept out of the banner, because it renders as a card`() = withScope {
        // Showing it in both places double-renders the same occasion on one screen.
        val viewModel = viewModel()
        val collector = launch { viewModel.announcement.collect { } }

        announcements.value = announcement(type = AnnouncementType.CELEBRATION)

        assertThat(viewModel.announcement.value.announcement).isNull()
        collector.cancel()
    }

    @Test
    fun `a CTA whose route does not resolve is shown without a button`() = withScope {
        // A button that goes nowhere is worse than no button: the reader taps it and nothing
        // happens, with no way to tell whether the app or the announcement is broken.
        val viewModel = viewModel()
        val collector = launch { viewModel.announcement.collect { } }

        announcements.value = announcement(ctaLabel = "Go", route = "not-a-real-route")

        assertThat(viewModel.announcement.value.announcement).isNotNull()
        assertThat(viewModel.announcement.value.showCta).isFalse()
        collector.cancel()
    }

    @Test
    fun `an announcement with no CTA label never offers one`() = withScope {
        val viewModel = viewModel()
        val collector = launch { viewModel.announcement.collect { } }

        announcements.value = announcement(ctaLabel = null, route = "settings")

        assertThat(viewModel.announcement.value.showCta).isFalse()
        collector.cancel()
    }

    // ── Permissions ─────────────────────────────────────────────────────────────

    @Test
    fun `refreshing permissions re-reads all three prerequisites`() = withScope {
        // Called back from every permission launcher on Home; a stale read leaves the warning
        // banner up after the user has just granted the thing it asks for.
        val viewModel = viewModel()

        viewModel.onEvent(HomeEvent.RefreshPermissions)

        val state = viewModel.state.value
        assertThat(state.hasNotificationPermission).isTrue()
        assertThat(state.hasLocationPermission).isTrue()
        assertThat(state.isBatteryOptimized).isFalse()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun hadith(
        id: String = "h1",
        textEnglish: String = "Actions are by intentions.",
        grade: HadithGrade? = HadithGrade.SAHIH,
        reference: String? = "Bukhari 1",
    ) = Hadith(
        id = id,
        bookId = "bukhari",
        chapterId = "1",
        hadithNumber = 1,
        hadithNumberInBook = 1,
        textArabic = "إنما الأعمال بالنيات",
        textEnglish = textEnglish,
        narratorChain = null,
        narratorName = null,
        grade = grade,
        gradeArabic = null,
        reference = reference,
    )

    private fun category() = DuaCategory(
        id = "1",
        nameArabic = "أذكار الصباح",
        nameEnglish = "Morning adhkar",
        description = null,
        iconName = "sun",
        displayOrder = 1,
        duaCount = 1,
    )

    private fun dua(reference: String? = "Hisn al-Muslim") = Dua(
        id = "dua-1",
        categoryId = "1",
        titleArabic = "ذكر",
        titleEnglish = "Morning remembrance",
        textArabic = "اللهم بك أصبحنا",
        textTransliteration = null,
        textEnglish = "O Allah, by You we enter the morning",
        reference = reference,
        occasion = null,
        benefits = null,
        repeatCount = null,
        audioUrl = null,
        displayOrder = 1,
    )

    private fun announcement(
        type: AnnouncementType = AnnouncementType.CHANGELOG,
        ctaLabel: String? = null,
        route: String? = null,
    ) = Announcement(
        id = "a1",
        type = type,
        title = "What's new",
        body = "A new version landed",
        ctaLabel = ctaLabel,
        route = route,
    )
}
