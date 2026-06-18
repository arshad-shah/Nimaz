package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A single reusable horizontal carousel: an edge-peeking [HorizontalPager] with
 * page-indicator dots underneath. Shared by the home "Today" cards and the
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

    val pagerState = rememberPagerState(pageCount = { count })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
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
            Spacer(modifier = Modifier.height(12.dp))
            PageIndicators(
                count = count,
                current = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Row of page-indicator dots; the active dot widens into a pill — a clearer
 * "you are here" cue than colour alone for reduced-colour-vision users.
 */
@Composable
fun PageIndicators(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val isCurrent = index == current
            val width by animateDpAsState(
                targetValue = if (isCurrent) 20.dp else 8.dp,
                label = "indicator_width_$index"
            )
            val color by animateColorAsState(
                targetValue = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                },
                label = "indicator_color_$index"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
