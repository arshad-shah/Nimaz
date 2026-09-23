package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.arshadshah.nimaz.presentation.theme.MiscArtColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import kotlin.math.abs

/** testTag applied to every dot so UI tests can count and locate the indicators. */
const val NimazPageIndicatorDotTag = "nimaz_page_indicator_dot"

/**
 * The app's canonical page-indicator: a centred row of dots where the active dot
 * widens into a pill. Widening (not colour alone) carries the "you are here" cue
 * so it stays legible for reduced-colour-vision users, and the active dot exposes
 * `selected = true` semantics for accessibility services and tests.
 *
 * This is the single indicator style for the whole app — onboarding and the
 * carousel both render through it. (It is *not* a progress tracker: for
 * "N of M completed" use `QaidaLineProgressDots` (`:feature:content`) instead.)
 *
 * This [PagerState] overload is the common case — it tracks `state.currentPage`
 * over `state.pageCount` automatically. Use the (`pageCount`, `currentPage`)
 * overload for non-pager callers.
 *
 * @param activeColor fill for the current dot; defaults to the theme primary.
 *   Override for surfaces with a bespoke palette (e.g. onboarding's gold).
 * @param inactiveColor fill for the remaining dots.
 * @param dotSize diameter of an inactive dot (and the height of the active pill).
 * @param activeWidth width the active dot animates to.
 * @param spacing gap on each side of every dot.
 * @param followDrag when true the pill tracks the pager's live scroll position — it shrinks on
 *   one dot and grows on the next as the finger moves — instead of jumping once a page is
 *   reached. Width and colour are read in the layout and draw phases, so a drag re-lays out
 *   the dots without recomposing them.
 * @param glowColor a soft halo drawn behind the active pill, scaled by how active it is.
 *   [Color.Unspecified] (the default) draws none.
 */
@Composable
fun NimazPageIndicator(
    state: PagerState,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
    dotSize: Dp = 8.dp,
    activeWidth: Dp = 20.dp,
    spacing: Dp = 3.dp,
    followDrag: Boolean = false,
    glowColor: Color = Color.Unspecified,
) {
    if (followDrag) {
        FollowingPageIndicator(state, modifier, activeColor, inactiveColor, dotSize, activeWidth, spacing, glowColor)
        return
    }
    NimazPageIndicator(
        pageCount = state.pageCount,
        currentPage = state.currentPage,
        modifier = modifier,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        dotSize = dotSize,
        activeWidth = activeWidth,
        spacing = spacing,
    )
}

/**
 * Stateless overload of [NimazPageIndicator] for callers that drive the current
 * page themselves (no [PagerState]). Renders nothing for [pageCount] `<= 1`,
 * since a single page needs no indicator. See the [PagerState] overload for the
 * styling parameters.
 */
@Composable
fun NimazPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
    dotSize: Dp = 8.dp,
    activeWidth: Dp = 20.dp,
    spacing: Dp = 3.dp,
) {
    if (pageCount <= 1) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isCurrent = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isCurrent) activeWidth else dotSize,
                label = "indicator_width_$index",
            )
            val color by animateColorAsState(
                targetValue = if (isCurrent) activeColor else inactiveColor,
                label = "indicator_color_$index",
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = spacing)
                    .semantics { selected = isCurrent }
                    .testTag(NimazPageIndicatorDotTag)
                    .size(width = width, height = dotSize)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/**
 * The [followDrag][NimazPageIndicator] rendering. Each dot's "activeness" is 1 on the page the
 * pager is showing and falls linearly to 0 one page away, so mid-swipe two dots share the pill.
 * `selected` still follows `currentPage`, which is what a screen reader should announce.
 */
@Composable
private fun FollowingPageIndicator(
    state: PagerState,
    modifier: Modifier,
    activeColor: Color,
    inactiveColor: Color,
    dotSize: Dp,
    activeWidth: Dp,
    spacing: Dp,
    glowColor: Color,
) {
    if (state.pageCount <= 1) return
    val position = { state.currentPage + state.currentPageOffsetFraction }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(state.pageCount) { index ->
            val activeness = { (1f - abs(position() - index)).coerceIn(0f, 1f) }
            Box(
                modifier = Modifier
                    .padding(horizontal = spacing)
                    .semantics { selected = index == state.currentPage }
                    .testTag(NimazPageIndicatorDotTag)
                    .layout { measurable, _ ->
                        val width = lerp(dotSize, activeWidth, activeness()).roundToPx()
                        val height = dotSize.roundToPx()
                        val placeable = measurable.measure(Constraints.fixed(width, height))
                        layout(width, height) { placeable.place(0, 0) }
                    }
                    .drawBehind {
                        val a = activeness()
                        val radius = CornerRadius(size.height / 2f)
                        if (glowColor.isSpecified && a > 0f) {
                            // A circular gradient stretched to the pill's proportions, so the halo
                            // fades to nothing on every side instead of stopping at a box edge.
                            val halo = size.height * 1.6f
                            val r = size.height / 2f + halo
                            scale(scaleX = (size.width / 2f + halo) / r, scaleY = 1f, pivot = center) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(glowColor.copy(alpha = glowColor.alpha * 0.45f * a), Color.Transparent),
                                        center = center, radius = r,
                                    ),
                                    radius = r, center = center,
                                )
                            }
                        }
                        drawRoundRect(color = lerpColor(inactiveColor, activeColor, a), cornerRadius = radius)
                    },
            )
        }
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun NimazPageIndicatorShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NimazPageIndicator(pageCount = 5, currentPage = 0)
        NimazPageIndicator(pageCount = 5, currentPage = 2)
        NimazPageIndicator(pageCount = 4, currentPage = 3)
        // Bespoke palette (e.g. onboarding's illuminated gold on dark).
        NimazPageIndicator(
            pageCount = 4,
            currentPage = 1,
            activeColor = MiscArtColors.PageIndicatorGold,
            inactiveColor = Color.White.copy(alpha = 0.28f),
        )
    }
}

@Preview(showBackground = true, name = "Page Indicator — Light")
@Composable
private fun NimazPageIndicatorLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazPageIndicatorShowcase() }
}

@Preview(
    showBackground = true, name = "Page Indicator — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazPageIndicatorDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazPageIndicatorShowcase() }
}
