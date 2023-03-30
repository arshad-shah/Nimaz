package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.models.Chapter
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

@Composable
fun ChapterListItem(chapter : Chapter , onNavigateToChapter : (Int) -> Unit , loading : Boolean)
{
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp)
				.clickable {
					onNavigateToChapter(chapter._id)
				}
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp) ,
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			Column(
					modifier = Modifier
						.weight(1f)
						.padding(end = 8.dp) ,
				  ) {
				Text(
						text = chapter.english_title ,
						style = MaterialTheme.typography.titleLarge ,
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth()
							.placeholder(
									visible = loading ,
									color = MaterialTheme.colorScheme.outline ,
									shape = RoundedCornerShape(4.dp) ,
									highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																			)
										) ,
					)
			}
			//arrow icon to navigate to chapter
			Icon(
					painter = painterResource(id = R.drawable.angle_small_right_icon) ,
					contentDescription = "Navigate to chapter" ,
					modifier = Modifier
						.align(Alignment.CenterVertically)
						.size(24.dp)
						.fillMaxWidth()
						.placeholder(
								visible = loading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
									)
				)
		}
	}
}
