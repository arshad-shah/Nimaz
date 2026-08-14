package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.presentation.components.atoms.NavArrowDirection
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazNavArrowButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazProgressSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazProgressTrack
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@Composable
internal fun MushafPageBar(
    pageNumber: Int,
    secondPageNumber: Int? = null,
    totalPages: Int,
    ayahs: List<Ayah>,
    isKhatamActive: Boolean,
    khatamReadAyahIds: Set<Int>,
    onKhatamTogglePage: (List<Ayah>) -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstAyah = ayahs.firstOrNull()
    val juzNumber = firstAyah?.juz ?: 0
    // Derived from the quarter counter, never read off `Ayah.hizbNumber`: that column holds the
    // 1..240 *quarter* index in the shipped data, so page 82 reported "hizb 33" for what is
    // hizb 9. `hizbOfQuarter` divides the quarter counter down to the 1..60 hizb, and the badge
    // is simply absent on a device whose `hizb_quarters` table has not landed rather than
    // falling back to the column that lies.
    val hizbNumber = firstAyah?.takeIf { it.rubNumber > 0 }?.hizbOfQuarter ?: 0

    Surface(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Next page (higher Quran page number — leftward in mushaf, so a left-pointing arrow)
            NimazNavArrowButton(
                direction = NavArrowDirection.PREVIOUS,
                enabled = (secondPageNumber ?: pageNumber) < totalPages,
                onClick = onNavigateNext,
                contentDescription = stringResource(R.string.cd_next_page),
                size = 40.dp
            )

            // One line, not three chips: "Page 293 · juz 15 · hizb 30". The page number leads
            // in full weight and the coordinates follow it in the muted colour, because they
            // are context for the page rather than three equal facts — which is what a row of
            // identically-weighted badges made them look like.
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (secondPageNumber != null)
                            stringResource(R.string.page_range_format, pageNumber, secondPageNumber)
                        else
                            stringResource(R.string.page_single_format, pageNumber),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    val coordinates = listOfNotNull(
                        juzNumber.takeIf { it > 0 }
                            ?.let { stringResource(R.string.khatam_juz_position, it) },
                        hizbNumber.takeIf { it > 0 }
                            ?.let { stringResource(R.string.hizb_format, it) },
                    )
                    if (coordinates.isNotEmpty()) {
                        Text(
                            text = " · " + coordinates.joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // Where this page sits in the whole mushaf, as one hairline under the label.
                // The page number alone answers "which page"; it does not answer "how far in",
                // and a reader turning pages in a 604-page book wants both. Across the *active*
                // edition's count, so a 548-page IndoPak reads as full at 548, not at 604.
                NimazProgressTrack(
                    progress = if (totalPages > 0) {
                        (secondPageNumber ?: pageNumber).toFloat() / totalPages
                    } else {
                        0f
                    },
                    tone = NimazTone.ACCENT,
                    size = NimazProgressSize.THIN,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }

            // Khatam toggle for the page
            if (isKhatamActive && ayahs.isNotEmpty()) {
                val pageAyahIds = ayahs.map { it.id }.toSet()
                val allPageRead = pageAyahIds.all { it in khatamReadAyahIds }

                IconButton(
                    onClick = { if (!allPageRead) onKhatamTogglePage(ayahs) },
                    enabled = !allPageRead,
                    modifier = Modifier.size(40.dp)
                ) {
                    NimazIcon(
                        imageVector = if (allPageRead) Icons.Filled.CheckCircle
                        else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (allPageRead) stringResource(R.string.cd_page_read)
                        else stringResource(R.string.cd_mark_page_as_read),
                        tint = if (allPageRead) NimazColors.Success
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        iconSize = 22.dp
                    )
                }
            }

            // Previous page (lower Quran page number — rightward in mushaf, so a right-pointing arrow)
            NimazNavArrowButton(
                direction = NavArrowDirection.NEXT,
                enabled = pageNumber > 1,
                onClick = onNavigatePrevious,
                contentDescription = stringResource(R.string.cd_previous_page),
                size = 40.dp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MushafPageBarPreview() {
    NimazTheme {
        MushafPageBar(
            pageNumber = 15,
            totalPages = 604,
            ayahs = listOf(
                Ayah(
                    id = 100,
                    surahNumber = 2,
                    ayahNumber = 94,
                    textArabic = "",
                    textSimple = "",
                    juzNumber = 1,
                    hizbNumber = 33,
                    rubNumber = 33,
                    pageNumber = 15,
                    sajdaType = null,
                    sajdaNumber = null
                )
            ),
            isKhatamActive = false,
            khatamReadAyahIds = emptySet(),
            onKhatamTogglePage = {},
            onNavigatePrevious = {},
            onNavigateNext = {}
        )
    }
}
