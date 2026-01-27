package com.arshadshah.nimaz.presentation.screens.dua

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.viewmodel.DuaEvent
import com.arshadshah.nimaz.presentation.viewmodel.DuaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuasCollectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: DuaViewModel = hiltViewModel()
) {
    val state by viewModel.collectionState.collectAsState()
    val favoritesState by viewModel.favoritesState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.onEvent(DuaEvent.LoadFavorites)
        viewModel.onEvent(DuaEvent.LoadTodayProgress)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Duas & Adhkar",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = onNavigateToFavorites) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorites"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Search Bar
                item {
                    NimazSearchBar(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.onEvent(DuaEvent.SearchCategories(it)) },
                        placeholder = "Search duas...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        showClearButton = state.searchQuery.isNotEmpty(),
                        onClear = { viewModel.onEvent(DuaEvent.ClearSearch) }
                    )
                }

                // Featured Dua of the Day Card
                item {
                    val featuredFavorite = favoritesState.favorites.firstOrNull()
                    FeaturedDuaCard(
                        featuredTitle = featuredFavorite?.duaTitle,
                        featuredCategoryName = featuredFavorite?.categoryName,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // Favorites Section
                if (favoritesState.favorites.isNotEmpty()) {
                    item {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = 16.dp,
                                bottom = 12.dp
                            )
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favoritesState.favorites.take(5)) { favorite ->
                                FavoriteDuaChip(
                                    title = favorite.duaId,
                                    onClick = { /* Navigate to dua */ }
                                )
                            }
                        }
                    }
                }

                // Daily Adhkar - 2-column grid
                item {
                    Text(
                        text = "Daily Adhkar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 20.dp,
                            bottom = 12.dp
                        )
                    )
                }

                // Category grid (first 4 categories as grid cards)
                item {
                    val gridCategories = state.filteredCategories.take(4)
                    val gridHeight = if (gridCategories.size <= 2) 160.dp else 320.dp

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        userScrollEnabled = false
                    ) {
                        items(
                            items = gridCategories,
                            key = { it.id }
                        ) { category ->
                            CategoryGridCard(
                                category = category,
                                onClick = { onNavigateToCategory(category.id) }
                            )
                        }
                    }
                }

                // Situational Duas - list style
                if (state.filteredCategories.size > 4) {
                    item {
                        Text(
                            text = "Situational Duas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = 20.dp,
                                bottom = 12.dp
                            )
                        )
                    }

                    items(
                        items = state.filteredCategories.drop(4),
                        key = { it.id }
                    ) { category ->
                        AdhkarListItem(
                            category = category,
                            onClick = { onNavigateToCategory(category.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                }

                // If 4 or fewer categories, show them all as grid only
                if (state.filteredCategories.size <= 4 && state.filteredCategories.size > 0) {
                    // Already shown in grid above
                }
            }
        }
    }
}

@Composable
private fun FeaturedDuaCard(
    featuredTitle: String? = null,
    featuredCategoryName: String? = null,
    modifier: Modifier = Modifier
) {
    val goldColor = Color(0xFFEAB308)
    val goldDark = Color(0xFFCA8A04)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(goldColor, goldDark)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                // Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (featuredTitle != null) "Your Favorite" else "Dua of the Day",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1917)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Arabic text
                Text(
                    text = "\u0631\u064E\u0628\u064E\u0651\u0646\u064E\u0627 \u0622\u062A\u0650\u0646\u064E\u0627 \u0641\u0650\u064A \u0627\u0644\u062F\u064F\u0651\u0646\u0652\u064A\u064E\u0627 \u062D\u064E\u0633\u064E\u0646\u064E\u0629\u064B \u0648\u064E\u0641\u0650\u064A \u0627\u0644\u0652\u0622\u062E\u0650\u0631\u064E\u0629\u0650 \u062D\u064E\u0633\u064E\u0646\u064E\u0629\u064B \u0648\u064E\u0642\u0650\u0646\u064E\u0627 \u0639\u064E\u0630\u064E\u0627\u0628\u064E \u0627\u0644\u0646\u064E\u0651\u0627\u0631\u0650",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.End,
                    lineHeight = 40.sp,
                    color = Color(0xFF1C1917),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Translation / Title
                Text(
                    text = featuredTitle
                        ?: "\"Our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1C1917).copy(alpha = 0.9f),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Source
                Text(
                    text = featuredCategoryName ?: "Quran 2:201",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1C1917).copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CategoryGridCard(
    category: DuaCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = getCategoryColor(category.id)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryEmoji(category.iconName),
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = category.nameEnglish,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${category.duaCount} duas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdhkarListItem(
    category: DuaCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryEmoji(category.iconName),
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(15.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.nameEnglish,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (category.description != null) {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${category.duaCount} duas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteDuaChip(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun getCategoryColor(categoryId: String): Color {
    return when (categoryId.hashCode() % 8) {
        0 -> Color(0xFFFBBF24) // morning/gold
        1 -> Color(0xFF6366F1) // evening/indigo
        2 -> Color(0xFF8B5CF6) // sleep/purple
        3 -> MaterialTheme.colorScheme.primary // prayer/teal
        4 -> Color(0xFFF97316) // travel/orange
        5 -> Color(0xFF22C55E) // food/green
        6 -> Color(0xFFEF4444) // protection/red
        7 -> Color(0xFFEC4899) // forgiveness/pink
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getCategoryEmoji(iconName: String?): String {
    if (iconName == null) return "\uD83D\uDD4C"
    return when (iconName.lowercase()) {
        "morning", "sunrise", "fajr" -> "\uD83C\uDF05"
        "evening", "sunset", "moon" -> "\uD83C\uDF19"
        "sleep", "night", "bed" -> "\uD83D\uDE34"
        "prayer", "mosque", "salah" -> "\uD83D\uDD4C"
        "travel", "journey", "plane" -> "\u2708\uFE0F"
        "food", "eat", "drink" -> "\uD83C\uDF7D\uFE0F"
        "home", "house" -> "\uD83C\uDFE0"
        "bathroom", "wudu" -> "\uD83D\uDEBF"
        "sick", "health", "healing" -> "\uD83D\uDE37"
        "protection", "shield" -> "\uD83D\uDEE1\uFE0F"
        "forgiveness", "repentance" -> "\uD83E\uDD32"
        else -> "\uD83D\uDD4C"
    }
}
