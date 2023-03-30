package com.arshadshah.nimaz.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun nimazCardShapes() = Shapes(
		extraSmall = RoundedCornerShape(8.dp) ,
		small = RoundedCornerShape(10.dp) ,
		medium = RoundedCornerShape(16.dp) ,
		large = RoundedCornerShape(24.dp) ,
		extraLarge = RoundedCornerShape(32.dp)
							  )
