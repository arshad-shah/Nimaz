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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/** A curated surah recommendation paired with the reason it is worth reading. */
private data class SurahRecommendation(
    val surahNumber: Int,
    @StringRes val reasonRes: Int
)

/** Surahs with well-known virtues. Al-Kahf is surfaced first on Fridays. */
private val RECOMMENDATIONS = listOf(
    SurahRecommendation(18, R.string.quran_home_reason_friday),        // Al-Kahf
    SurahRecommendation(67, R.string.quran_home_reason_before_sleep),  // Al-Mulk
    SurahRecommendation(36, R.string.quran_home_reason_heart),         // Ya-Sin
    SurahRecommendation(55, R.string.quran_home_reason_mercy),         // Ar-Rahman
    SurahRecommendation(56, R.string.quran_home_reason_provision)      // Al-Waqi'ah
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
    modifier: Modifier = Modifier
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
    onClick: () -> Unit
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = surah.number.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = surah.nameEnglish,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            ArabicText(
                text = surah.nameArabic,
                size = ArabicTextSize.SMALL,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuranRecommendedSurahsPreview() {
    NimazTheme {
        QuranRecommendedSurahs(
            surahs = listOf(
                Surah(18, "الكهف", "Al-Kahf", "The Cave", RevelationType.MECCAN, 110, 15, 18, 293),
                Surah(67, "الملك", "Al-Mulk", "The Sovereignty", RevelationType.MECCAN, 30, 29, 67, 562)
            ),
            isFriday = true,
            onSurahClick = {}
        )
    }
}
