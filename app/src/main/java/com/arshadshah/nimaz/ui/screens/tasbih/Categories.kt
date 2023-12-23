package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.viewModel.DuaViewModel

@Composable
fun Categories(
    paddingValues: PaddingValues,
    onNavigateToChapterListScreen: (String, Int) -> Unit,
) {
    val viewModel = viewModel(
        key = AppConstants.DUA_CHAPTERS_VIEWMODEL_KEY,
        initializer = { DuaViewModel() },
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )

    LaunchedEffect(Unit) {
        viewModel.getCategories()
    }

    val categories = remember { viewModel.categories }.collectAsState()

    //if the categories are not null, and not empty, then show them
    if (categories.value.isNotEmpty()) {
        //sort the categories alphabetically
        categories.value.sortBy { it.name }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            contentPadding = paddingValues
        ) {
            items(categories.value.size) {
                Category(
                    title = categories.value[it].name,
                    onClicked = {
                        onNavigateToChapterListScreen(
                            categories.value[it].name,
                            categories.value[it].id
                        )
                    }
                )
            }
        }
    }
}

//one category
@Composable
fun Category(
    title: String,
    icon: Int? = null,
    description: String = "",
    onClicked: () -> Unit = {},
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable {
                onClicked()
            }
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.Start
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            if (icon != null) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = description,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(32.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewCategory() {
    Category(title = "Subhanallah")
}