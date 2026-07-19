package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.QuranOrnamentalDivider
import com.arshadshah.nimaz.presentation.components.atoms.ShamsaMedallion
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.QuranSurfaceColors
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Which Quran reading surface a [QuranFrame] is dressing.
 *
 * The variant changes **only padding and content spacing** — never the colours,
 * corner radius or ornament. Both variants are the same illuminated frame.
 */
enum class QuranFrameVariant {
    /** Full-bleed mushaf page: fills the available height, content scrolls inside. */
    READER,

    /** Tafseer / study surface: wraps its content, roomier padding for long prose. */
    STUDY,
}

/**
 * The single illuminated frame for every Quran reading surface — one gold outer
 * keyline over a teal echo keyline, an ornamental [QuranOrnamentalDivider] above
 * and below the content, and an optional [ShamsaMedallion] footer.
 *
 * This replaces the two near-identical frames that used to exist (the mushaf's
 * private `MushafFrame` and the tafseer's `TafseerBookFrame`). All colours
 * resolve from [QuranSurfaceColors], so the frame is genuinely light in light
 * mode instead of pinning the dark-manuscript gold.
 *
 * @param variant [QuranFrameVariant.READER] to fill the height, [QuranFrameVariant.STUDY] to wrap
 * @param number when non-null, a shamsa medallion footer showing this number
 *   (the mushaf passes its page number; the study frame passes `null`)
 */
@Composable
fun QuranFrame(
    variant: QuranFrameVariant,
    modifier: Modifier = Modifier,
    number: Int? = null,
    content: @Composable () -> Unit,
) {
    val gold = QuranSurfaceColors.frameGold
    val teal = QuranSurfaceColors.frameTeal

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(QuranSurfaceColors.pageSurface)
            .border(1.5.dp, gold, RoundedCornerShape(5.dp))
            .padding(3.dp)
            .border(1.dp, teal, RoundedCornerShape(3.dp))
            .padding(2.dp)
    ) {
        Column(
            modifier = when (variant) {
                QuranFrameVariant.READER -> Modifier.fillMaxSize()
                QuranFrameVariant.STUDY -> Modifier.fillMaxWidth()
            }
        ) {
            QuranOrnamentalDivider()

            FrameBody(variant = variant, content = content)

            QuranOrnamentalDivider()

            if (number != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ShamsaMedallion(
                        number = number,
                        size = 45.dp,
                        numberColor = QuranSurfaceColors.medallionInk,
                        gold = QuranSurfaceColors.frameGold,
                        numberStyle = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * READER gives the content the remaining height so it can scroll; STUDY simply
 * wraps it with a little extra breathing room for long prose.
 */
@Composable
private fun ColumnScope.FrameBody(
    variant: QuranFrameVariant,
    content: @Composable () -> Unit,
) {
    when (variant) {
        QuranFrameVariant.READER -> Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { content() }

        QuranFrameVariant.STUDY -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) { content() }
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun QuranFrameShowcase() {
    Column(Modifier.padding(16.dp)) {
        QuranFrame(variant = QuranFrameVariant.STUDY, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "In the name of Allah, the Most Gracious, the Most Merciful. " +
                        "This ayah reminds the believer of Allah's boundless mercy and " +
                        "the importance of beginning every undertaking with His name.",
                style = MaterialTheme.typography.bodyMedium,
                color = QuranSurfaceColors.ayahInk,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        QuranFrame(
            variant = QuranFrameVariant.STUDY,
            modifier = Modifier.fillMaxWidth(),
            number = 604
        ) {
            Text(
                text = "A framed page with its shamsa medallion footer.",
                style = MaterialTheme.typography.bodyMedium,
                color = QuranSurfaceColors.ayahInk,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380, name = "QuranFrame — Light")
@Composable
private fun QuranFrameLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { QuranFrameShowcase() }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0A0A08,
    widthDp = 380,
    name = "QuranFrame — Dark"
)
@Composable
private fun QuranFrameDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { QuranFrameShowcase() }
}
