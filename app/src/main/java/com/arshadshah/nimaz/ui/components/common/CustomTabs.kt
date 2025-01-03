package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

@Composable
fun CustomTabs(
    pagerState: PagerState,
    titles: List<String>,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        containerColor = Color.Transparent,
        indicator = { tabPositions ->
            TabIndicator(
                pagerState = pagerState,
                tabPositions = tabPositions
            )
        },
        divider = {}
    ) {
        TabsList(pagerState = pagerState, titles = titles)
    }
}

@Composable
private fun TabsList(
    pagerState: PagerState,
    titles: List<String>
) {
    val scope = rememberCoroutineScope()

    titles.forEachIndexed { index, title ->
        val selected = pagerState.currentPage == index

        val alpha by animateFloatAsState(
            targetValue = if (selected) 1f else 0.7f,
            animationSpec = tween(300),
            label = "tabAlpha"
        )

        val scale by animateFloatAsState(
            targetValue = if (selected) 1.05f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "tabScale"
        )

        Tab(
            modifier = Modifier
                .padding(2.dp)
                .zIndex(2f)
                .clip(MaterialTheme.shapes.medium)
                .scale(scale),
            selected = selected,
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(
                        page = index,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            },
            text = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
        )
    }
}

@Composable
private fun TabIndicator(
    pagerState: PagerState,
    tabPositions: List<TabPosition>
) {
    val transition = updateTransition(pagerState.currentPage, label = "indicatorTransition")

    val indicatorStart by transition.animateDp(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = if (initialState < targetState)
                    Spring.StiffnessVeryLow
                else
                    Spring.StiffnessMedium
            )
        },
        label = "indicatorStart"
    ) { page -> tabPositions[page].left }

    val indicatorEnd by transition.animateDp(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = if (initialState < targetState)
                    Spring.StiffnessMedium
                else
                    Spring.StiffnessVeryLow
            )
        },
        label = "indicatorEnd"
    ) { page -> tabPositions[page].right }

    Box(
        modifier = Modifier
            .offset(x = indicatorStart)
            .wrapContentSize(align = Alignment.BottomStart)
            .width(indicatorEnd - indicatorStart)
            .padding(4.dp)
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            )
            .zIndex(1f)
    )
}