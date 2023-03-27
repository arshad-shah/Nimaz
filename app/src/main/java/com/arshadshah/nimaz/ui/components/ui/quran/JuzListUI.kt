package com.arshadshah.nimaz.ui.components.ui.quran

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_JUZ_ITEM
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN_JUZ
import com.arshadshah.nimaz.data.remote.models.Juz
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

@Composable
fun JuzListUI(
	juz : ArrayList<Juz> ,
	onNavigateToAyatScreen : (String , Boolean , String , Int?) -> Unit ,
	loading : Boolean ,
			 )
{
	LazyColumn(
			userScrollEnabled = ! loading ,
			modifier = Modifier.testTag(TEST_TAG_QURAN_JUZ)
			  ) {
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
	onNavigateToAyatScreen : (String , Boolean , String , Int?) -> Unit ,
	context : Context = LocalContext.current ,
	loading : Boolean ,
				 )
{
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.padding(vertical = 4.dp , horizontal = 8.dp)
				.fillMaxWidth()
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
					.testTag(TEST_TAG_JUZ_ITEM)
					.clickable(
							enabled = ! loading ,
							  ) {
						onNavigateToAyatScreen(juzNumber , false , translation , null)
					},
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
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
					style = MaterialTheme.typography.bodyLarge,
					textAlign = TextAlign.Center
				)

			Column(
					modifier = Modifier
						.padding(16.dp , 0.dp)
						.align(Alignment.CenterVertically)
						.weight(0.80f)
						.fillMaxWidth()
				  ) {
				//apply quran font
				Text(
						text = name ,
						style = MaterialTheme.typography.titleLarge ,
						fontFamily = utmaniQuranFont ,
						fontWeight = FontWeight.SemiBold ,
						fontSize = 32.sp ,
						modifier = Modifier
							.padding(vertical = 4.dp)
							.fillMaxWidth()
							.placeholder(
									visible = loading ,
									color = MaterialTheme.colorScheme.outline ,
									shape = RoundedCornerShape(4.dp) ,
									highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																			)
										) ,
						textAlign = TextAlign.Center
					)
				Text(
						text = tname ,
						style = MaterialTheme.typography.titleSmall ,
						modifier = Modifier
							.fillMaxWidth()
							.placeholder(
								visible = loading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
													   ),
						textAlign = TextAlign.Center
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

//preview of the juz list item
@Preview
@Composable
fun JuzListItemUIPreview()
{
	NimazTheme {
		JuzListItemUI(
				juzNumber = "1" ,
				name = "الفاتحة" ,
				tname = "Al-Faatiha" ,
				onNavigateToAyatScreen = { _, _, _, _ -> } ,
				loading = false ,
					 )
	}
}