package com.arshadshah.nimaz.ui.components.ui.quran

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

@Composable
fun JuzListUI(
	juz : ArrayList<Juz> ,
	onNavigateToAyatScreen : (String , Boolean , String) -> Unit ,
	loading : Boolean ,
			 )
{
	LazyColumn(userScrollEnabled = true) {
		items(juz.size) { index ->
			JuzListItemUI(
					loading = loading ,
					juzNumber = juz[index].number.toString() ,
					name = juz[index].name ,
					tname = juz[index].tname ,
					onNavigateToAyatScreen = onNavigateToAyatScreen
						 )
		}
	}
}

@Composable
fun JuzListItemUI(
	juzNumber : String ,
	name : String ,
	tname : String ,
	onNavigateToAyatScreen : (String , Boolean , String) -> Unit ,
	context : Context = LocalContext.current ,
	loading : Boolean ,
				 )
{
	ElevatedCard(
			modifier = Modifier
				.padding(4.dp)
				.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
				.fillMaxWidth()
				.background(color = MaterialTheme.colorScheme.surface) ,
			shape = RoundedCornerShape(8.dp)
				) {
		//get the translation type from shared preferences
		val translationType =
			PrivateSharedPreferences(context).getData(
					key = AppConstants.TRANSLATION_LANGUAGE ,
					s = "English"
													 )
		val translation = when (translationType)
		{
			"English" -> "english"
			"Urdu" -> "urdu"
			else -> "english"
		}
		Row(
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
					.clickable(
							enabled = true
							  ) {
						onNavigateToAyatScreen(juzNumber , false , translation)
					}
		   ) {

			Text(
					modifier = Modifier
						.align(Alignment.CenterVertically)
						.weight(0.10f)
						.placeholder(
								visible = loading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
									) ,
					text = "$juzNumber." ,
					style = MaterialTheme.typography.bodyLarge
				)

			Column(
					modifier = Modifier
						.padding(16.dp , 0.dp)
						.align(Alignment.CenterVertically)
						.weight(0.80f)
				  ) {
				//apply quran font
				Text(
						text = name ,
						style = MaterialTheme.typography.titleLarge ,
						fontFamily = utmaniQuranFont ,
						modifier = Modifier
							.padding(vertical = 4.dp)
							.placeholder(
									visible = loading ,
									color = MaterialTheme.colorScheme.outline ,
									shape = RoundedCornerShape(4.dp) ,
									highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																			)
										)
					)
				Text(
						text = tname ,
						style = MaterialTheme.typography.titleSmall ,
						modifier = Modifier.placeholder(
								visible = loading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
													   )
					)
			}
			//an arrow right icon
			Icon(
					painter = painterResource(id = R.drawable.angle_small_right_icon) ,
					contentDescription = "Clear" ,
					modifier = Modifier
						.align(Alignment.CenterVertically)
						.weight(0.10f)
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