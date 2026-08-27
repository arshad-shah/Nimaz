package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    // The two variants part company here, and deliberately. READER *is* a printed page and
    // takes the paper register: cream ground, one hairline rule, a small medallion at the foot.
    // STUDY is a study surface and keeps the illuminated gold-over-teal frame, which is what
    // the June tafseer design chose and what still reads correctly on a page of prose.
    //
    // Two ornamental registers in one app is a deliberate cost, taken because the mushaf's job
    // is to disappear behind the text and Tafseer's is to frame it.
    if (variant == QuranFrameVariant.READER) {
        PaperFrame(number = number, modifier = modifier, content = content)
        return
    }

    val ground = QuranSurfaceColors.pageSurface
    val rule = QuranSurfaceColors.frameGold

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(ground)
            .border(1.5.dp, QuranSurfaceColors.frameGold, RoundedCornerShape(5.dp))
            .padding(3.dp)
            .border(1.dp, QuranSurfaceColors.frameTeal, RoundedCornerShape(3.dp))
            .padding(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            QuranOrnamentalDivider(color = rule)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) { content() }

            QuranOrnamentalDivider(color = rule)

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
                        gold = rule,
                        teal = rule,
                        numberStyle = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * The mushaf page: a paper card, a keyline **inside** it, and the page number as a small pill
 * straddling the keyline's bottom edge.
 *
 * Two nested rounded rectangles rather than a border and a pair of horizontal rules — which is
 * how a printed mushaf frames its text block, and what the design prototype draws. The medallion
 * is a pill, not a shamsa: a rosette at the foot of every page competes with the ۞ and ع markers
 * that are actually carrying meaning inside it.
 */
@Composable
private fun PaperFrame(
    number: Int?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val rule = QuranSurfaceColors.paperLine
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(QuranSurfaceColors.paper)
            .border(1.dp, rule, RoundedCornerShape(16.dp))
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, rule, RoundedCornerShape(12.dp))
                // Room at the foot for the page pill to sit on the keyline without landing on
                // the last line of Arabic.
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 26.dp),
        ) { content() }

        if (number != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 7.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(QuranSurfaceColors.paper)
                    .border(1.dp, rule, RoundedCornerShape(99.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = QuranSurfaceColors.frameGold,
                )
            }
        }
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
