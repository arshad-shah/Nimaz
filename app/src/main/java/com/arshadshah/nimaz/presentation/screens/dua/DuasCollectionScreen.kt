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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.DuaEvent
import com.arshadshah.nimaz.presentation.viewmodel.DuaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuasCollectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToBookmarks: () -> Unit,
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
                    IconButton(onClick = onNavigateToBookmarks) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmarks"
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
                Icon(
                    imageVector = getCategoryIcon(category.iconName),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
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
                Icon(
                    imageVector = getCategoryIcon(category.iconName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
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
        else -> Icons.Default.Mosque
    }
}
