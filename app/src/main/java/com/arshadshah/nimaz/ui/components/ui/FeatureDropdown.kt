package com.arshadshah.nimaz.ui.components.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.ui.trackers.Placeholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FeaturesDropDown(
	items : List<T> ,
	label : String ,
	dropDownItem : @Composable (T) -> Unit ,
	header : @Composable (() -> Unit)? = null ,
					)
{
	Log.d("FeaturesDropDown" , "FeaturesDropDown: $label")
	Log.d("FeaturesDropDown" , "FeaturesDropDown: ${items.size}")
	val isExpanded = remember { mutableStateOf(false) }

	//the icon that is shown in the dropdown
	val icon = when (isExpanded.value)
	{
		true -> painterResource(id = R.drawable.arrow_up_icon)
		false -> painterResource(id = R.drawable.arrow_down_icon)
	}

	ElevatedCard(
			modifier = Modifier
				.padding(8.dp)
				) {

		//an elevation card that shows the text and icon
		ElevatedCard(
				modifier = Modifier
					.fillMaxWidth()
					.clickable {
						isExpanded.value = ! isExpanded.value
					} ,
				shape = MaterialTheme.shapes.medium ,
				content = {
					Row(
							modifier = Modifier
								.padding(8.dp)
								.fillMaxWidth() ,
							verticalAlignment = Alignment.CenterVertically ,
							horizontalArrangement = Arrangement.SpaceBetween
					   ) {
						Row(
								modifier = Modifier
									.fillMaxWidth(0.9f) ,
								verticalAlignment = Alignment.CenterVertically ,
								horizontalArrangement = Arrangement.Start
						   ){
							//the text
							Text(
									modifier = Modifier
										.padding(8.dp) ,
									text = label ,
									textAlign = TextAlign.Start ,
									maxLines = 2 ,
									overflow = TextOverflow.Ellipsis ,
									style = MaterialTheme.typography.bodyLarge
								)
							//a bubble that shows the number of features
							//if the list is empty then the bubble is not shown
							if (items.isNotEmpty())
							{
								Badge()
								{
									Text(
											text = items.size.toString() ,
											style = MaterialTheme.typography.bodyLarge
										)
								}
							}
						}
						//the icon
						Icon(
								painter = icon ,
								contentDescription = "dropdown icon" ,
								modifier = Modifier
									.padding(8.dp)
									.size(24.dp)
							)

					}
				}
					)

		//when the card is clicked show the dropdown menu
		//the menu has a list of bookmarks
		//when a bookmark is clicked it navigates to the ayat screen
		//the bookmark is highlighted
		AnimatedVisibility(
				visible = isExpanded.value,
				enter = expandVertically() + fadeIn() ,
				exit = shrinkVertically() + fadeOut()
						  ) {
			if(items.isEmpty()){
				Placeholder(nameOfDropdown = label)
			}
			else
			{
				Column(
						modifier = Modifier
							.fillMaxWidth()
					) {
					if (header != null)
					{
						header()
					}
					(items).forEach { item ->
						dropDownItem(item)
					}
				}
			}
		}
	}
}

@Composable
fun <T> FeatureDropdownItem(item: T , onClick: (T) -> Unit , itemContent: @Composable (T) -> Unit){
		ElevatedCard(
				modifier = Modifier
					.padding(bottom = 4.dp ,
							 start = 8.dp ,
							 end = 8.dp,
							 top = 4.dp)
					.fillMaxWidth()
					.clickable {
						onClick(item)
					} ,
				shape = MaterialTheme.shapes.medium ,
				content = {
					Row(
							modifier = Modifier
								.padding(8.dp)
								.fillMaxWidth() ,
							verticalAlignment = Alignment.CenterVertically ,
							horizontalArrangement = Arrangement.SpaceBetween
					   ) {
						itemContent(item)
						//the icon
						Icon(
								painter = painterResource(id = R.drawable.angle_small_right_icon) ,
								contentDescription = "Navigate" ,
								modifier = Modifier
									.padding(8.dp)
									.size(24.dp)
							)
					}
				}
					)
}