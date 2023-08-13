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
fun Categories(paddingValues : PaddingValues , onNavigateToChapterListScreen : (String) -> Unit)
{
	val viewModel = viewModel(
			key = AppConstants.DUA_CHAPTERS_VIEWMODEL_KEY ,
			initializer = { DuaViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )

	LaunchedEffect(Unit) {
		viewModel.getCategories()
	}

	val categories = remember { viewModel.categories }.collectAsState()

	//if the categories are not null, and not empty, then show them
	if (categories.value.isNotEmpty())
	{
		val uniqueCategories = categories.value.distinctBy { it.keys }
		//get the titles of the categories
		//List<Map<String, ArrayList<Chapter>>>
		val categoryTitles = uniqueCategories.map { it.keys }.flatten()
		//empty list
		val newCategoryTitles = ArrayList<String>()
		//find the empty string and change it to "All Chapters"
		categoryTitles.forEach {
			if (it == "")
			{
				newCategoryTitles.add("All Chapters")
				//remove the empty string
				newCategoryTitles.remove("")
			} else
			{
				newCategoryTitles.add(it)
			}
		}

		//return the amount of chapters in each category
		val chaptersInEachCategory = uniqueCategories.map { it.values }.flatten()

		//sort the categories alphabetically
		newCategoryTitles.sort()

		LazyVerticalGrid(
				columns = GridCells.Adaptive(minSize = 128.dp) ,
				contentPadding = paddingValues
						) {
			items(newCategoryTitles.size) {
				//if the title is All Chapters, then return the amount of chapters in the list
				//else return the amount of chapters in each category
				if (newCategoryTitles[it] == "All Chapters")
				{
					Category(
							title = newCategoryTitles[it] ,
							//return the amount of chapters in the list
							amount = chaptersInEachCategory[it].size ,
							onClicked = {
								onNavigateToChapterListScreen(newCategoryTitles[it])
							}
							)
				} else
				{
					Category(
							title = newCategoryTitles[it] ,
							//return the amount of chapters in each category
							//where the title is the same as chaptersInEachCategory[0][0].category
							amount = chaptersInEachCategory[it].count { chapter ->
								chapter.category == newCategoryTitles[it]
							} ,
							onClicked = {
								onNavigateToChapterListScreen(newCategoryTitles[it])
							}
							)
				}
			}
		}
	}
}

//one category
@Composable
fun Category(
	title : String ,
	icon : Int? = null ,
	description : String = "" ,
	amount : Int ,
	onClicked : () -> Unit = {} ,
			)
{
	ElevatedCard(
			shape = MaterialTheme.shapes.large ,
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
					.fillMaxWidth() ,
				verticalAlignment = Alignment.CenterVertically ,
				horizontalArrangement = Arrangement.SpaceBetween
		   ) {
			Column(
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth() ,
					verticalArrangement = Arrangement.SpaceAround ,
					horizontalAlignment = Alignment.Start
				  ) {
				Text(text = title , style = MaterialTheme.typography.titleMedium)
				Text(text = "$amount Chapters" , style = MaterialTheme.typography.bodySmall)
			}
			if (icon != null)
			{
				Image(
						painter = painterResource(id = icon) ,
						contentDescription = description ,
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
fun PreviewCategory()
{
	Category(title = "Subhanallah" , amount = 0)
}