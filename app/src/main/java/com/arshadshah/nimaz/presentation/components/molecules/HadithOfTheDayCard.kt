package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * "Hadith of the day" card: titled header with an indigo book accent and the
 * day's hadith text.
 *
 * In the carousel ([fillHeight] = true, [maxLines] limited) the body
 * ellipsizes so every page is the same height. Standalone usage lets the text
 * expand naturally.
 */
@Composable
fun HadithOfTheDayCard(
    hadith: String,
    modifier: Modifier = Modifier,
    reference: String? = null,
    fillHeight: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
                .padding(start = 16.dp, end= 16.dp, top = 12.dp, bottom = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HadithAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = HadithAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.hadith_of_the_day),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // In fill-height mode the body expands to absorb leftover space
            // so the card doesn't look top-clumped on tall pager pages.
            Text(
                text = hadith,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                lineHeight = 22.sp,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = if (fillHeight) {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            if (!reference.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = reference,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = HadithAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }
        }
    }
}

private val HadithAccent = Color(0xFF3B82F6)

private const val SAMPLE_HADITH =
    "The Prophet (peace be upon him) said: \"The best of you are those who learn the Quran and teach it.\" — Sahih al-Bukhari"

@Preview(showBackground = true, widthDp = 400, name = "Standalone")
@Composable
private fun HadithOfTheDayCard_Preview() {
    NimazTheme {
        HadithOfTheDayCard(
            hadith = SAMPLE_HADITH,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 200, name = "Carousel mode (capped)")
@Composable
private fun HadithOfTheDayCard_Carousel_Preview() {
    NimazTheme {
        HadithOfTheDayCard(
            hadith = SAMPLE_HADITH + " " + SAMPLE_HADITH,
            fillHeight = true,
            maxLines = 4,
            modifier = Modifier.padding(16.dp)
        )
    }
}
