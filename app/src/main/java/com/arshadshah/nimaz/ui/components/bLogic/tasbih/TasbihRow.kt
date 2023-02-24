package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont


@Composable
fun TasbihRow(
	englishName : String ,
	arabicName : String ,
	translationName : String ,
	onNavigateToTasbihScreen : ((String , String , String) -> Unit)? = null
			 )
{
	ElevatedCard(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp)
				.clickable(
						//disable it if onNavigateToTasbihScreen has no implementation
						enabled = onNavigateToTasbihScreen != null ,
						  ) {
					if (onNavigateToTasbihScreen != null)
					{
						onNavigateToTasbihScreen(arabicName , englishName , translationName)
					}
				} ,
				) {
		Row(
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
		   ) {
			Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(4.dp)
						.weight(0.80f),
					horizontalAlignment = Alignment.Start ,
					verticalArrangement = Arrangement.Center ,
				  ) {
				CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
					Text(
							text = arabicName ,
							style = MaterialTheme.typography.titleLarge ,
							fontSize = 28.sp ,
							fontFamily = utmaniQuranFont ,
							modifier = Modifier
								.padding(4.dp)
								.fillMaxWidth() ,
							color = MaterialTheme.colorScheme.onSurface ,
						)
				}
				Text(
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
						text = englishName ,
						style = MaterialTheme.typography.titleSmall ,
						color = MaterialTheme.colorScheme.onSurface ,
					)
				Text(
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
						text = translationName ,
						style = MaterialTheme.typography.titleSmall ,
						color = MaterialTheme.colorScheme.onSurface ,
					)
			}
			if(onNavigateToTasbihScreen != null)
			{
				Icon(
						painter = painterResource(id = R.drawable.angle_small_right_icon) ,
						contentDescription = "Navigate to chapter" ,
						modifier = Modifier
							.size(24.dp)
							.fillMaxWidth()
							.align(Alignment.CenterVertically)
					)
			}
		}
	}

}

@Preview
@Composable
fun TasbihRowPreview()
{
	TasbihRow(
			englishName = "Tasbih" ,
			arabicName = "تسبيح" ,
			translationName = "Praise" ,
			onNavigateToTasbihScreen = null
			 )
}