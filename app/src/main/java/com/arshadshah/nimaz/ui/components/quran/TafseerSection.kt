package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.viewModel.ViewModelLogger

@Composable
fun TafseerSection(
    aya: LocalAya,
    onOpenTafsir: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ViewModelLogger.d(
        "Nimaz: TafsirViewModel",
        "TafseerSection: $aya"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable {
                    onOpenTafsir(aya.ayaNumberInQuran, aya.suraNumber)
                }
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section with icon and text
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon container
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Tafseer icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }

                // Text column
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Tafseer Ibn Kathir",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Read explanation of verse ${aya.ayaNumberInSurah}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Right section with action indicator
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open Tafseer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

// Preview
@Composable
@Preview(showBackground = true)
fun TafseerSectionPreview() {
    val sampleAya = LocalAya(
        ayaNumberInQuran = 1,
        suraNumber = 1,
        ayaNumberInSurah = 1,
        ayaArabic = "",
        translationEnglish = "",
        translationUrdu = "",
        audioFileLocation = "",
        sajda = false,
        sajdaType = "",
        bookmark = false,
        favorite = false,
        note = "",
        ruku = 1,
        juzNumber = 1
    )

    TafseerSection(
        aya = sampleAya,
        onOpenTafsir = { _, _ -> }
    )
}