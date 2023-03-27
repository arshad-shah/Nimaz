package com.arshadshah.nimaz.ui.components.ui.trackers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme

//a composable that shows a placeholder when the data is not available in the database
@Composable
fun Placeholder(nameOfDropdown : String)
{
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth() ,
			content = {
				Column(
						modifier = Modifier
							.padding(8.dp) ,
						verticalArrangement = Arrangement.Center ,
						horizontalAlignment = Alignment.CenterHorizontally
					  ) {
					Text(
							text = "No $nameOfDropdown available" ,
							maxLines = 2 ,
							modifier = Modifier
								.padding(8.dp)
								.fillMaxWidth() ,
							textAlign = TextAlign.Center ,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) ,
							overflow = TextOverflow.Ellipsis ,
							style = MaterialTheme.typography.bodyLarge
						)
				}
			}
				)
}


//a preview of the placeholder
@Preview
@Composable
fun PlaceholderPreview()
{
	NimazTheme {
		Placeholder(nameOfDropdown = "Bookmarks")
	}
}