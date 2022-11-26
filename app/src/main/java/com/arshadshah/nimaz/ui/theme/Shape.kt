package com.arshadshah.nimaz.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun nimazCardShapes() = Shapes(
		small = RoundedCornerShape(4.dp) ,
		medium = RoundedCornerShape(4.dp) ,
		large = RoundedCornerShape(0.dp)
							  )
