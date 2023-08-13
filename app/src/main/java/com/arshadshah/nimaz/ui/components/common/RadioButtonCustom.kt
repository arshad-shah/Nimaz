package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

//a custom radio button with a checkmark
@Composable
fun RadioButtonCustom(
	selected : Boolean ,
	onClick : () -> Unit ,
	modifier : Modifier = Modifier ,
					 )
{
	//a circle which gets filled and a checkmark icon is shown when selected else just an empty circle
	Box(
			modifier = modifier
				.size(32.dp)
				.clip(CircleShape)
				.clickable(onClick = onClick) ,
			contentAlignment = Alignment.Center ,
	   ) {
		//the circle
		Box(
				modifier = Modifier
					.size(28.dp)
					.background(
							color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
									alpha = 0.2f
																																 ) ,
							shape = CircleShape
							   ) ,
				contentAlignment = Alignment.Center ,
		   ) {
			Crossfade(
					targetState = selected ,
					animationSpec = tween(durationMillis = 100 , easing = LinearEasing)
					 ) { selected ->
				//the checkmark icon
				if (selected)
				{
					Icon(
							painter = painterResource(id = R.drawable.check_icon) ,
							contentDescription = "checkmark" ,
							modifier = Modifier
								.size(24.dp)
								.padding(2.dp) ,
							tint = MaterialTheme.colorScheme.onPrimary ,
						)
				} else
				{
					Icon(
							painter = painterResource(id = R.drawable.circle_open_icon) ,
							contentDescription = "circle" ,
							modifier = Modifier.size(24.dp) ,
							tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) ,
						)
				}
			}
		}
	}
}

//a preview
@Preview(showBackground = true)
@Composable
fun RadioButtonCustomPreview()
{
	val selected = remember {
		mutableStateOf(true)
	}
	RadioButtonCustom(selected = selected.value , onClick = {
		selected.value = ! selected.value
	})
}