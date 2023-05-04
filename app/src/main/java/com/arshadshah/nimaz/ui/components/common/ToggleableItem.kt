package com.arshadshah.nimaz.ui.components.common

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

//toggelable item variant rowed
//overloaded function
@Composable
fun ToggleableItemRow(
	text : String ,
	checked : Boolean ,
	onCheckedChange : (Boolean) -> Unit ,
	enabled : Boolean = true ,
	modifier : Modifier ,
					 )
{

	Crossfade(
			targetState = checked ,
			animationSpec = tween(durationMillis = 300)
			 ) { targetState ->

		Column(
				modifier = modifier
					.clickable (
							enabled = enabled ,
							){
						onCheckedChange(! targetState)
					} ,
				verticalArrangement = Arrangement.Center ,
				horizontalAlignment = Alignment.CenterHorizontally
			  ) {
			//a icon button to toggle the state of the toggleable item
			OutlinedIconToggleButton(
					enabled = enabled ,
					colors = IconButtonDefaults.outlinedIconToggleButtonColors(
							containerColor = MaterialTheme.colorScheme.surface ,
							contentColor = MaterialTheme.colorScheme.onSurface ,
							checkedContainerColor = MaterialTheme.colorScheme.primary ,
							checkedContentColor = MaterialTheme.colorScheme.onPrimary ,
							disabledContentColor = Color(0xFFE91E63) ,
							disabledContainerColor = Color(0x1FE91E63) ,
																			  ) ,
					checked = targetState ,
					onCheckedChange = {
						Log.d("ToggleableItem" , "onCheckedChange: $it")
						onCheckedChange(it)
					} ,
									) {
				if (! targetState)
				{
					Icon(
							painter = painterResource(id = R.drawable.cross_icon) ,
							contentDescription = "Close" ,
							modifier = Modifier.padding(10.dp)
						)
				} else
				{
					Icon(
							painter = painterResource(id = R.drawable.check_icon) ,
							contentDescription = "Check" ,
							modifier = Modifier.padding(8.dp)
						)
				}
			}


			Crossfade(
					targetState = checked ,
					animationSpec = tween(durationMillis = 300)
					 ) { targetState ->
				if (! targetState)
				{
					Text(
							modifier = Modifier.padding(
									top = 8.dp ,
									start = 8.dp ,
									end = 8.dp ,
									bottom = 8.dp
													   ) ,
							text = text ,
							style = MaterialTheme.typography.bodySmall
						)
				} else
				{
					Text(
							modifier = Modifier.padding(
									top = 8.dp ,
									start = 8.dp ,
									end = 8.dp ,
									bottom = 8.dp
													   ) ,
							text = text ,
							style = MaterialTheme.typography.bodySmall ,
							color = MaterialTheme.colorScheme.primary
						)
				}
			}
		}
	}
}

