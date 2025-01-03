package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.viewModel.DuaViewModel
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Categories(
    viewModel: DuaViewModel = viewModel(key = AppConstants.DUA_CHAPTERS_VIEWMODEL_KEY),
    paddingValues: PaddingValues,
    onNavigateToChapterListScreen: (String, Int) -> Unit,
) {
    LaunchedEffect(true) {
        viewModel.getCategories()
    }

    val categories = viewModel.categories.collectAsState()
    val loading = viewModel.isLoading.collectAsState()
    val itemCount = if (loading.value) 5 else categories.value.size

    Card(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(itemCount) { index ->
                if (loading.value) {
                    CategoryItem(
                        title = "Loading category...",
                        number = index + 1,
                        loading = true,
                        onClicked = {}
                    )
                } else {
                    val category = categories.value[index]
                    CategoryItem(
                        title = category.name,
                        number = index + 1,
                        loading = false,
                        onClicked = {
                            onNavigateToChapterListScreen(category.name, category.id)
                        }
                    )
                }

                if (index < itemCount - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    title: String,
    number: Int,
    loading: Boolean,
    onClicked: () -> Unit,
    icon: Int? = null,
    description: String = ""
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = { if (!loading) onClicked() },
        enabled = !loading,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (!loading) {
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            )
                        } else {
                            listOf(Color.Transparent, Color.Transparent)
                        }
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Number
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (!loading)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .placeholder(
                        visible = loading,
                        highlight = PlaceholderHighlight.shimmer()
                    )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Category Title
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.placeholder(
                        visible = loading,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .placeholder(
                                visible = loading,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )
                }
            }

            // Navigation Icon
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Navigate to category",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = if (loading) 0.5f else 1f),
                modifier = Modifier
                    .size(24.dp)
                    .placeholder(
                        visible = loading,
                        highlight = PlaceholderHighlight.shimmer()
                    )
            )
        }
    }
}