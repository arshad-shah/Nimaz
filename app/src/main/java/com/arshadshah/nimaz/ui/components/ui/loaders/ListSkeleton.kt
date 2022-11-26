package com.arshadshah.nimaz.ui.components.ui.loaders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun ListSkeletonLoader(modifier : Modifier = Modifier , brush : Brush)
{
	Column(modifier = modifier) {
		repeat(6) {
			ItemSkeleton(brush = brush)
		}
	}
}

@Composable
fun ItemSkeleton(brush : Brush)
{
	Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp)
				.height(50.dp)
				.clip(RoundedCornerShape(8.dp))
				.background(brush)
	   ) {
		Spacer(modifier = Modifier.width(16.dp))
		Column(
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight()
					.padding(8.dp) ,
				verticalArrangement = Arrangement.SpaceEvenly
			  ) {
			Spacer(modifier = Modifier.height(8.dp))
			Spacer(modifier = Modifier.height(8.dp))
		}
	}
}

@Composable
@Preview(showBackground = true)
fun ShimmerPreview()
{
	ListSkeletonLoader(brush = loadingShimmerEffect())
}