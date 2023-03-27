package com.arshadshah.nimaz.ui.components.ui.trackers

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.models.Tasbih
import kotlin.math.roundToInt

// a dropdown item for each tasbih
//to contain annimated visibility delete button and the tasbih name, goal and count
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TasbihDropdownItem(
	item : Tasbih ,
	onClick : (Tasbih) -> Unit ,
	onDelete : (Tasbih) -> Unit,
	onEdit : (Tasbih) -> Unit,
					  )
{
	val swipeState = rememberSwipeableState(initialValue = 0)
	Log.d("SwipeState" , swipeState.offset.value.toString())

	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			elevation = CardDefaults.elevatedCardElevation(
					defaultElevation = 4.dp ,
														  ) ,
			modifier = Modifier
				.offset { IntOffset(swipeState.offset.value.roundToInt() , 0) }
				.padding(8.dp)
				.clickable { onClick(item) }
				.swipeable(
						state = swipeState ,
						anchors = mapOf(0f to 0 , - 100f to 1) ,
						orientation = Orientation.Horizontal ,
						  )
				) {

		//a row to contain the tasbih name, goal and count and the delete button
		Row(
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth() ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			if(item.count == item.goal)
			{
				Icon(
						imageVector = Icons.Default.CheckCircle ,
						contentDescription = "Completed" ,
						modifier = Modifier
							.size(24.dp)
					)
			}
			//name
			Text(
					modifier = Modifier
						.weight(1f)
						.padding(8.dp) ,
					text = item.englishName ,
					textAlign = TextAlign.Center ,
					maxLines = 1 ,
					overflow = TextOverflow.Ellipsis ,
					style = MaterialTheme.typography.bodyMedium
				)
			//divider
			Divider(
					modifier = Modifier
						.width(1.dp)
						.height(24.dp) ,
					color = MaterialTheme.colorScheme.onSurface.copy(
							alpha = 0.08f
																	) ,
					thickness = 1.dp ,
				   )
			//goal
			Text(
					modifier = Modifier
						.weight(1f)
						.padding(8.dp) ,
					text = item.goal.toString() ,
					textAlign = TextAlign.Center ,
					maxLines = 2 ,
					overflow = TextOverflow.Ellipsis ,
					style = MaterialTheme.typography.bodySmall
				)
			//divider
			Divider(
					modifier = Modifier
						.width(1.dp)
						.height(24.dp) ,
					color = MaterialTheme.colorScheme.onSurface.copy(
							alpha = 0.08f
																	) ,
					thickness = 1.dp ,
				   )
			//count
			Text(
					modifier = Modifier
						.weight(1f)
						.padding(8.dp) ,
					text = item.count.toString() ,
					textAlign = TextAlign.Center ,
					maxLines = 2 ,
					overflow = TextOverflow.Ellipsis ,
					style = MaterialTheme.typography.bodySmall
				)
			AnimatedVisibility(
					visible = swipeState.offset.value < -50 ,
					enter = fadeIn() + expandHorizontally() ,
					exit = fadeOut() + shrinkHorizontally()
							  ) {
				Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.End
				   ) {
					//a delete button
					IconButton(
							onClick = {
								onDelete(item)
							} ,
							modifier = Modifier
								.size(48.dp)
							  ) {
						Icon(
								painter = painterResource(id = R.drawable.delete_icon) ,
								contentDescription = "Delete" ,
								modifier = Modifier
									.size(24.dp)
							)
					}

					//an edit button
					IconButton(
							onClick = {
								onEdit(item)
							} ,
							modifier = Modifier
								.size(48.dp)
							  ) {
						Icon(
								painter = painterResource(id = R.drawable.edit_icon) ,
								contentDescription = "Edit" ,
								modifier = Modifier
									.size(24.dp)
							)
					}
				}
			}
		}
	}
}


@Preview(
		showBackground = true
		)
@Composable
//MyTasbihDropDownItem
fun TasbihDropDownItemPreview()
{
	//tasbih object
	//val id: Int = 0,
	//    val date: String = LocalDate.now().toString(),
	//    val arabicName: String,
	//    val englishName: String,
	//    val translationName: String,
	//    val goal: Int = 0,
	//    val count: Int =
	val tasbih = Tasbih(
			arabicName = "الله أكبر" ,
			englishName = "Allahu Akbar" ,
			translationName = "God is the greatest" ,
			goal = 33 ,
			count = 0
					   )
	TasbihDropdownItem(
			item = tasbih,
			onClick = { } ,
			onDelete = { },
			onEdit = { }
					  )
}