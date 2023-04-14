package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.models.Tasbih

// a dropdown item for each tasbih
//to contain annimated visibility delete button and the tasbih name, goal and count
@OptIn(ExperimentalMaterialApi::class , ExperimentalMaterial3Api::class)
@Composable
fun TasbihDropdownItem(
	item : Tasbih ,
	onClick : (Tasbih) -> Unit ,
	onDelete : (Tasbih) -> Unit ,
	onEdit : (Tasbih) -> Unit ,
					  )
{
	val currentItem = rememberUpdatedState(newValue = item)
	val dismissState = rememberDismissState(
			confirmStateChange = {
				if (it == DismissValue.DismissedToEnd)
				{
					onEdit(currentItem.value)
				} else if (it == DismissValue.DismissedToStart)
				{
					onDelete(currentItem.value)
				}
				false
			}
										   )

	SwipeToDismiss(
			state = dismissState ,
			background = {
				SwipeBackground(dismissState = dismissState)
			} ,
			dismissContent = {
				ElevatedCard(
						shape = MaterialTheme.shapes.extraLarge ,
						elevation = CardDefaults.elevatedCardElevation(
								defaultElevation = 4.dp ,
																	  ) ,
						modifier = Modifier
							.padding(8.dp)
							.clickable { onClick(currentItem.value) }
							) {

					//a row to contain the tasbih name, goal and count and the delete button
					Row(
							modifier = Modifier
								.padding(8.dp)
								.fillMaxWidth() ,
							verticalAlignment = Alignment.CenterVertically
					   ) {
						if (currentItem.value.count == currentItem.value.goal)
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
								text = currentItem.value.englishName ,
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
								text = currentItem.value.goal.toString() ,
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
								text = currentItem.value.count.toString() ,
								textAlign = TextAlign.Center ,
								maxLines = 2 ,
								overflow = TextOverflow.Ellipsis ,
								style = MaterialTheme.typography.bodySmall
							)
					}
				}
			})
}

@Composable
@OptIn(ExperimentalMaterial3Api::class , ExperimentalMaterialApi::class)
fun SwipeBackground(dismissState : DismissState)
{
	val direction = dismissState.dismissDirection ?: return

	val color by animateColorAsState(
			when (dismissState.targetValue)
			{
				DismissValue.Default -> MaterialTheme.colorScheme.tertiaryContainer
				DismissValue.DismissedToEnd -> MaterialTheme.colorScheme.primary
				DismissValue.DismissedToStart -> MaterialTheme.colorScheme.errorContainer
			}
									)
	val iconTintColor by animateColorAsState(
			when (dismissState.targetValue)
			{
				DismissValue.Default -> MaterialTheme.colorScheme.tertiaryContainer
				DismissValue.DismissedToEnd -> MaterialTheme.colorScheme.onPrimary
				DismissValue.DismissedToStart -> MaterialTheme.colorScheme.onErrorContainer
			}
											)
	val alignment = when (direction)
	{
		DismissDirection.StartToEnd -> Alignment.CenterStart
		DismissDirection.EndToStart -> Alignment.CenterEnd
	}
	val icon = when (direction)
	{
		DismissDirection.StartToEnd -> painterResource(id = com.arshadshah.nimaz.R.drawable.edit_icon)
		DismissDirection.EndToStart -> painterResource(id = com.arshadshah.nimaz.R.drawable.delete_icon)
	}
	val scale by animateFloatAsState(
			if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f
									)

	val haptic = LocalHapticFeedback.current
	LaunchedEffect(key1 = dismissState.targetValue , block = {
		if (dismissState.targetValue != DismissValue.Default)
		{
			haptic.performHapticFeedback(HapticFeedbackType.LongPress)
		}
	})

	Box(
			Modifier
				.fillMaxSize()
				.background(color)
				.padding(horizontal = 20.dp) ,
			contentAlignment = alignment
	   ) {
		Icon(
				painter = icon ,
				contentDescription = "Localized description" ,
				modifier = Modifier
					.scale(scale)
					.size(24.dp) ,
				tint = iconTintColor
			)
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
			item = tasbih ,
			onClick = { } ,
			onDelete = { tasbih ->

			} ,
			onEdit = { tasbih ->

			} ,
					  )
}