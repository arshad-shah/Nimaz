package com.arshadshah.nimaz.ui.components.ui.loaders

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color

@Composable
fun loadingShimmerEffect() : Brush
{

	//These colors will be used on the brush. The lightest color should be in the middle

	val gradient = listOf(
			Color.LightGray.copy(alpha = 0.9f) , //darker grey (90% opacity)
			Color.LightGray.copy(alpha = 0.3f) , //lighter grey (30% opacity)
			Color.LightGray.copy(alpha = 0.9f)
						 )

	val transition = rememberInfiniteTransition() // animate infinite times

	//create a smooth animation from left to right
	val xAnimation = transition.animateFloat(
			initialValue = 0f ,
			targetValue = 2000f ,
			animationSpec = infiniteRepeatable(
					animation = tween(
							durationMillis = 1500 ,
							easing = LinearEasing ,
									 ) ,
					repeatMode = RepeatMode.Restart
											  )
											)
	//a gradient that will be used to create the shimmer effect
	//its like a conveyor belt that moves from left to right
	return linearGradient(
			colors = gradient ,
			start = Offset(xAnimation.value , 0f) ,
			end = Offset(xAnimation.value - 800f , 0f)
						 )
}