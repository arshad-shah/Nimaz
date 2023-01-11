package com.arshadshah.nimaz.ui.components.bLogic.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun AuthorDetails()
{

	//Designed and Created By: Arshad Shah
	//occupation: Associate Software Engineer
	//company: HMHco (Houghton Mifflin Harcourt)
	//Description: Loves to code, create, and learn new things.
	//Nimaz is a project that I created to learn more about Android development and to help others learn about Islam.
	//it is created as part of my final year project for my BSc in Computer Science.
	//it is a free and open source project that I hope will be useful to many people. I hope you enjoy it.
	ElevatedCard(
		modifier = Modifier
			.padding(8.dp)
			.fillMaxWidth() ,
		shape = RoundedCornerShape(8.dp) ,
		content = {
			Column(
					modifier = Modifier
						.padding(16.dp)
						.fillMaxWidth() ,
				  ) {
				Text(
						modifier = Modifier.fillMaxWidth() ,
						text = "Author Details" ,
						style = MaterialTheme.typography.titleLarge ,
						textAlign = TextAlign.Center
					)
				Divider(
						modifier = Modifier.padding(8.dp) ,
						color = MaterialTheme.colorScheme.outline,
						thickness = 1.dp
					)
				AuthorCustomText(
						rowModifier = Modifier.padding(8.dp) ,
						labelModifier = Modifier.fillMaxWidth(0.3f) ,
						textModifier = Modifier.fillMaxWidth() ,
						label = "Designed By:" ,
						text = "Arshad Shah",
						labelStyle = MaterialTheme.typography.labelLarge ,
						textStyle = MaterialTheme.typography.bodyMedium
					)
				AuthorCustomText(
						rowModifier = Modifier.padding(8.dp) ,
						labelModifier = Modifier.fillMaxWidth(0.3f) ,
						textModifier = Modifier.fillMaxWidth() ,
						label = "Developed By:" ,
						text = "Arshad Shah",
						labelStyle = MaterialTheme.typography.labelLarge ,
						textStyle = MaterialTheme.typography.bodyMedium
								)
				AuthorCustomText(
						rowModifier = Modifier.padding(8.dp) ,
						labelModifier = Modifier.fillMaxWidth(0.3f) ,
						textModifier = Modifier.fillMaxWidth() ,
						label = "Occupation:" ,
						text = "Associate Software Engineer",
						labelStyle = MaterialTheme.typography.labelLarge ,
						textStyle = MaterialTheme.typography.bodyMedium
								)
				AuthorCustomText(
						rowModifier = Modifier.padding(8.dp) ,
						labelModifier = Modifier.fillMaxWidth(0.3f) ,
						textModifier = Modifier.fillMaxWidth() ,
						label = "Company:" ,
						text = "HMHco \n(Houghton Mifflin Harcourt)",
						labelStyle = MaterialTheme.typography.labelLarge ,
						textStyle = MaterialTheme.typography.bodyMedium
								)
				Divider(
						modifier = Modifier.padding(8.dp) ,
						color = MaterialTheme.colorScheme.outline,
						thickness = 1.dp
					)
				//links
				Text(
						modifier = Modifier.fillMaxWidth() ,
						text = "Links" ,
						style = MaterialTheme.typography.titleLarge ,
						textAlign = TextAlign.Center
					)
				AuthorLinks()
				Divider(
						modifier = Modifier.padding(8.dp) ,
						color = MaterialTheme.colorScheme.outline,
						thickness = 1.dp
					)
				Text(
						modifier = Modifier.padding(8.dp) ,
						text = "Loves to code, create, and learn new things." ,
						style = MaterialTheme.typography.bodyMedium ,
					)
				Text(
						modifier = Modifier.padding(8.dp) ,
						text = "Nimaz is a project that I created to learn more about Android development and to help others learn about Islam." ,
						style = MaterialTheme.typography.bodyMedium ,
					)
				Text(
						modifier = Modifier.padding(8.dp) ,
						text = "It is created as part of my final year project for my BSc in Computer Science." ,
						style = MaterialTheme.typography.bodyMedium ,
					)
				Text(
						modifier = Modifier.padding(8.dp) ,
						text = "It is a free, Ad-free and open source project that I hope will be useful to many people.I hope you enjoy it." ,
						style = MaterialTheme.typography.bodyMedium ,
					)
			}
		} ,
	)
}

@Composable
fun AuthorCustomText(rowModifier: Modifier,
					 labelModifier: Modifier,
					 textModifier:Modifier,
					 label: String ,
					 text : String,
					 labelStyle: TextStyle,
					 textStyle: TextStyle)
{
	Row(
			modifier = rowModifier ,
			content = {
				Text(
						modifier = labelModifier ,
						text = label ,
						style = labelStyle ,
					)
				Text(
						modifier = textModifier ,
						text = text ,
						style = textStyle ,
					)
			}
		)
}


@Preview
@Composable
fun AuthorDetailsPreview()
{
	NimazTheme {
		AuthorDetails()
	}
}