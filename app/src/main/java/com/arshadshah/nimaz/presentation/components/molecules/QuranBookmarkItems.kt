package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookmarkListItem(
    bookmark: QuranBookmark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        tone = NimazCardTone.MUTED,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NimazIcon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                variant = NimazIconVariant.PRIMARY,
                size = NimazIconSize.LARGE
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.surahName ?: stringResource(
                        R.string.quran_home_surah_fallback,
                        bookmark.surahNumber
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.quran_home_verse_format, bookmark.ayahNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!bookmark.ayahText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bookmark.ayahText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookmarkCard(
    bookmark: QuranBookmark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        modifier = modifier.width(160.dp),
        shape = RoundedCornerShape(12.dp),
        tone = NimazCardTone.MUTED
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = bookmark.surahName ?: stringResource(
                    R.string.quran_home_surah_fallback,
                    bookmark.surahNumber
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.quran_home_verse_format, bookmark.ayahNumber),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!bookmark.ayahText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = bookmark.ayahText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookmarkListItemPreview() {
    NimazTheme {
        BookmarkListItem(
            bookmark = QuranBookmark(
                id = 1,
                ayahId = 1,
                surahNumber = 1,
                ayahNumber = 1,
                surahName = "Al-Fatihah",
                ayahText = "In the name of Allah, the Most Gracious, the Most Merciful.",
                note = null,
                color = null,
                createdAt = 0,
                updatedAt = 0
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookmarkCardPreview() {
    NimazTheme {
        BookmarkCard(
            bookmark = QuranBookmark(
                id = 1,
                ayahId = 1,
                surahNumber = 1,
                ayahNumber = 1,
                surahName = "Al-Fatihah",
                ayahText = "In the name of Allah, the Most Gracious, the Most Merciful.",
                note = null,
                color = null,
                createdAt = 0,
                updatedAt = 0
            ),
            onClick = {}
        )
    }
}
