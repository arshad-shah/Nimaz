package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Single Nimaz entry point for creating the [PagerState] that drives a
 * [NimazPager]. A thin re-export of [rememberPagerState] kept so call sites read
 * `rememberNimazPagerState { count }` and never reach for the raw Compose API —
 * the one place to evolve paging defaults if we ever need to.
 *
 * @param pageCount lambda returning the live page count (re-read on change).
 * @param initialPage page shown on first composition.
 * @param initialPageOffsetFraction starting scroll offset within [initialPage],
 *   in `[-0.5, 0.5]` (see [rememberPagerState]).
 */
@Composable
fun rememberNimazPagerState(
    initialPage: Int = 0,
    initialPageOffsetFraction: Float = 0f,
    pageCount: () -> Int,
): PagerState = rememberPagerState(
    initialPage = initialPage,
    initialPageOffsetFraction = initialPageOffsetFraction,
    pageCount = pageCount,
)

/**
 * The app's standard horizontal pager: a thin, consistently-styled wrapper over
 * [HorizontalPager].
 *
 * It owns nothing stateful — the caller supplies the [state] (typically via
 * [rememberNimazPagerState]) and keeps any page⇄ViewModel synchronisation. The
 * wrapper exists purely so every paged surface in the app (onboarding, carousel,
 * the Quran/Hadith/Dua/Tafseer readers, tab pagers) flows through one component
 * with one set of defaults, instead of each screen re-deriving its own
 * `HorizontalPager` call. Pair it with [NimazPageIndicator] when dots are wanted;
 * readers simply omit them.
 *
 * Every parameter the existing call sites need is exposed as a pass-through so no
 * screen has to drop back to the raw pager:
 *
 * @param state the pager state driving paging and the current page.
 * @param contentPadding inset around the pages — e.g. an edge-peek so the
 *   neighbouring page shows (carousel).
 * @param pageSize how wide each page is; [PageSize.Fill] (one page per viewport)
 *   by default.
 * @param pageSpacing gap rendered between pages.
 * @param beyondViewportPageCount pages to keep composed on each side of the
 *   visible one — readers preload neighbours for snappy swipes.
 * @param reverseLayout lay pages out in reverse; combine with an RTL
 *   `LocalLayoutDirection` for right-to-left readers such as the Mushaf.
 * @param userScrollEnabled allow swipe gestures (false to drive paging only
 *   programmatically).
 * @param verticalAlignment how each page aligns within the pager's height.
 * @param key stable key per page so state survives reordering — readers key by
 *   content id (ayah/hadith/dua) rather than index.
 * @param pageContent the content for [page], scoped to [PagerScope].
 */
@Composable
fun NimazPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    pageSpacing: Dp = 0.dp,
    beyondViewportPageCount: Int = 0,
    reverseLayout: Boolean = false,
    userScrollEnabled: Boolean = true,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    key: ((page: Int) -> Any)? = null,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    HorizontalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        pageSpacing = pageSpacing,
        beyondViewportPageCount = beyondViewportPageCount,
        reverseLayout = reverseLayout,
        userScrollEnabled = userScrollEnabled,
        verticalAlignment = verticalAlignment,
        key = key,
        pageContent = pageContent,
    )
}


// ==================== PREVIEWS ====================

@Composable
private fun NimazPagerShowcase() {
    val state = rememberNimazPagerState(initialPage = 1) { 4 }
    Column(modifier = Modifier.fillMaxWidth()) {
        NimazPager(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 12.dp,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Page ${page + 1}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        NimazPageIndicator(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}

@Preview(showBackground = true, name = "Pager — Light")
@Composable
private fun NimazPagerLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazPagerShowcase() }
}

@Preview(
    showBackground = true, name = "Pager — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazPagerDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazPagerShowcase() }
}
