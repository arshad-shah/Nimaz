package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.ui.theme.quranFont

@Composable
fun DuaListItem(dua : Dua)
{
	ElevatedCard(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp)
				) {
		Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp)
			  ) {
			CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
				Text(
						text = dua.arabic_dua ,
						style = MaterialTheme.typography.titleLarge ,
						fontSize = 28.sp ,
						fontFamily = quranFont ,
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth()
					)
			}
			Text(
					text = dua.english_translation ,
					style = MaterialTheme.typography.titleMedium ,
					modifier = Modifier
						.padding(4.dp)
						.fillMaxWidth() ,
				)
			Row {
				Text(
						text = "Reference: ${dua.english_reference}" ,
						style = MaterialTheme.typography.titleSmall ,
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
					)
			}
		}
	}
}


@Preview
@Composable
fun DuaListItemPreview()
{
	val dua = Dua(
			1 ,
			1 ,
			0 ,
			"اللهم صل على محمد وآل محمد" ,
			"O Allah, send blessings on Muhammad and the family of Muhammad" ,
			"O Allah, send blessings on Muhammad and the family of Muhammad" ,
			"Sahih Muslim"
				 )
	DuaListItem(dua)
}