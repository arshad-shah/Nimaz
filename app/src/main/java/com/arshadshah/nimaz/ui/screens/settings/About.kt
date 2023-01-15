package com.arshadshah.nimaz.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arshadshah.nimaz.ui.components.bLogic.settings.AuthorDetails

@Composable
fun About(paddingValues : PaddingValues)
{
	Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState() , true)
				.padding(paddingValues)
				.fillMaxWidth()
				.fillMaxHeight()
		  ) {
		AuthorDetails()
	}
}