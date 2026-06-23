package com.arshadshah.nimaz.presentation.screens.dua

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.DuaEvent
import com.arshadshah.nimaz.presentation.viewmodel.DuaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuasCollectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
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
                title = stringResource(R.string.duas_adhkar),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(DuaEvent.ToggleCategoriesSort) }) {
                        NimazIcon(
                            imageVector = Icons.Default.SortByAlpha,
                            contentDescription = stringResource(
                                if (state.sortAlphabetical) R.string.sort_categories_default
                                else R.string.sort_categories_alphabetically
                            ),
                            tint = if (state.sortAlphabetical) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LocalContentColor.current
                            }
                        )
                    }
                    IconButton(onClick = onNavigateToSearch) {
                        NimazIcon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_title)
                        )
                    }
                    IconButton(onClick = onNavigateToBookmarks) {
                        NimazIcon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = stringResource(R.string.bookmarks)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            NimazLoadingState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                // Favorites Section
                if (favoritesState.favorites.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.favorites),
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
                }

                if (state.sortAlphabetical) {
                    // Alphabetical mode: a single flat A–Z list (the curated
                    // Daily/Situational split is meaningless once reordered).
                    item {
                        Text(
                            text = stringResource(R.string.all_categories_az),
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
                        items = state.filteredCategories,
                        key = { it.id }
                    ) { category ->
                        AdhkarListItem(
                            category = category,
                            onClick = { onNavigateToCategory(category.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                } else {
                    // Daily Adhkar - 2-column grid
                    item {
                        Text(
                            text = stringResource(R.string.daily_adhkar),
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
                                text = stringResource(R.string.situational_duas),
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
                }
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

    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
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
                NimazIcon(
                    imageVector = getCategoryIcon(category.iconName),
                    contentDescription = null,
                    tint = iconColor,
                    size = NimazIconSize.LARGE
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
                text = stringResource(R.string.duas_count_format, category.duaCount),
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
    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
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
                NimazIcon(
                    imageVector = getCategoryIcon(category.iconName),
                    contentDescription = null,
                    variant = NimazIconVariant.MUTED,
                    iconSize = 22.dp
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
                    text = stringResource(R.string.duas_count_format, category.duaCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun getCategoryColor(categoryId: String): Color {
    return when (categoryId.hashCode() % 8) {
        0 -> NimazColors.Amber // morning/gold
        1 -> NimazColors.PrayerColors.Fajr // evening/indigo
        2 -> NimazColors.PrayerColors.Isha // sleep/purple
        3 -> MaterialTheme.colorScheme.primary // prayer/teal
        4 -> NimazColors.PrayerColors.Asr // travel/orange
        5 -> NimazColors.Success // food/green
        6 -> NimazColors.PrayerColors.Maghrib // protection/red
        7 -> NimazColors.Pink // forgiveness/pink
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getCategoryIcon(iconName: String?): ImageVector {
    if (iconName == null) return Icons.Default.Mosque
    return when (iconName) {
        "🌅" -> Icons.Default.LightMode
        "🌙" -> Icons.Default.DarkMode
        "🤲" -> Icons.Default.Mosque
        "☀️" -> Icons.Default.WbSunny
        "😴" -> Icons.Default.Hotel
        "🏠" -> Icons.Default.Home
        "🚪" -> Icons.Default.DoorFront
        "🕌" -> Icons.Default.Mosque
        "🕋" -> Icons.Default.Mosque
        "🍽️" -> Icons.Default.Restaurant
        "✨" -> Icons.Default.AutoAwesome
        "✈️" -> Icons.Default.Flight
        "🌧️" -> Icons.Default.WaterDrop
        "💚" -> Icons.Default.Favorite
        "🙏" -> Icons.Default.VolunteerActivism
        "🚿" -> Icons.Default.WaterDrop
        "🚻" -> Icons.Default.DoorFront
        "📣" -> Icons.AutoMirrored.Filled.VolumeUp
        "👕" -> Icons.Default.Person
        "🌟" -> Icons.Default.Star
        "🤧" -> Icons.Default.AutoAwesome
        "📖" -> Icons.AutoMirrored.Filled.MenuBook
        "💰" -> Icons.Default.Savings
        "💊" -> Icons.Default.LocalHospital
        "🕊️" -> Icons.Default.Warning
        "😤" -> Icons.Default.LocalFireDepartment
        "💳" -> Icons.Default.CreditCard
        "📜" -> Icons.Default.AutoStories
        "🌿" -> Icons.Default.Lightbulb
        "👨‍👩‍👧" -> Icons.Default.Groups
        "💍" -> Icons.Default.Favorite
        "🐫" -> Icons.Default.Explore
        "🌹" -> Icons.Default.FavoriteBorder
        "🛡️" -> Icons.Default.Shield
        "🧎" -> Icons.Default.Mosque
        "🌃" -> Icons.Default.NightsStay
        "🪦" -> Icons.Default.Bedtime
        "🌛" -> Icons.Default.Nightlight
        "⚔️" -> Icons.Default.Security
        "🌬️" -> Icons.Default.WbTwilight
        "🤍" -> Icons.Default.Celebration
        "📿" -> Icons.Default.AutoAwesome
        "🤝" -> Icons.Default.Public
        else -> Icons.Default.Mosque
    }
}
