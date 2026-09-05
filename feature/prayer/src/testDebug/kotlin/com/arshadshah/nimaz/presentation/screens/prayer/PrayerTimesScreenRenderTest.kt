package com.arshadshah.nimaz.presentation.screens.prayer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import com.arshadshah.nimaz.presentation.viewmodel.prayer.PrayerTimesUiState
import com.arshadshah.nimaz.presentation.viewmodel.prayer.PrayerTimesViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.ZoneId

/**
 * Draws the whole screen and writes it to `feature/prayer/build/reports/prayerTimes/`.
 *
 * **Why a test writes PNGs.** Three defects in this screen were invisible to the 29 assertions in
 * `PrayerTimesScreenTest` and were caught by looking at one of these renders: the daylight figure
 * stated three times in one scroll, twice of them twenty dp apart; a solar card toned `ACCENT`,
 * which fills with the primary hue in dark and swallowed the arc's daylight limb, drawn from that
 * same hue; and — in the render harness itself — a fixture that set a future date with
 * `isToday = true`, so no prayer resolved as passed and the "today" render was quietly the
 * other-day render in disguise.
 *
 * None of those fail a semantics assertion. All three change what a reader sees.
 *
 * It asserts as well as renders — a blank bitmap is a failure, not a quiet no-op — so it earns a
 * place in the suite rather than being a script that happens to live here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1400dp-mdpi")
class PrayerTimesScreenRenderTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File("build/reports/prayerTimes")

    /**
     * A real today, so prayers resolve as passed against the wall clock and the window card is
     * actually exercised. A fixed date with `isToday = true` renders the wrong branch.
     */
    private val realToday: LocalDate = LocalDate.now()

    /** The next Friday, so the browsed-day render carries Dhuhr's Jumu'ah qualifier. */
    private val friday: LocalDate = realToday.plusDays(
        ((5 - realToday.dayOfWeek.value + 7) % 7).toLong().let { if (it == 0L) 7L else it }
    )

    private fun at(date: LocalDate, hour: Int, minute: Int): kotlin.time.Instant =
        kotlin.time.Instant.fromEpochMilliseconds(
            date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

    private fun prayers(date: LocalDate) = listOf(
        PrayerTimeDisplay(PrayerType.FAJR, "Fajr", at(date, 5, 12)),
        PrayerTimeDisplay(PrayerType.SUNRISE, "Sunrise", at(date, 6, 48)),
        PrayerTimeDisplay(PrayerType.DHUHR, "Dhuhr", at(date, 13, 22)),
        PrayerTimeDisplay(PrayerType.ASR, "Asr", at(date, 17, 13)),
        PrayerTimeDisplay(PrayerType.MAGHRIB, "Maghrib", at(date, 20, 4)),
        PrayerTimeDisplay(PrayerType.ISHA, "Isha", at(date, 21, 38)),
    )

    private fun stateFor(date: LocalDate, isToday: Boolean) = PrayerTimesUiState(
        locationName = "Dublin, Ireland",
        selectedDate = date,
        isToday = isToday,
        prayers = prayers(date),
        tomorrowFajrAt = at(date.plusDays(1), 5, 14),
        moonFraction = 0.62f,
        sunriseFraction = 0.283f,
        sunsetFraction = 0.836f,
        sunriseAt = at(date, 6, 48),
        sunsetAt = at(date, 20, 4),
        daylight = "13h 16m",
        methodLabel = "Muslim World League · Standard",
    )

    private fun render(themeMode: ThemeMode, isToday: Boolean, fileName: String) {
        val date = if (isToday) realToday else friday
        val flow = MutableStateFlow(stateFor(date, isToday))
        val viewModel: PrayerTimesViewModel = mockk(relaxed = true) {
            every { state } returns flow
        }

        composeRule.setContent {
            NimazTheme(themeMode = themeMode) {
                PrayerTimesScreen(
                    onNavigateBack = {},
                    onNavigateToSettings = {},
                    viewModel = viewModel,
                )
            }
        }
        composeRule.waitForIdle()

        val root: View = composeRule.activity
            .findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0)
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))

        outDir.mkdirs()
        val file = File(outDir, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        // Under the legacy shadow canvas every draw is a no-op and the PNG is written blank, so
        // the render only means something if the pixels are checked.
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertThat(pixels.toSet().size).isGreaterThan(50)
        assertThat(file.length()).isGreaterThan(0L)
    }

    @Test
    fun `renders today in light theme`() =
        render(ThemeMode.LIGHT, isToday = true, "prayer-times-today-light.png")

    @Test
    fun `renders today in dark theme`() =
        render(ThemeMode.DARK, isToday = true, "prayer-times-today-dark.png")

    @Test
    fun `renders a browsed day`() =
        render(ThemeMode.LIGHT, isToday = false, "prayer-times-other-day.png")
}
