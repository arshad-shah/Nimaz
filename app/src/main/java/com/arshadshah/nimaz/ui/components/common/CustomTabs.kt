package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.arshadshah.nimaz.constants.AppConstants
import kotlinx.coroutines.launch

/**
 * Custom Tabs Component for Nimaz App
 * @param pagerState [PagerState]
 * @param titles [List] of [String]
 * */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTabs(pagerState: PagerState, titles: List<String>) {
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = Modifier
            .padding(4.dp)
            .clip(MaterialTheme.shapes.medium),
        containerColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions: List<TabPosition> ->
            TabIndicator(
                pagerState = pagerState,
                tabPositions = tabPositions
            )
        },
        divider = { }
    ) {
        TabsList(
            pagerState = pagerState,
            titles = titles
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabsList(
    pagerState: PagerState,
    titles: List<String>
){
    val scope = rememberCoroutineScope()
    titles.forEachIndexed { index, title ->
        val selected = pagerState.currentPage == index
        Tab(
            modifier = Modifier
                .zIndex(2f)
                .clip(MaterialTheme.shapes.medium)
                .testTag(
                    AppConstants.TEST_TAG_QURAN_TAB.replace(
                        "{number}",
                        index.toString()
                    )
                ),
            selected = pagerState.currentPage == index,
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(index)
                }
            },
            text = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.ExtraBold
                    else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabIndicator(
    pagerState: PagerState,
    tabPositions: List<TabPosition>
){
    val transition = updateTransition(pagerState.currentPage, label = "indicatorTransition")
    val indicatorStart by transition.animateDp(
        transitionSpec = {
            if (initialState < targetState) {
                spring(dampingRatio = 1f, stiffness = 50f)
            } else {
                spring(dampingRatio = 1f, stiffness = 1000f)
            }
        }, label = "tabIndicatorStart"
    ) {
        tabPositions[it].left
    }

    val indicatorEnd by transition.animateDp(
        transitionSpec = {
            if (initialState < targetState) {
                spring(dampingRatio = 1f, stiffness = 1000f)
            } else {
                spring(dampingRatio = 1f, stiffness = 50f)
            }
        }, label = "tabIndicatorEnd"
    ) {
        tabPositions[it].right
    }

    Box(
        Modifier
            .offset(x = indicatorStart)
            .wrapContentSize(align = Alignment.BottomStart)
            .width(indicatorEnd - indicatorStart)
            .padding(4.dp)
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                MaterialTheme.shapes.small
            )
            .zIndex(1f)
    )
}
