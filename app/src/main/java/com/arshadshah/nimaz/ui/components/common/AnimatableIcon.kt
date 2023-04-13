package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R

@Composable
fun AnimatableIcon(
	modifier: Modifier = Modifier ,
	painter : Painter ,
	contentDescription : String? = null ,
	iconSize: Dp = 24.dp ,
	scale: Float = 1f ,
	color: Color = Color.Unspecified ,
	onClick: () -> Unit
				  ) {
	// Animation params
	val animatedScale: Float by animateFloatAsState(
			targetValue = scale,
												   )
	val animatedColor by animateColorAsState(
			targetValue = color,
											)

	IconButton(
			onClick = onClick,
			modifier = modifier.size(iconSize).indication(MutableInteractionSource(),null)
			  ) {
		Icon(
				painter = painter,
				contentDescription = contentDescription,
				tint = animatedColor,
				modifier = modifier.scale(animatedScale)
			)
	}
}

@Preview(group = "Icon")
@Composable
fun PreviewIcon() {
	Surface(
			modifier = Modifier.fillMaxWidth().size(100.dp) ,
		   ) {

		var selected by remember {
			mutableStateOf(false)
		}

		AnimatableIcon(
				painter = if (selected) painterResource(id = R.drawable.dashboard_icon) else painterResource(id = R.drawable.dashboard_icon_empty) ,
				scale = if (selected) 1.5f else 1f ,
				color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) ,
					  ) {
			selected = !selected
		}
	}
}