package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.ui.icons.NineNine
import com.arshadshah.nimaz.ui.components.ui.icons.PlusMinusTasbih
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Github
import compose.icons.feathericons.List


@Composable
fun FeatureCard(
	onNavigateToTasbihScreen : (String) -> Unit ,
	onNavigateToNames : () -> Unit ,
	paddingValues : PaddingValues ,
	onNavigateToListOfTasbeeh : () -> Unit ,
	onNavigateToShadah : () -> Unit ,
	onNavigateToZakat : () -> Unit ,
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
				icon = {
					Icon(
							modifier = Modifier.size(48.dp) ,
							imageVector = Icons.PlusMinusTasbih ,
							contentDescription = "Tasbih" ,
						)
				} ,
				onClick = {
					onNavigateToTasbihScreen(" ")
				} ,
				title = "Tasbih" ,
				  )

		//linkdIn link
		LinkButton(
				icon = {
					Icon(
							modifier = Modifier.size(28.dp) ,
							imageVector = Icons.NineNine ,
							contentDescription = "Names of Allah" ,
						)
				} ,
				onClick = {
					onNavigateToNames()
				} ,
				title = "Names of Allah" ,
				  )
		//email link
		LinkButton(
				icon = {
					Icon(
							modifier = Modifier.size(48.dp) ,
							imageVector = FeatherIcons.List ,
							contentDescription = "Duas" ,
						)
				} ,
				onClick = {
					onNavigateToListOfTasbeeh()
				} ,
				title = "Duas" ,
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
				icon = {
					//get the icon from drawable folder
					Icon(
							modifier = Modifier.size(48.dp) ,
							painter = painterResource(id = R.drawable.shahadah) ,
							contentDescription = "Shadah" ,
						)
				} ,
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
	icon : @Composable () -> Unit ,
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
			IconButton(
					onClick = onClick ,
					enabled = true ,
					content = icon
					  )

			Text(
					text = title ,
					modifier = Modifier.padding(start = 8.dp) ,
					style = MaterialTheme.typography.titleLarge ,
				)
			//an icon of arrow to indicate that it is a link
			Icon(
					imageVector = FeatherIcons.ArrowRight ,
					contentDescription = "Link" ,
					modifier = Modifier.padding(start = 8.dp) ,
				)
		}
	}
}

@Preview
@Composable
fun LinkButtonPreview()
{
	LinkButton(
			icon = {
				Icon(
						imageVector = FeatherIcons.Github ,
						contentDescription = "Github"
					)
			} ,
			onClick = {} ,
			title = "Github"
			  )
}