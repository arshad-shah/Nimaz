package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * A compact row of one-tap shortcuts into the Quran's browse modes and saved lists.
 * These are navigation entry points only — they do not duplicate any card content.
 */
@Composable
internal fun QuranQuickActions(
    onJuzClick: () -> Unit,
    onPageClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickAction(
            icon = Icons.AutoMirrored.Filled.ViewList,
            label = stringResource(R.string.quran_home_tab_juz),
            onClick = onJuzClick,
            modifier = Modifier.weight(1f)
        )
        QuickAction(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            label = stringResource(R.string.quran_home_tab_page),
            onClick = onPageClick,
            modifier = Modifier.weight(1f)
        )
        QuickAction(
            icon = Icons.Default.Bookmark,
            label = stringResource(R.string.quran_home_bookmarks),
            onClick = onBookmarksClick,
            modifier = Modifier.weight(1f)
        )
        QuickAction(
            icon = Icons.Default.Favorite,
            label = stringResource(R.string.quran_home_tab_favorites),
            onClick = onFavoritesClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuranQuickActionsPreview() {
    NimazTheme {
        QuranQuickActions(
            onJuzClick = {},
            onPageClick = {},
            onBookmarksClick = {},
            onFavoritesClick = {}
        )
    }
}
