package com.arshadshah.nimaz.ui.components.ui.compass

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A custom text component that can be used to display text in the app.
 * it has a small header above the text.
 * @param header the header text
 * @param text the Text composable component
 * */
@Composable
fun CustomText(
	modifier : Modifier ,
	headingModifier : Modifier = Modifier ,
	textModifier : Modifier = Modifier ,
	heading : String ,
	text : String ,
			  )
{
	Column(modifier = modifier , horizontalAlignment = Alignment.CenterHorizontally) {
		Text(
				modifier = headingModifier ,
				text = heading ,
				textAlign = TextAlign.Center ,
				style = MaterialTheme.typography.titleSmall
			)
		Spacer(modifier = Modifier.padding(4.dp))
		Text(
				modifier = textModifier ,
				text = text ,
				textAlign = TextAlign.Center ,
				style = MaterialTheme.typography.titleLarge
			)
	}
}

@Preview
@Composable
fun CustomTextPreview()
{
	CustomText(modifier = Modifier.padding(8.dp) , heading = "Heading" , text = "Text")
}