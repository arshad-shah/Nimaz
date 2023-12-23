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

@OptIn(ExperimentalFoundationApi::class)
@Composable
        /**
         * Custom Tabs Component for Nimaz App
         * @param pagerState [PagerState]
         * @param titles [List] of [String]
         * */
fun CustomTabs(pagerState: PagerState, titles: List<String>) {
    val scope = rememberCoroutineScope()
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = Modifier
            .padding(
                top = 0.dp,
                bottom = 2.dp,
                start = 4.dp,
                end = 4.dp
            )
            .clip(MaterialTheme.shapes.extraLarge),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        indicator = { tabPositions: List<TabPosition> ->
            val transition = updateTransition(pagerState.currentPage, label = "")
            val indicatorStart by transition.animateDp(
                transitionSpec = {
                    if (initialState < targetState) {
                        spring(dampingRatio = 1f, stiffness = 50f)
                    } else {
                        spring(dampingRatio = 1f, stiffness = 1000f)
                    }
                }, label = ""
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
                }, label = ""
            ) {
                tabPositions[it].right
            }

            Box(
                Modifier
                    .offset(x = indicatorStart)
                    .wrapContentSize(align = Alignment.BottomStart)
                    .width(indicatorEnd - indicatorStart)
                    .padding(2.dp)
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.shapes.extraLarge
                    )
                    .zIndex(1f)
            )
        },
        divider = { }
    ) {
        titles.forEachIndexed { index, title ->
            val selected = pagerState.currentPage == index
            Tab(
                modifier = Modifier
                    .zIndex(2f)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .testTag(
                        AppConstants.TEST_TAG_QURAN_TAB.replace(
                            "{number}",
                            index.toString()
                        )
                    ),
                selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                unselectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                    alpha = 0.6f
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
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            )
        }
    }
}