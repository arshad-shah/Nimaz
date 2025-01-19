package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalCategory
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.DuaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaTabs(
    viewModel: DuaViewModel,
    paddingValues: PaddingValues,
    navController: NavHostController,
    onNavigateToChapterListScreen: (String, Int) -> Unit,
    onDuaClick: (LocalDua) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Categories", "Favorites", "Search")
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val filteredDuas by viewModel.filteredDuas.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        // Tab Row
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    icon = {
                        when (index) {
                            0 -> Icon(Icons.Default.Category, contentDescription = null)
                            1 -> Icon(Icons.Default.Favorite, contentDescription = null)
                            2 -> Icon(Icons.Default.Search, contentDescription = null)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> {
                val categories by viewModel.categories.collectAsState()
                CategoriesContent(
                    categories = categories,
                    onCategoryClick = { category ->
                        onNavigateToChapterListScreen(
                            category.name,
                            category.id
                        )
                    }
                )
            }

            1 -> FavoritesContent(
                favorites = favorites,
                viewModel = viewModel,
                onDuaClick = onDuaClick
            )

            2 -> SearchContent(
                searchQuery = searchQuery,
                filteredDuas = filteredDuas,
                onSearchClear = { viewModel.clearSearch() },
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onSearch = { query: String -> viewModel.getSearchDuas(query) },
                onDuaClick = onDuaClick
            )
        }
    }
}


@Composable
private fun CategoriesContent(
    categories: List<LocalCategory>,
    onCategoryClick: (LocalCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            CategoryGroupCard(
                categories = categories,
                onCategoryClick = onCategoryClick
            )
        }
    }
}

@Composable
private fun CategoryGroupCard(
    categories: List<LocalCategory>,
    onCategoryClick: (LocalCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section with animation
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${categories.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Categories List with enhanced items
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categories.forEachIndexed { index, category ->
                    CategoryItem(
                        category = category,
                        index = index + 1,
                        onClick = { onCategoryClick(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: LocalCategory,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {// Enhanced Category Number Container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Category Name
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Navigation Arrow with animation
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.angle_small_right_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoritesContent(
    favorites: List<LocalDua>,
    viewModel: DuaViewModel,
    onDuaClick: (LocalDua) -> Unit
) {
    viewModel.getFavorites()
    if (favorites.isEmpty()) {
        EmptyFavorites()
    } else {
        val groupedFavorites = favorites.groupBy { dua ->
            val chapter = viewModel.getChapterById(dua.chapter_id)
            val category = viewModel.getCategoryById(chapter?.category_id ?: 0)
            category?.name ?: "Uncategorized"
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            groupedFavorites.forEach { (category, duas) ->
                stickyHeader {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                items(duas) { dua ->
                    DuaCard(
                        dua = dua,
                        onClick = { onDuaClick(dua) },
                        onFavoriteClick = { viewModel.toggleFavorite(dua) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchContent(
    searchQuery: String,
    filteredDuas: List<LocalDua>,
    onSearchClear: () -> Unit,
    onSearch: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDuaClick: (LocalDua) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth(0.8f),
                placeholder = { Text("Search duas...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchClear() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )
            //enter button
            FilledIconButton(
                onClick = {
                    onSearch(searchQuery)
                },
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = "Enter",
                )
            }
        }

        if (filteredDuas.isEmpty() && searchQuery.isNotEmpty()) {
            EmptySearchResults()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDuas) { dua ->
                    DuaCard(
                        dua = dua,
                        onClick = { onDuaClick(dua) },
                        onFavoriteClick = null // Optional: Add favorite toggle functionality
                    )
                }
            }
        }
    }
}

@Composable
private fun DuaCard(
    dua: LocalDua,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)?
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Arabic Text
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = dua.arabic_dua.cleanText(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = utmaniQuranFont,
                            fontSize = 28.sp,
                            lineHeight = 46.sp,
                            textAlign = TextAlign.Start
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        softWrap = true,
                        overflow = TextOverflow.Visible
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dua.english_translation.cleanText(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (onFavoriteClick != null) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = if (dua.favourite == 1) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (dua.favourite == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFavorites() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No Favorites Yet",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Add duas to favorites to see them here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySearchResults() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No Results Found",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Try different search terms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}