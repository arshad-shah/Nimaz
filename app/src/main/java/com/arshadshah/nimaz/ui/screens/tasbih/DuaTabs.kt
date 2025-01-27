package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import com.arshadshah.nimaz.ui.components.common.CustomTabs
import com.arshadshah.nimaz.ui.components.common.NoResultFound
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.DuaViewModel
import kotlinx.coroutines.coroutineScope

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

        CustomTabs(
            selectedPage = selectedTabIndex,
            onPageSelected = { selectedTabIndex = it },
            titles = tabs
        )

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
                .padding(8.dp),
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
                    modifier = Modifier.size(48.dp)
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
    // State to hold the grouped favorites
    var groupedFavorites by remember { mutableStateOf<Map<String, List<LocalDua>>>(emptyMap()) }

    // Launch effect to load the grouped data
    LaunchedEffect(favorites) {
        // Group favorites with coroutine calls
        groupedFavorites = favorites.groupBy { dua ->
            coroutineScope {
                val chapter = viewModel.getChapterById(dua.chapter_id)
                val category = viewModel.getCategoryById(chapter?.category_id ?: 0)
                category?.name ?: "Uncategorized"
            }
        }
    }

    if (favorites.isEmpty()) {
        NoResultFound(
            title = "No Favorites Yet",
            subtitle = "Add duas to favorites to see them here"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            groupedFavorites.forEach { (category, duas) ->
                stickyHeader {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                items(
                    items = duas,
                    key = { it._id }
                ) { dua ->
                    DuaCard(
                        dua = dua,
                        onClick = { onDuaClick(dua) },
                        onFavoriteClick = { viewModel.toggleFavorite(dua) },
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
    var isSearchFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Animated Search Bar Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .scale(
                    animateFloatAsState(
                        targetValue = if (isSearchFocused) 1.02f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "searchScale"
                    ).value
                ),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (isSearchFocused) 8.dp else 4.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .onFocusChanged { isSearchFocused = it.isFocused },
                    placeholder = {
                        Text(
                            "Search duas...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            IconButton(
                                onClick = onSearchClear,
                                modifier = Modifier.scale(0.8f)
                            ) {
                                Icon(
                                    Icons.Rounded.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                FilledIconButton(
                    onClick = { onSearch(searchQuery) },
                    modifier = Modifier.scale(0.9f),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardReturn,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedContent(
            targetState = Pair(filteredDuas.isEmpty(), searchQuery.isNotEmpty()),
            transitionSpec = {
                fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
            },
            label = "contentTransition"
        ) { (isEmpty, hasQuery) ->
            when {
                isEmpty && hasQuery -> {
                    NoResultFound(
                        title = "No Results Found",
                        subtitle = "Try different search terms",
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredDuas,
                            key = { it._id }
                        ) { dua ->
                            DuaCard(
                                dua = dua,
                                onClick = { onDuaClick(dua) },
                                onFavoriteClick = null,
                            )
                        }
                    }
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