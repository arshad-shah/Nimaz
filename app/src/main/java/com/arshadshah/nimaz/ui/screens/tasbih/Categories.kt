package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.viewModel.DuaViewModel

@Composable
fun Categories(
    viewModel: DuaViewModel = viewModel(
        key = AppConstants.DUA_CHAPTERS_VIEWMODEL_KEY
    ),
    paddingValues: PaddingValues,
    onNavigateToChapterListScreen: (String, Int) -> Unit,
) {
    LaunchedEffect(true) { // If getCategories should be called only once, use a more meaningful key
        viewModel.getCategories()
    }

    val categories = viewModel.categories.collectAsState()
    val loading = viewModel.isLoading.collectAsState()

    // Determine the item count and content based on loading state
    val itemCount = if (loading.value) 5 else categories.value.size
    val itemContent: @Composable (Int) -> Unit = { index ->
        if (loading.value) {
            Category(title = "Category $index", loading = true, onClicked = {})
        } else {
            Category(
                title = categories.value[index].name,
                loading = false,
                onClicked = {
                    onNavigateToChapterListScreen(
                        categories.value[index].name,
                        categories.value[index].id
                    )
                }
            )
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = paddingValues
    ) {
        items(itemCount) {
            itemContent(it)
        }
    }
}


@Composable
fun Category(
    title: String,
    icon: Int? = null,
    description: String = "",
    onClicked: () -> Unit = {},
    loading: Boolean,
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClicked, enabled = !loading),
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .placeholder(
                    visible = loading,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            icon?.let {
                Image(
                    painter = painterResource(id = it),
                    contentDescription = description,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}


@Preview
@Composable
fun PreviewCategory() {
    Category(title = "Subhanallah", loading = false)
}