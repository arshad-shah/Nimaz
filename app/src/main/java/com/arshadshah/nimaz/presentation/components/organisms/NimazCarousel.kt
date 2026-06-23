package com.arshadshah.nimaz.presentation.components.organisms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazPageIndicator
import com.arshadshah.nimaz.presentation.components.atoms.NimazPager
import com.arshadshah.nimaz.presentation.components.atoms.rememberNimazPagerState
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A single reusable horizontal carousel: an edge-peeking [NimazPager] with a
 * [NimazPageIndicator] underneath. Shared by the home "Today" cards and the
 * banner pills so the swipe feel, peek, spacing and indicators stay identical.
 *
 * Deliberately has no auto-advance — pages only move on user swipe.
 *
 * @param count number of pages.
 * @param pageHeight fixed height for every page.
 * @param horizontalPadding inset on both edges so the neighbouring page peeks.
 * @param pageSpacing gap between pages.
 * @param pageSize [PageSize.Fill] (one page per width) by default.
 * @param showIndicators draw the dots row (auto-hidden for a single page).
 */
@Composable
fun NimazCarousel(
    count: Int,
    modifier: Modifier = Modifier,
    pageHeight: Dp,
    horizontalPadding: Dp = 20.dp,
    pageSpacing: Dp = 12.dp,
    pageSize: PageSize = PageSize.Fill,
    showIndicators: Boolean = true,
    pageContent: @Composable (page: Int) -> Unit,
) {
    if (count <= 0) return

    val pagerState = rememberNimazPagerState(pageCount = { count })

    Column(modifier = modifier.fillMaxWidth()) {
        NimazPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(pageHeight),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            pageSpacing = pageSpacing,
            pageSize = pageSize,
        ) { page ->
            pageContent(page)
        }

        if (showIndicators && count > 1) {
            NimazPageIndicator(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
    }
}


// ==================== PREVIEWS ====================

/**
 * Showcase of [NimazCarousel] (edge-peeking pager with indicator dots) and a
 * standalone [NimazPageIndicator] row. Rendered in both light and dark themes by
 * the previews below.
 */
@Composable
private fun NimazCarouselShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        NimazCarousel(
            count = 3,
            pageHeight = 120.dp,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Card ${page + 1}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        NimazPageIndicator(
            pageCount = 5,
            currentPage = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, name = "Carousel — Light")
@Composable
private fun NimazCarouselLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazCarouselShowcase()
    }
}

@Preview(
    showBackground = true, name = "Carousel — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazCarouselDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazCarouselShowcase()
    }
}
