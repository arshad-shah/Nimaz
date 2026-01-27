package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazChipVariant

/**
 * Tab item data.
 */
data class TabItem(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    val badge: Int? = null,
    val enabled: Boolean = true
)

/**
 * Standard tabs with indicator.
 */
@Composable
fun NimazTabs(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    height = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        divider = {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelect(index) },
                enabled = tab.enabled,
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (tab.icon != null) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = tab.title,
                            fontWeight = if (selectedTabIndex == index) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                        if (tab.badge != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge {
                                Text(text = tab.badge.toString())
                            }
                        }
                    }
                }
            )
        }
    }
}

/**
 * Scrollable tabs for many items.
 */
@Composable
fun NimazScrollableTabs(
    tabs: List<TabItem>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    edgePadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    height = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        divider = {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelect(index) },
                enabled = tab.enabled,
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (tab.icon != null) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = tab.title,
                            fontWeight = if (selectedTabIndex == index) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                        if (tab.badge != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge {
                                Text(text = tab.badge.toString())
                            }
                        }
                    }
                }
            )
        }
    }
}

/**
 * Pill-style tabs (segmented control).
 */
@Composable
fun NimazPillTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedIndex == index

                val backgroundColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(200),
                    label = "tab_background"
                )

                val textColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(200),
                    label = "tab_text"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundColor)
                        .clickable { onTabSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}

/**
 * Filter chips row with optional icon.
 */
@Composable
fun FilterChipsRow(
    filters: List<String>,
    selectedFilters: Set<String>,
    onFilterToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleSelection: Boolean = false,
    showFilterIcon: Boolean = true,
    onFilterIconClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showFilterIcon) {
            IconButton(
                onClick = onFilterIconClick ?: {},
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = if (showFilterIcon) 0.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                val selected = selectedFilters.contains(filter)

                FilterChip(
                    selected = selected,
                    onClick = {
                        if (singleSelection) {
                            if (!selected) {
                                onFilterToggle(filter)
                            }
                        } else {
                            onFilterToggle(filter)
                        }
                    },
                    label = { Text(text = filter) },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

/**
 * Category tabs for content sections.
 */
@Composable
fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    counts: Map<String, Int>? = null
) {
    val listState = rememberLazyListState()
    val selectedIndex = categories.indexOf(selectedCategory)

    LaunchedEffect(selectedCategory) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val selected = category == selectedCategory
            val count = counts?.get(category)

            NimazChip(
                text = if (count != null) "$category ($count)" else category,
                selected = selected,
                onClick = { onCategorySelect(category) },
                variant = NimazChipVariant.FILTER
            )
        }
    }
}

/**
 * Animated tab indicator.
 */
@Composable
fun AnimatedIndicatorTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var tabWidths by remember { mutableStateOf(listOf<Int>()) }

    val indicatorOffset by animateDpAsState(
        targetValue = if (tabWidths.isNotEmpty() && selectedIndex < tabWidths.size) {
            tabWidths.take(selectedIndex).sum().dp
        } else 0.dp,
        animationSpec = spring(),
        label = "indicator_offset"
    )

    val indicatorWidth by animateDpAsState(
        targetValue = if (tabWidths.isNotEmpty() && selectedIndex < tabWidths.size) {
            tabWidths[selectedIndex].dp
        } else 0.dp,
        animationSpec = spring(),
        label = "indicator_width"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedIndex == index
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.05f else 1f,
                    animationSpec = spring(),
                    label = "tab_scale"
                )

                val interactionSource = remember { MutableInteractionSource() }

                Text(
                    text = tab,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .scale(scale)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onTabSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        // Animated indicator
        if (indicatorWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset + 16.dp)
                    .width(indicatorWidth)
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * Sort options row.
 */
@Composable
fun SortOptionsRow(
    options: List<Pair<String, String>>, // id to label
    selectedOption: String,
    onOptionSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Sort by"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { (id, labelText) ->
                val selected = selectedOption == id

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    onClick = { onOptionSelect(id) }
                ) {
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * View mode toggle (list/grid).
 */
@Composable
fun ViewModeToggle(
    isGridView: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    listIcon: ImageVector = Icons.Default.FilterList,
    gridIcon: ImageVector = Icons.Default.FilterList
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            // List view
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (!isGridView) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onToggle(false) }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = listIcon,
                    contentDescription = "List view",
                    tint = if (!isGridView) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Grid view
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isGridView) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onToggle(true) }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = gridIcon,
                    contentDescription = "Grid view",
                    tint = if (isGridView) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
