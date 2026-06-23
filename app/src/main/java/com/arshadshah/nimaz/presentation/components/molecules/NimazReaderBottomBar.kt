package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazActionPill
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * The shared reader bottom bar used by the Dua and Hadith readers: prev/next
 * chevrons flanking a [NimazActionPill] (whose contents the caller supplies via
 * [actions]) in a single row, with a page indicator directly beneath. Chevrons
 * disable at the ends; the indicator falls back to a "current / total" counter
 * when there are too many pages for dots. When [pageCount] <= 1 only the pill
 * shows (no chevrons or indicator).
 */
@Composable
fun NimazReaderBottomBar(
    currentPage: Int,
    pageCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    prevContentDescription: String,
    nextContentDescription: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit
) {
    val hasPager = pageCount > 1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasPager) {
                NavChevron(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    enabled = currentPage > 0,
                    onClick = onPrev,
                    contentDescription = prevContentDescription
                )
            }
            NimazActionPill(content = actions)
            if (hasPager) {
                NavChevron(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    enabled = currentPage < pageCount - 1,
                    onClick = onNext,
                    contentDescription = nextContentDescription
                )
            }
        }

        if (hasPager) {
            if (pageCount <= 12) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { index ->
                        val selected = index == currentPage
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (selected) 20.dp else 6.dp)
                                .background(
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = if (selected) RoundedCornerShape(3.dp) else CircleShape
                                )
                        )
                    }
                }
            } else {
                Text(
                    text = "${currentPage + 1} / $pageCount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NavChevron(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            NimazIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                size = NimazIconSize.LARGE
            )
        }
    }
}


// ==================== PREVIEWS ====================

@Composable
private fun NimazReaderBottomBarShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Middle page of a short pager (dots indicator)
        NimazReaderBottomBar(
            currentPage = 1,
            pageCount = 4,
            onPrev = {},
            onNext = {},
            prevContentDescription = "Previous page",
            nextContentDescription = "Next page"
        ) {
            com.arshadshah.nimaz.presentation.components.atoms.NimazPillActionButton(
                icon = androidx.compose.material.icons.Icons.Default.Edit,
                contentDescription = "Highlight",
                onClick = {},
                active = true
            )
            com.arshadshah.nimaz.presentation.components.atoms.NimazPillActionButton(
                icon = androidx.compose.material.icons.Icons.Default.Share,
                contentDescription = "Share",
                onClick = {}
            )
        }

        // Long pager (counter indicator instead of dots)
        NimazReaderBottomBar(
            currentPage = 7,
            pageCount = 30,
            onPrev = {},
            onNext = {},
            prevContentDescription = "Previous page",
            nextContentDescription = "Next page"
        ) {
            com.arshadshah.nimaz.presentation.components.atoms.NimazPillActionButton(
                icon = androidx.compose.material.icons.Icons.Default.Share,
                contentDescription = "Share",
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "ReaderBottomBar — Light")
@Composable
private fun NimazReaderBottomBarLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazReaderBottomBarShowcase()
    }
}

@Preview(showBackground = true, name = "ReaderBottomBar — Dark")
@Composable
private fun NimazReaderBottomBarDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazReaderBottomBarShowcase()
    }
}
