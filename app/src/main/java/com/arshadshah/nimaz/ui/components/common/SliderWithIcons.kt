package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

//custom slider encased in a card to make it look better and two icons before and after the slider
@Composable
fun SliderWithIcons(
	value : Float ,
	onValueChange : (Float) -> Unit ,
	valueRange : ClosedFloatingPointRange<Float> ,
	modifier : Modifier = Modifier ,
	leadingIcon : Painter ,
	trailaingIcon : Painter ,
	contentDescription1 : String ,
	contentDescription2 : String ,
	trailingIconSize : Dp = 24.dp ,
	leadingIconSize : Dp = 24.dp ,
				   )
{
	ElevatedCard(
			modifier = modifier
				.padding(vertical = 8.dp)
				.fillMaxWidth()
				.wrapContentHeight() ,
			content = {
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.wrapContentHeight() ,
						verticalAlignment = Alignment.CenterVertically ,
						horizontalArrangement = Arrangement.SpaceBetween
				   ) {
					Icon(
							modifier = Modifier
								.padding(start = 4.dp)
								.size(leadingIconSize)
								.clickable {
									onValueChange(value - 1)
								} ,
							painter = leadingIcon ,
							contentDescription = contentDescription1
						)
					Slider(
							value = value ,
							onValueChange = onValueChange ,
							valueRange = valueRange ,
							modifier = Modifier.width(200.dp)
						  )
					//click on this icon increses the font size
					Icon(
							modifier = Modifier
								.padding(end = 4.dp)
								.size(trailingIconSize)
								.clickable {
									onValueChange(value + 1)
								} ,
							painter = trailaingIcon ,
							contentDescription = contentDescription2
						)
				}
			}
				)
}