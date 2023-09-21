package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


//my tasbih drop down item for each tasbih
@Composable
fun DropDownHeader(headerLeft : String , headerMiddle : String , headerRight : String)
{
	// a three section row for the tasbih name, goal and count
	//divider between each section
	//should look like this
	//Name - Goal - Count

	Row(
			 modifier = Modifier
				 .fillMaxWidth() ,
			 verticalAlignment = Alignment.CenterVertically
	   ) {
		//name
		Text(
				 modifier = Modifier
					 .weight(1f)
					 .padding(8.dp) ,
				 text = headerLeft ,
				 textAlign = TextAlign.Center ,
				 maxLines = 2 ,
				 overflow = TextOverflow.Ellipsis ,
				 style = MaterialTheme.typography.bodyLarge
			)
		//divider
		Divider(
				 modifier = Modifier
					 .width(1.dp)
					 .height(24.dp) ,
				 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) ,
				 thickness = 1.dp ,
			   )
		//goal
		Text(
				 modifier = Modifier
					 .weight(1f)
					 .padding(8.dp) ,
				 text = headerMiddle ,
				 textAlign = TextAlign.Center ,
				 maxLines = 2 ,
				 overflow = TextOverflow.Ellipsis ,
				 style = MaterialTheme.typography.bodyLarge
			)
		//divider
		Divider(
				 modifier = Modifier
					 .width(1.dp)
					 .height(24.dp) ,
				 color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) ,
				 thickness = 1.dp ,
			   )
		//count
		Text(
				 modifier = Modifier
					 .weight(1f)
					 .padding(8.dp) ,
				 text = headerRight ,
				 textAlign = TextAlign.Center ,
				 maxLines = 2 ,
				 overflow = TextOverflow.Ellipsis ,
				 style = MaterialTheme.typography.bodyLarge
			)
	}
}