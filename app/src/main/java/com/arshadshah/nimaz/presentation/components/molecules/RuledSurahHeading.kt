package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.BISMILLAH_TEXT
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.QuranSurfaceColors
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A surah's opening, as a printed mushaf sets it: **hairline, name, hairline**.
 *
 * The paper page used [SurahHeaderCartouche] — a saturated teal panel with a gold ogee stroke,
 * a scalloped shamsa medallion and a bud finial. That ornament is a manuscript object and it is
 * beautiful, but it is a *card* sitting on a page that is trying to be paper: it pins the brand
 * ramps regardless of theme, so on a cream ground it lands as a dark plaque with the surah's
 * English name and a "Meccan" badge — chrome, in the middle of the text block.
 *
 * A printed mushaf does none of that. It rules the heading, sets the surah's name in Arabic
 * between the rules, and prints the Basmala on its own line beneath. Everything here is drawn
 * in the page's own [QuranSurfaceColors.paperLine] and [QuranSurfaceColors.paperInk], so the
 * heading belongs to the page rather than floating over it, with a wash of the page gold to
 * mark it as an opening.
 *
 * The cartouche is **not** retired: it still opens the translation reader, where the surface is
 * a card and an illuminated header is exactly right.
 */
@Composable
fun RuledSurahHeading(
    arabicName: String,
    modifier: Modifier = Modifier,
    showBismillah: Boolean = true,
) {
    val rule = QuranSurfaceColors.paperLine
    val gold = QuranSurfaceColors.frameGold
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                // A wash of gold rather than a plaque of it: enough to say "a surah opens here"
                // on a cream page, not enough to become the loudest thing on it.
                .background(gold.copy(alpha = 0.07f))
                .border(1.dp, rule, RoundedCornerShape(11.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                thickness = 1.dp,
                color = rule,
                modifier = Modifier.weight(1f),
            )
            ArabicText(
                text = arabicName,
                size = ArabicTextSize.SMALL,
                color = gold,
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = rule,
                modifier = Modifier.weight(1f),
            )
        }

        if (showBismillah) {
            ArabicText(
                text = BISMILLAH_TEXT,
                size = ArabicTextSize.MEDIUM,
                // Slightly held back from the body ink: the Basmala opens the surah, it is not
                // one of its verses.
                color = QuranSurfaceColors.paperInk.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** Convenience overload — the Basmala rule (all but Al-Fātiḥah and At-Tawbah) applied for you. */
@Composable
fun RuledSurahHeading(
    surah: Surah,
    modifier: Modifier = Modifier,
    showBismillah: Boolean = surah.number != 1 && surah.number != 9,
) {
    RuledSurahHeading(
        arabicName = surah.nameArabic,
        modifier = modifier,
        showBismillah = showBismillah,
    )
}

@Preview(showBackground = true, widthDp = 380, name = "RuledSurahHeading — Light")
@Composable
private fun RuledSurahHeadingLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            RuledSurahHeading(surah = sampleSurahFatihah)
            RuledSurahHeading(surah = sampleSurahBaqarah)
        }
    }
}

@Preview(
    showBackground = true, widthDp = 380, name = "RuledSurahHeading — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun RuledSurahHeadingDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            RuledSurahHeading(surah = sampleSurahFatihah)
            RuledSurahHeading(surah = sampleSurahBaqarah)
        }
    }
}
