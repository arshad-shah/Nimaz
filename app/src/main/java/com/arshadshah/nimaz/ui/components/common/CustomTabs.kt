package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

@Composable
fun CustomTabs(
    selectedPage: Int,
    titles: List<String>,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedPage,
                modifier = Modifier.padding(4.dp),
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabIndicator(
                        selectedPage = selectedPage,
                        tabPositions = tabPositions
                    )
                },
                divider = {}
            ) {
                TabsList(
                    selectedPage = selectedPage,
                    titles = titles,
                    onPageSelected = onPageSelected
                )
            }
        }
    }
}

@Composable
private fun TabsList(
    selectedPage: Int,
    titles: List<String>,
    onPageSelected: (Int) -> Unit
) {
    titles.forEachIndexed { index, title ->
        val selected = selectedPage == index

        Surface(
            modifier = Modifier.zIndex(2f),
            color = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                Color.Transparent,
            shape = RoundedCornerShape(12.dp)
        ) {
            Tab(
                selected = selected,
                onClick = { onPageSelected(index) },
                text = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Composable
private fun TabIndicator(
    selectedPage: Int,
    tabPositions: List<TabPosition>
) {
    val transition = updateTransition(selectedPage, label = "indicatorTransition")

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
            .zIndex(1f)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
    )
}

// Preview-specific wrapper for PagerState version
@Composable
fun CustomTabsWithPager(
    pagerState: PagerState,
    titles: List<String>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    CustomTabs(
        selectedPage = pagerState.currentPage,
        titles = titles,
        onPageSelected = { page ->
            // Launch in the composable's scope
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = page,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        },
        modifier = modifier
    )
}

@Preview(device = "id:small_phone")
@Composable
fun CustomTabsPreview() {
    var selectedPage by remember { mutableIntStateOf(0) }

    CustomTabs(
        selectedPage = selectedPage,
        titles = listOf("Sura", "Tab 2", "My Quran"),
        onPageSelected = { selectedPage = it }
    )
}