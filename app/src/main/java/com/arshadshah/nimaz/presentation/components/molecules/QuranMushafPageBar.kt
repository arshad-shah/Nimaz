package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.R
import androidx.compose.ui.res.stringResource

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
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Next page (higher Quran page number — leftward in mushaf)
            IconButton(
                onClick = onNavigateNext,
                enabled = (secondPageNumber ?: pageNumber) < totalPages,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_next_page),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Page info
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (secondPageNumber != null) "Pages $pageNumber\u2013$secondPageNumber" else "Page $pageNumber",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (juzNumber > 0) {
                    Text(
                        text = "  \u2022  Juz $juzNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (hizbNumber > 0) {
                    Text(
                        text = "  \u2022  Hizb $hizbNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Icon(
                        imageVector = if (allPageRead) Icons.Filled.CheckCircle
                        else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (allPageRead) "Page read" else "Mark page as read",
                        tint = if (allPageRead) Color(0xFF22C55E)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Previous page (lower Quran page number — rightward in mushaf)
            IconButton(
                onClick = onNavigatePrevious,
                enabled = pageNumber > 1,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.cd_previous_page),
                    modifier = Modifier.size(22.dp)
                )
            }
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
