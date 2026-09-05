package com.arshadshah.nimaz.presentation.components.atoms

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import com.arshadshah.nimaz.testing.allPixels
import com.arshadshah.nimaz.testing.drawToBitmap
import com.arshadshah.nimaz.testing.ink
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the solar arc in three real states and writes the result to
 * `core/ui/build/reports/solarArc/` for a human to look at.
 *
 * **Why a test writes PNGs.** Two defects in this component were invisible to every other kind of
 * check and were caught by looking at one of these renders. The night limb was painted with
 * `surfaceVariant` — a *container* token used for a stroke, so the dashed curve vanished into the
 * card and Fajr and Isha read as two orphaned dots. And `litSpan` was drawn in the dusk tone on a
 * stretch of curve the dawn-to-dusk gradient had already made that colour, so the one thing the
 * card exists to say — *you are inside this window* — was carried by the text alone.
 *
 * Neither is reachable from the semantics tree (`clearAndSetSemantics` collapses the whole arc to
 * one node), neither changes the ink count an assertion could sum, and both render without error.
 * A `@Preview` would show them, but only to someone with the IDE open. This puts the same picture
 * in `build/` on every run.
 *
 * It still asserts — a render that comes back blank is a failure, not a quiet no-op — so it earns
 * its place in the suite rather than being a script that happens to live here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class NimazSolarArcPreviewRenderTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File("build/reports/solarArc")

    /** Dublin, early September. */
    private val september = listOf(
        NimazSolarNode(0.216f, "Fajr", NimazTone.MUTED, "Fajr"),
        NimazSolarNode(0.283f, null, NimazTone.ACCENT, "Sunrise"),
        NimazSolarNode(0.557f, "Dhuhr", NimazTone.PROMINENT, "Dhuhr"),
        NimazSolarNode(0.718f, "Asr", NimazTone.WARNING, "Asr"),
        NimazSolarNode(0.836f, null, NimazTone.WARNING, "Maghrib"),
        NimazSolarNode(0.901f, "Isha", NimazTone.MUTED, "Isha"),
    )

    /** Dublin, late December — the short day the night compression has to survive. */
    private val december = listOf(
        NimazSolarNode(0.28f, "Fajr", NimazTone.MUTED, "Fajr"),
        NimazSolarNode(0.35f, null, NimazTone.ACCENT, "Sunrise"),
        NimazSolarNode(0.525f, "Dhuhr", NimazTone.PROMINENT, "Dhuhr"),
        NimazSolarNode(0.62f, "Asr", NimazTone.WARNING, "Asr"),
        NimazSolarNode(0.70f, null, NimazTone.WARNING, "Maghrib"),
        NimazSolarNode(0.76f, "Isha", NimazTone.MUTED, "Isha"),
    )

    @Composable
    private fun ArcCard(
        lede: String,
        headline: String,
        nodes: List<NimazSolarNode>,
        sunrise: Float,
        sunset: Float,
        footer: String,
        sun: Float? = null,
        span: ClosedFloatingPointRange<Float>? = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(14.dp)
        ) {
            Text(
                text = lede,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            NimazSolarArc(
                nodes = nodes,
                sunriseFraction = sunrise,
                sunsetFraction = sunset,
                contentDescription = "The sun's day",
                sunPosition = sun,
                litSpan = span,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = footer,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    @Composable
    private fun SectionLabel(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    @Composable
    private fun Sheet() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel("TODAY — inside Dhuhr's window")
            ArcCard(
                lede = "You are in the window of",
                headline = "Dhuhr",
                nodes = september,
                sunrise = 0.283f,
                sunset = 0.836f,
                sun = 0.605f,
                span = 0.557f..0.718f,
                footer = "started 13:22          Asr in 2h 41m",
            )

            SectionLabel("ANOTHER DAY — no sun")
            ArcCard(
                lede = "The day's arc",
                headline = "12h 43m of daylight",
                nodes = september,
                sunrise = 0.283f,
                sunset = 0.836f,
                footer = "Sunrise 07:01          Sunset 19:44",
            )

            SectionLabel("DECEMBER — a short day, a deep night")
            ArcCard(
                lede = "The day's arc",
                headline = "7h 24m of daylight",
                nodes = december,
                sunrise = 0.35f,
                sunset = 0.70f,
                sun = 0.45f,
                span = 0.525f..0.62f,
                footer = "Sunrise 08:39          Sunset 16:07",
            )
        }
    }

    private fun render(themeMode: ThemeMode, fileName: String) {
        val bitmap: Bitmap = composeRule.drawToBitmap {
            NimazTheme(themeMode = themeMode) { Sheet() }
        }

        outDir.mkdirs()
        FileOutputStream(File(outDir, fileName)).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        // A blank render is the failure this guards against: under the legacy shadow canvas every
        // draw is a no-op, and the PNG would be written and be uniformly empty.
        assertThat(bitmap.allPixels().ink()).isGreaterThan(1_000)
        assertThat(File(outDir, fileName).length()).isGreaterThan(0L)
    }

    @Test
    fun `renders the arc in light theme`() {
        render(ThemeMode.LIGHT, "solar-arc-light.png")
    }

    @Test
    fun `renders the arc in dark theme`() {
        render(ThemeMode.DARK, "solar-arc-dark.png")
    }
}
