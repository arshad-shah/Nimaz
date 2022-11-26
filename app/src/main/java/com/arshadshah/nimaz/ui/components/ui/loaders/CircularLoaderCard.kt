package com.arshadshah.nimaz.ui.components.ui.loaders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun CircularLoaderCard()
{
	Box(modifier = Modifier.fillMaxSize() , contentAlignment = Alignment.Center) {
		CircularProgressIndicator(
				color = MaterialTheme.colorScheme.primary ,
				modifier = Modifier
					.align(Alignment.Center)
					.size(100.dp) ,
				strokeWidth = 8.dp ,
								 )
	}
}