//toggelable item variant columned
//overloaded function
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ToggleableItemColumn(
	text : String ,
	selectedText : String? = null ,
	checked : Boolean ,
	onCheckedChange : (Boolean) -> Unit ,
	enabled : Boolean = true ,
	modifier : Modifier ,
						)
{
	Crossfade(
			targetState = checked ,
			animationSpec = tween(durationMillis = 300)
			 ) { targetState ->
		Row(
				modifier = modifier
					.fillMaxWidth()
					.clickable(
							enabled = enabled ,
							  ) {
						onCheckedChange(! targetState)
					} ,
				verticalAlignment = Alignment.CenterVertically ,
				horizontalArrangement = Arrangement.Start
		   ) {
			//a icon button to toggle the state of the toggleable item
			OutlinedIconToggleButton(
					enabled = enabled ,
					colors = IconButtonDefaults.outlinedIconToggleButtonColors(
							containerColor = MaterialTheme.colorScheme.surface ,
							contentColor = MaterialTheme.colorScheme.onSurface ,
							checkedContainerColor = MaterialTheme.colorScheme.primary ,
							checkedContentColor = MaterialTheme.colorScheme.onPrimary ,
							disabledContentColor = Color(0xFFE91E63) ,
							disabledContainerColor = Color(0x1FE91E63) ,
																			  ) ,
					checked = targetState ,
					onCheckedChange = {
						Log.d("ToggleableItem" , "onCheckedChange: $it")
						onCheckedChange(it)
					} ,
									) {
				if (! targetState)
				{
					Icon(
							painter = painterResource(id = R.drawable.cross_icon) ,
							contentDescription = "Close" ,
							modifier = Modifier.padding(10.dp)
						)
				} else
				{
					Icon(
							painter = painterResource(id = R.drawable.check_icon) ,
							contentDescription = "Check" ,
							modifier = Modifier.padding(8.dp)
						)
				}
			}

			Crossfade(
					targetState = checked ,
					animationSpec = tween(durationMillis = 300)
					 ) { targetState ->
				if (! targetState)
				{
					Text(
							modifier = Modifier.padding(
									top = 8.dp ,
									start = 8.dp ,
									end = 8.dp ,
									bottom = 8.dp
													   ) ,
							text = text ,
							style = MaterialTheme.typography.bodyLarge
						)
				} else
				{
					Text(
							modifier = Modifier.padding(
									top = 8.dp ,
									start = 8.dp ,
									end = 8.dp ,
									bottom = 8.dp
													   ) ,
							text = selectedText ?: text ,
							style = MaterialTheme.typography.bodyLarge ,
							color = MaterialTheme.colorScheme.primary
						)
				}
			}
		}
	}
}


@Preview(showBackground = true , device = "id:S20 Fe")
@Composable
fun ToggleableItemRowPreview()
{
	val items = listOf("Fajr" , "Dhuhr" , "Asr" , "Maghrib" , "Isha")

	var isChecked by remember { mutableStateOf(false) }

	NimazTheme {
		ElevatedCard(
				modifier = Modifier
					.padding(16.dp)
					.fillMaxWidth()
					) {
			Row(
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth() ,
					horizontalArrangement = Arrangement.SpaceBetween ,
					verticalAlignment = Alignment.CenterVertically
			   ) {
				items.forEachIndexed { index , item ->
					ToggleableItemRow(
							text = item ,
							checked = isChecked ,
							onCheckedChange = {
								Log.d("ToggleableItemPreview" , "onCheckedChange: $it")
								isChecked = it
							} ,
							modifier = Modifier
								.placeholder(
										visible = false ,
										color = MaterialTheme.colorScheme.outline ,
										shape = RoundedCornerShape(4.dp) ,
										highlight = PlaceholderHighlight.shimmer(
												highlightColor = Color.White ,
																				)
											) ,
									 )
				}
			}
		}
	}
}

@Preview
@Composable
fun ToggleableItemColumnPreview()
{
	val items = listOf("Fajr" , "Dhuhr" , "Asr" , "Maghrib" , "Isha")

	var isChecked by remember { mutableStateOf(false) }

	NimazTheme {
		ElevatedCard(
				modifier = Modifier
					.fillMaxWidth()
					) {
			Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(8.dp) ,
					horizontalAlignment = Alignment.Start ,
					verticalArrangement = Arrangement.Center
				  ) {
				items.forEachIndexed { index , item ->
					ToggleableItemColumn(
							text = item ,
							checked = isChecked ,
							onCheckedChange = {
								Log.d("ToggleableItemPreview" , "onCheckedChange: $it")
								isChecked = it
							} ,
							modifier = Modifier
								.placeholder(
										visible = false ,
										color = MaterialTheme.colorScheme.outline ,
										shape = RoundedCornerShape(4.dp) ,
										highlight = PlaceholderHighlight.shimmer(
												highlightColor = Color.White ,
																				)
											) ,
										)
				}
			}
		}
	}
}