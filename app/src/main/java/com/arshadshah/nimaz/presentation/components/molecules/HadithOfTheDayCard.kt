package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.arshadshah.nimaz.presentation.components.atoms.IconBadge
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
    grade: String? = null,
    onClick: (() -> Unit)? = null,
    fillHeight: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
            // Tapping opens this exact hadith in the reader (issue #161). Kept on
            // the whole card so the large surface is the tap target, not a tiny link.
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    imageVector = Icons.Default.Book,
                    backgroundColor = HadithAccent.copy(alpha = 0.2f),
                    iconColor = HadithAccent,
                    containerSize = 32.dp,
                    iconSize = 18.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.hadith_of_the_day),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!grade.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    GradeChip(grade)
                }
                if (onClick != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.read),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = HadithAccent
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = HadithAccent,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(14.dp)
                    )
                }
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

/**
 * Small authenticity-grade pill (Sahih / Hasan / Da'if …). Colour-coded to
 * match the grade badge in the hadith reader so the same grade reads the same
 * everywhere. Self-contained (matches on the display string) so this card stays
 * decoupled from the HadithGrade enum.
 */
@Composable
private fun GradeChip(grade: String) {
    val color = when (grade.trim().lowercase()) {
        "sahih" -> Color(0xFF4CAF50)
        "hasan" -> Color(0xFF8BC34A)
        "da'if", "daif", "dai'f" -> Color(0xFFFF9800)
        "mawdu", "mawdu'", "fabricated" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = grade,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
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
