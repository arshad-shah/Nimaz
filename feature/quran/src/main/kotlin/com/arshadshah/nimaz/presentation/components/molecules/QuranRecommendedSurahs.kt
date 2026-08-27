package com.arshadshah.nimaz.presentation.components.molecules

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/** A curated surah recommendation paired with the reason it is worth reading. */
private data class SurahRecommendation(
    val surahNumber: Int,
    @StringRes val reasonRes: Int
)

/** Surahs with well-known virtues. Al-Kahf is surfaced first on Fridays. */
private val RECOMMENDATIONS = listOf(
    SurahRecommendation(18, R.string.quran_home_reason_friday),           // Al-Kahf
    SurahRecommendation(67, R.string.quran_home_reason_before_sleep),     // Al-Mulk
    SurahRecommendation(36, R.string.quran_home_reason_heart),            // Ya-Sin
    SurahRecommendation(55, R.string.quran_home_reason_mercy),            // Ar-Rahman
    SurahRecommendation(56, R.string.quran_home_reason_provision),        // Al-Waqi'ah
    SurahRecommendation(1, R.string.quran_home_reason_greatest),          // Al-Fatihah
    SurahRecommendation(112, R.string.quran_home_reason_third),           // Al-Ikhlas
    SurahRecommendation(113, R.string.quran_home_reason_refuge_evil),     // Al-Falaq
    SurahRecommendation(114, R.string.quran_home_reason_refuge_whispers)  // An-Nas
)

/**
 * Horizontally scrolling row of recommended surahs with their virtue. On Fridays the
 * Al-Kahf recommendation is moved to the front to match the Sunnah of reading it.
 */
@Composable
internal fun QuranRecommendedSurahs(
    surahs: List<Surah>,
    isFriday: Boolean,
    onSurahClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * The active edition's page mapping, which is what actually knows a surah's juz. Every card
     * used to read `Surah.juzStart` and so said "Juz 1" — Al-Kahf and Al-Mulk included, since
     * the mapper filled that field with a literal 1 for all 114 rows.
     */
    pagination: MushafPagination = MushafPagination.fallback(MushafScript.DEFAULT),
) {
    val ordered = remember(isFriday) {
        if (isFriday) {
            RECOMMENDATIONS.sortedByDescending { it.surahNumber == 18 }
        } else {
            RECOMMENDATIONS
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = ordered,
            key = { it.surahNumber }
        ) { rec ->
            val surah = surahs.find { it.number == rec.surahNumber }
            if (surah != null) {
                RecommendedSurahCard(
                    surah = surah,
                    reason = stringResource(rec.reasonRes),
                    juzNumber = pagination.juzForPage(surah.startPage),
                    onClick = { onSurahClick(surah.number) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendedSurahCard(
    surah: Surah,
    reason: String,
    juzNumber: Int,
    onClick: () -> Unit
) {
    // The card carries one spoken label for the whole tile. The number is only a
    // decorative numeral visually, but it still belongs in the announcement —
    // scanning by surah number is a real way people navigate the Quran.
    val spokenLabel = stringResource(
        R.string.quran_home_recommended_a11y,
        surah.nameEnglish,
        surah.number,
        reason
    )
    NimazCard(
        style = NimazCardStyle.ELEVATED,
        onClick = onClick,
        modifier = Modifier
            .width(144.dp)
            .semantics(mergeDescendants = true) { contentDescription = spokenLabel },
        shape = RoundedCornerShape(16.dp),
        tone = NimazTone.NEUTRAL
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // The surah number recedes into the card as a large ghost numeral.
            // It is the least useful thing on the card, so it stops competing
            // with the name — but stays available for anyone scanning by number.
            Text(
                text = surah.number.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = GHOST_NUMERAL_ALPHA),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 6.dp)
                    .clearAndSetSemantics { }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // The reason leads: it is why this surah is being suggested.
                NimazBadge(
                    text = reason,
                    tone = NimazTone.ACCENT,
                    size = NimazBadgeSize.SMALL
                )

                Spacer(modifier = Modifier.height(14.dp))

                ArabicText(
                    text = surah.nameArabic,
                    size = ArabicTextSize.MEDIUM,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = stringResource(
                        R.string.quran_home_recommended_meta,
                        pluralStringResource(
                            R.plurals.quran_home_verses_count,
                            surah.ayahCount,
                            surah.ayahCount
                        ),
                        juzNumber
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Opacity of the decorative ghost numeral behind each recommended surah card. */
private const val GHOST_NUMERAL_ALPHA = 0.3f

@Preview(showBackground = true)
@Composable
private fun QuranRecommendedSurahsPreview() {
    NimazTheme {
        QuranRecommendedSurahs(
            surahs = listOf(
                Surah(18, "الكهف", "Al-Kahf", "The Cave", RevelationType.MECCAN, 110, 18, 293),
                Surah(
                    67,
                    "الملك",
                    "Al-Mulk",
                    "The Sovereignty",
                    RevelationType.MECCAN,
                    30,
                    67,
                    562
                )
            ),
            isFriday = true,
            onSurahClick = {}
        )
    }
}
