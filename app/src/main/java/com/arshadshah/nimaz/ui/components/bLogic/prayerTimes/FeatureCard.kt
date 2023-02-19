package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R


@Composable
fun FeatureCard(
	onNavigateToTasbihScreen : (String) -> Unit ,
	onNavigateToNames : () -> Unit ,
	paddingValues : PaddingValues ,
	onNavigateToListOfTasbeeh : () -> Unit ,
	onNavigateToShadah : () -> Unit ,
	onNavigateToZakat : () -> Unit ,
	onNavigateToPrayerTracker : () -> Unit ,
			   )
{
	Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues) ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Center
		  ) {
		//website link
		LinkButton(
				painter = painterResource(id = R.drawable.tasbih) ,
				content = "Tasbih" ,
				onClick = {
					onNavigateToTasbihScreen(" ")
				} ,
				title = "Tasbih" ,
				  )

		//linkdIn link
		LinkButton(
				painter = painterResource(id = R.drawable.names_of_allah) ,
				content = "Names of Allah" ,
				onClick = {
					onNavigateToNames()
				} ,
				title = "Names of Allah" ,
				  )
		//email link
		LinkButton(
				painter = painterResource(id = R.drawable.dua) ,
				content = "Dua" ,
				onClick = {
					onNavigateToListOfTasbeeh()
				} ,
				title = "Duas" ,
				  )
		LinkButton(
				painter = painterResource(id = R.drawable.calendar_icon) ,
				content = "Calender" ,
				onClick = {
					onNavigateToPrayerTracker()
				} ,
				title = "Calender" ,
				  )
		//TODO: IN PROGRESS
//		LinkButton(
//				icon = {
//					//get the icon from drawable folder
//					Icon(
//							modifier = Modifier.size(48.dp) ,
//							imageVector = FeatherIcons.Github ,
//							contentDescription = "Zakat Calculator" ,
//						)
//				} ,
//				onClick = {
//					onNavigateToZakat()
//				},
//				title = "Zakah Calculator"
//				  )
		LinkButton(
				painter = painterResource(id = R.drawable.shahadah) ,
				content = "Shahadah" ,
				onClick = {
					onNavigateToShadah()
				} ,
				title = "Shahadah"
				  )

	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkButton(
	painter : Painter ,
	content : String ,
	onClick : () -> Unit ,
	title : String = "" ,
			  )
{
	ElevatedCard(
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth() ,
			onClick = onClick
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp) ,
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			Image(
					modifier = Modifier
						.size(48.dp) ,
					painter = painter ,
					contentDescription = content
				 )

			Text(
					text = title ,
					modifier = Modifier.padding(start = 8.dp) ,
					style = MaterialTheme.typography.titleLarge ,
				)
			//an icon of arrow to indicate that it is a link
			Icon(
					modifier = Modifier
						.size(24.dp)
						.padding(start = 8.dp) ,
					painter = painterResource(id = R.drawable.angle_small_right_icon) ,
					contentDescription = "Link" ,
				)
		}
	}
}

@Preview
@Composable
fun LinkButtonPreview()
{
	LinkButton(
			painter = painterResource(id = R.drawable.tasbih) ,
			content = "Tasbih" ,
			onClick = {} ,
			title = "Github"
			  )
}