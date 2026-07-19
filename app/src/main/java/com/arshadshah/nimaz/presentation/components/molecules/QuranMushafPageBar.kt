package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.presentation.components.atoms.NavArrowDirection
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazNavArrowButton
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
    val hizbNumber = firstAyah?.hizbNumber ?: 0

    Surface(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Next page (higher Quran page number — leftward in mushaf, so a left-pointing arrow)
            NimazNavArrowButton(
                direction = NavArrowDirection.PREVIOUS,
                enabled = (secondPageNumber ?: pageNumber) < totalPages,
                onClick = onNavigateNext,
                contentDescription = stringResource(R.string.cd_next_page),
                size = 44.dp
            )

            // Page / Juz / Hizb as badge chips — mirrors the audio player position row
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NimazBadge(
                    text = if (secondPageNumber != null)
                        stringResource(R.string.page_range_format, pageNumber, secondPageNumber)
                    else
                        stringResource(R.string.page_single_format, pageNumber),
                    tone = NimazTone.ACCENT,
                    emphasis = NimazBadgeEmphasis.FILLED,
                    size = NimazBadgeSize.SMALL
                )
                if (juzNumber > 0) {
                    NimazBadge(
                        text = "Juz $juzNumber",
                        tone = NimazTone.ACCENT,
                        emphasis = NimazBadgeEmphasis.OUTLINED,
                        size = NimazBadgeSize.SMALL
                    )
                }
                if (hizbNumber > 0) {
                    NimazBadge(
                        text = "Hizb $hizbNumber",
                        tone = NimazTone.ACCENT,
                        emphasis = NimazBadgeEmphasis.OUTLINED,
                        size = NimazBadgeSize.SMALL
                    )
                }
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
                        contentDescription = if (allPageRead) "Page read" else "Mark page as read",
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
                size = 44.dp
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
                    hizbNumber = 2,
                    rubNumber = 0,
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
