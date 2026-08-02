package com.arshadshah.nimaz.presentation.viewmodel

import android.content.Context
import android.os.Vibrator
import android.os.VibratorManager
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.TasbihUseCases
import com.google.common.truth.Truth.assertThat
import com.arshadshah.nimaz.domain.model.TasbihSession
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The dhikr category filter must survive a change to the preset list.
 *
 * `filteredPresets` was **stored** state, recomputed by hand at each of three mutation sites.
 * Two of them — the Room collectors in `loadPresets` — rebuilt it as `defaults + customs` and
 * never consulted `selectedCategory`. So adding, editing or deleting a custom dhikr while a
 * category was selected re-emitted the presets flow and silently reset the list to *everything*,
 * while the category chip carried on looking selected. The user sees a filter that is on and a
 * list that ignores it.
 *
 * Derived state that is stored has to be recomputed at every site that touches an input, and
 * one site forgot. These tests pin the behaviour; the fix makes it a computed property, so
 * there is no site left to forget.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasbihViewModelPresetFilterTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var tasbihUseCases: TasbihUseCases
    private lateinit var preferences: SettingsRepository
    private lateinit var context: Context

    private val defaults = MutableStateFlow(
        listOf(
            preset(1, "Subhanallah", TasbihCategory.DAILY),
            preset(2, "After Fajr", TasbihCategory.AFTER_PRAYER)
        )
    )
    private val customs = MutableStateFlow(listOf(preset(10, "My dhikr", TasbihCategory.CUSTOM)))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        tasbihUseCases = mockk(relaxed = true)
        preferences = mockk(relaxed = true)

        // The ViewModel resolves a Vibrator from Context in its *constructor*, so the mock has
        // to answer that before any of this can be exercised. (That constructor-time system
        // service lookup is itself why this ViewModel is awkward to test.)
        context = mockk(relaxed = true)
        val vibratorManager = mockk<VibratorManager>(relaxed = true)
        every { vibratorManager.defaultVibrator } returns mockk(relaxed = true)
        every { context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) } returns vibratorManager
        every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns
                mockk<Vibrator>(relaxed = true)

        every { preferences.tasbihBeadMode } returns flowOf(false)
        every { preferences.tasbihBeadDesign } returns flowOf("")
        every { preferences.tasbihSelectedPresetId } returns flowOf(-1L)
        every { preferences.tasbihFavorites } returns flowOf(emptySet())
        every { preferences.tasbihLeftHanded } returns flowOf(false)
        // init calls .first() on this one, which throws on the relaxed mock's empty flow.
        // A version at the ceiling means the one-time default-preset seed is skipped.
        every { preferences.tasbihPresetSeedVersion } returns flowOf(Int.MAX_VALUE)

        every { tasbihUseCases.getDefaultPresets() } returns defaults
        every { tasbihUseCases.getCustomPresets() } returns customs
        coEvery { tasbihUseCases.getActiveSession() } returns null
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TasbihViewModel(tasbihUseCases, preferences, context)

    @Test
    fun `adding a custom dhikr keeps the selected category filter applied`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.FilterByCategory(TasbihCategory.DAILY))
        advanceUntilIdle()
        assertThat(vm.presetsState.value.filteredPresets.map { it.id }).containsExactly(1L)

        // The user saves a new custom dhikr; Room re-emits the customs flow.
        customs.value = customs.value + preset(11, "Another", TasbihCategory.CUSTOM)
        advanceUntilIdle()

        assertThat(vm.presetsState.value.selectedCategory).isEqualTo(TasbihCategory.DAILY)
        assertThat(vm.presetsState.value.filteredPresets.map { it.id }).containsExactly(1L)
    }

    @Test
    fun `a defaults re-emission keeps the selected category filter applied`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.FilterByCategory(TasbihCategory.AFTER_PRAYER))
        advanceUntilIdle()
        assertThat(vm.presetsState.value.filteredPresets.map { it.id }).containsExactly(2L)

        defaults.value = defaults.value + preset(3, "After Isha", TasbihCategory.AFTER_PRAYER)
        advanceUntilIdle()

        assertThat(vm.presetsState.value.selectedCategory).isEqualTo(TasbihCategory.AFTER_PRAYER)
        assertThat(vm.presetsState.value.filteredPresets.map { it.id }).containsExactly(2L, 3L)
    }

    @Test
    fun `clearing the filter shows every preset again`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(TasbihEvent.FilterByCategory(TasbihCategory.DAILY))
        advanceUntilIdle()
        vm.onEvent(TasbihEvent.FilterByCategory(null))
        advanceUntilIdle()

        assertThat(vm.presetsState.value.filteredPresets.map { it.id })
            .containsExactly(1L, 2L, 10L)
    }

    @Test
    fun `deleting the last preset of the selected category leaves an empty list, not everything`() =
        runTest {
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(TasbihEvent.FilterByCategory(TasbihCategory.CUSTOM))
            advanceUntilIdle()
            assertThat(vm.presetsState.value.filteredPresets.map { it.id }).containsExactly(10L)

            customs.value = emptyList()
            advanceUntilIdle()

            assertThat(vm.presetsState.value.filteredPresets).isEmpty()
        }

    @Test
    fun `restoring a session whose target count is zero does not crash the counter`() = runTest {
        // `count = currentCount % targetCount` runs at init against a stored row. Every writer
        // coerces the target to >= 1, but a legacy or imported row need not have, and an
        // ArithmeticException here kills the whole init coroutine — the counter comes up dead
        // rather than merely wrong.
        //
        // Restoring a session also starts the elapsed-time ticker, an unbounded
        // `while (isActive) { delay(1000) }`. runTest drains virtual time when the body
        // returns, so that loop has to be stopped or the test never finishes — advance a
        // bounded amount, assert, then pause the session.
        coEvery { tasbihUseCases.getActiveSession() } returns session(targetCount = 0, count = 7)

        val vm = viewModel()
        advanceTimeBy(100)
        runCurrent()

        assertThat(vm.counterState.value.currentSession).isNotNull()
        assertThat(vm.counterState.value.count).isEqualTo(0)

        vm.onEvent(TasbihEvent.PauseSession)
        runCurrent()
    }
}

private fun session(targetCount: Int, count: Int) = TasbihSession(
    id = 1L,
    presetId = null,
    presetName = null,
    date = 0L,
    currentCount = count,
    targetCount = targetCount,
    totalLaps = 0,
    isCompleted = false,
    duration = null,
    startedAt = 0L,
    completedAt = null,
    note = null
)

private fun preset(id: Long, name: String, category: TasbihCategory) = TasbihPreset(
    id = id,
    name = name,
    arabicText = null,
    transliteration = null,
    translation = null,
    targetCount = 33,
    category = category,
    reference = null,
    isDefault = id < 10,
    displayOrder = id.toInt(),
    createdAt = 0L,
    updatedAt = 0L
)
