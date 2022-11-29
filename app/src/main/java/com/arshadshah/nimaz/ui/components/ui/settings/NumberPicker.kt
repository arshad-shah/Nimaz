package com.arshadshah.nimaz.ui.components.ui.settings

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Composable
fun NumberPicker(
	modifier : Modifier = Modifier ,
	label : (Int) -> String = {
		it.toString()
	} ,
	value : Int ,
	onValueChange : (Int) -> Unit ,
	dividersColor : Color = MaterialTheme.colorScheme.outline ,
	range : List<Int> ,
	textStyle : TextStyle = LocalTextStyle.current ,
				)
{
	ListItemPicker(
			modifier = modifier ,
			label = label ,
			value = value ,
			onValueChange = onValueChange ,
			dividersColor = dividersColor ,
			list = range ,
			textStyle = textStyle
				  )
}