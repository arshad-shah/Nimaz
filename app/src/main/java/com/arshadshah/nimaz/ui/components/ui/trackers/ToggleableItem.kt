package com.arshadshah.nimaz.ui.components.ui.trackers

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

@Composable
fun ToggleableItem(
	text : String ,
	checked : Boolean ,
	onCheckedChange : (Boolean) -> Unit ,
	modifier : Modifier ,
	showDateSelector : Boolean ,
				  )
{
	if (showDateSelector)
	{
		Row(
				modifier = modifier.clickable {
					onCheckedChange(! checked)
				} ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			//a icon button to toggle the state of the toggleable item
			//a icon button to toggle the state of the toggleable item
			IconButton(
					modifier = Modifier
						.padding(vertical = 8.dp , horizontal = 4.dp)
						.size(32.dp)
						.border(1.dp , MaterialTheme.colorScheme.primary , CircleShape) ,
					onClick = {
						onCheckedChange(! checked)
					}) {
				if (! checked)
				{
					Icon(
							painter = painterResource(id = R.drawable.cross_icon) ,
							contentDescription = "Close" ,
							tint = MaterialTheme.colorScheme.primary ,
							modifier = Modifier.size(48.dp)
						)
				} else
				{
					Icon(
							painter = painterResource(id = R.drawable.check_icon) ,
							contentDescription = "Check" ,
							tint = MaterialTheme.colorScheme.primary ,
							modifier = Modifier.size(48.dp)
						)
				}
			}


			Text(
					text = text ,
					modifier = Modifier.padding(start = 16.dp) ,
					style = MaterialTheme.typography.bodyLarge
				)
		}
	} else
	{
		Column(
				modifier = modifier.clickable {
					onCheckedChange(! checked)
				} ,
				horizontalAlignment = Alignment.CenterHorizontally ,
				verticalArrangement = Arrangement.Center
			  ) {
			//a icon button to toggle the state of the toggleable item
			IconButton(
					modifier = Modifier
						.padding(vertical = 8.dp , horizontal = 4.dp)
						.size(24.dp)
						.border(1.dp , MaterialTheme.colorScheme.primary , CircleShape) ,
					onClick = {
						onCheckedChange(! checked)
					}) {
				if (! checked)
				{
					Icon(
							painter = painterResource(id = R.drawable.cross_icon) ,
							contentDescription = "Close" ,
							tint = MaterialTheme.colorScheme.primary ,
							modifier = Modifier.size(26.dp)
						)
				} else
				{
					Icon(
							painter = painterResource(id = R.drawable.check_icon) ,
							contentDescription = "Check" ,
							tint = MaterialTheme.colorScheme.primary ,
							modifier = Modifier.size(26.dp)
						)
				}
			}


			Text(
					modifier = Modifier.padding(top = 8.dp) ,
					text = text ,
					style = MaterialTheme.typography.bodySmall
				)
		}
	}
}