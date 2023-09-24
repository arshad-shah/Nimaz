package com.arshadshah.nimaz.ui.components.quran

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_SURAH_ITEM
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun SurahListUI(
	surahs : ArrayList<Surah> ,
	onNavigateToAyatScreen : (String , Boolean , String , Int?) -> Unit ,
	loading : Boolean ,
			   )
{
	val state = rememberLazyListState()
	LazyColumn(
			 state = state ,
			 userScrollEnabled = ! loading ,
			 modifier = Modifier.testTag(AppConstants.TEST_TAG_QURAN_SURAH)
			  ) {
		items(surahs.size) { index ->
			SurahListItemUI(
					 loading = loading ,
					 surahNumber = surahs[index].number.toString() ,
					 surahAyaAmount = surahs[index].numberOfAyahs.toString() ,
					 surahName = surahs[index].name ,
					 englishName = surahs[index].englishName ,
					 englishNameTranslation = surahs[index].englishNameTranslation ,
					 type = surahs[index].revelationType ,
					 rukus = surahs[index].rukus.toString() ,
					 onNavigateToAyatScreen = onNavigateToAyatScreen
						   )
		}
	}
}

@Composable
fun SurahListItemUI(
	surahNumber : String ,
	surahAyaAmount : String ,
	surahName : String ,
	englishName : String ,
	englishNameTranslation : String ,
	type : String ,
	rukus : String ,
	onNavigateToAyatScreen : (String , Boolean , String , Int?) -> Unit ,
	context : Context = LocalContext.current ,
	loading : Boolean ,
				   )
{

	val translationType =
		PrivateSharedPreferences(context).getData(
				 key = AppConstants.TRANSLATION_LANGUAGE ,
				 s = "English"
												 )
	val language = when (translationType)
	{
		"English" -> "english"
		"Urdu" -> "urdu"
		else -> "english"
	}

	Card(
			 colors = CardDefaults.elevatedCardColors(
					  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp) ,
					  contentColor = MaterialTheme.colorScheme.onSurface ,
					  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
					  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
													 ) ,
			 shape = MaterialTheme.shapes.extraLarge ,
			 modifier = Modifier
				 .padding(vertical = 4.dp , horizontal = 8.dp)
				 .fillMaxWidth()
				 .clip(MaterialTheme.shapes.extraLarge)
				 .clickable(
						  enabled = ! loading ,
						   ) {
					 onNavigateToAyatScreen(surahNumber , true , language , null)
				 }
		) {
		Row(
				 modifier = Modifier
					 .padding(8.dp)
					 .testTag(
							  TEST_TAG_SURAH_ITEM + surahNumber
							 )
		   ) {
			Text(
					 modifier = Modifier
						 .align(Alignment.CenterVertically)
						 .padding(start = 8.dp)
						 .placeholder(
								  visible = loading ,
								  color = MaterialTheme.colorScheme.outline ,
								  shape = RoundedCornerShape(4.dp) ,
								  highlight = PlaceholderHighlight.shimmer(
										   highlightColor = Color.White ,
																		  )
									 ) ,
					 text = "$surahNumber." ,
					 style = MaterialTheme.typography.bodyMedium ,
					 textAlign = TextAlign.Center
				)

			Column(
					 modifier = Modifier
						 .weight(0.4f)
						 .align(Alignment.CenterVertically)
				  ) {
				Text(
						 text = englishName ,
						 style = MaterialTheme.typography.titleSmall ,
						 modifier = Modifier
							 .placeholder(
									  visible = loading ,
									  color = MaterialTheme.colorScheme.outline ,
									  shape = RoundedCornerShape(4.dp) ,
									  highlight = PlaceholderHighlight.shimmer(
											   highlightColor = Color.White ,
																			  )
										 )
							 .fillMaxWidth() ,
						 textAlign = TextAlign.Center
					)
				//apply quran font
				Text(
						 text = surahName ,
						 style = MaterialTheme.typography.titleLarge ,
						 fontFamily = utmaniQuranFont ,
						 fontSize = 28.sp ,
						 fontWeight = FontWeight.SemiBold ,
						 modifier = Modifier
							 .placeholder(
									  visible = loading ,
									  color = MaterialTheme.colorScheme.outline ,
									  shape = RoundedCornerShape(4.dp) ,
									  highlight = PlaceholderHighlight.shimmer(
											   highlightColor = Color.White ,
																			  )
										 )
							 .fillMaxWidth() ,
						 textAlign = TextAlign.Center
					)
				Text(
						 text = englishNameTranslation ,
						 style = MaterialTheme.typography.titleSmall ,
						 modifier = Modifier
							 .placeholder(
									  visible = loading ,
									  color = MaterialTheme.colorScheme.outline ,
									  shape = RoundedCornerShape(4.dp) ,
									  highlight = PlaceholderHighlight.shimmer(
											   highlightColor = Color.White ,
																			  )
										 )
							 .fillMaxWidth() ,
						 textAlign = TextAlign.Center
					)
			}

			Column(
					 modifier = Modifier
						 .weight(0.3f)
						 .align(Alignment.CenterVertically)
						 .fillMaxWidth()
				  ) {
				MetadataTextUI(
						 heading = "Type" ,
						 value = type ,
						 loading = loading
							  )
				MetadataTextUI(
						 heading = "Ayat" ,
						 value = surahAyaAmount ,
						 loading = loading
							  )

				MetadataTextUI(
						 heading = "Ruku" ,
						 value = rukus ,
						 loading = loading
							  )
			}
			//an arrow right icon
			Icon(
					 painter = painterResource(id = R.drawable.angle_small_right_icon) ,
					 contentDescription = "Clear" ,
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

// text component for the type, ayat, ruku
//looks like this
//Type: Meccan
//the heading is the type, ayat, ruku and the value is the value of the type, ayat, ruku
//we supply the heading and the value
@Composable
fun MetadataTextUI(heading : String , value : String , loading : Boolean)
{
	Row(
			 modifier = Modifier ,
			 verticalAlignment = Alignment.CenterVertically ,
			 horizontalArrangement = Arrangement.SpaceBetween
	   ) {
		//heading text
		Text(
				 text = "$heading: " ,
				 style = MaterialTheme.typography.titleSmall ,
				 modifier = Modifier
					 .padding(4.dp)
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
		//value text
		Text(
				 text = value ,
				 style = MaterialTheme.typography.titleSmall ,
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
				 textAlign = TextAlign.Center
			)
	}
}


//preview
@Preview(showBackground = true)
@Composable
fun SurahListItemUIPreview()
{
	//set the theme
	NimazTheme {
		SurahListItemUI(
				 surahNumber = "1" ,
				 surahAyaAmount = "7" ,
				 surahName = "الفاتحة" ,
				 englishName = "Al-Faatiha" ,
				 englishNameTranslation = "The Opening" ,
				 type = "Meccan" ,
				 rukus = "1" ,
				 onNavigateToAyatScreen = { surahNumber , isSurah , language , ayahNumber -> } ,
				 loading = false
					   )
	}
}

//surah list item ui with the long surah name and the surah type
@Preview(showBackground = true , device = "spec:width=1080px,height=2340px,dpi=440,isRound=true")
@Composable
fun SurahListItemUIPreview2()
{
	//set the theme
	NimazTheme {
		SurahListItemUI(
				 surahNumber = "114" ,
				 surahAyaAmount = "182" ,
				 surahName = "المعارج" ,
				 englishName = "As-Saaffaat" ,
				 englishNameTranslation = "Those drawn up in Ranks" ,
				 type = "Medinan" ,
				 rukus = "1" ,
				 onNavigateToAyatScreen = { surahNumber , isSurah , language , ayahNumber -> } ,
				 loading = false
					   )
	}
}