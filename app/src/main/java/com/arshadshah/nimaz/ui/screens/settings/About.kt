package com.arshadshah.nimaz.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_ABOUT_PAGE
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
				.testTag(TEST_TAG_ABOUT_PAGE)
		  ) {
		AuthorDetails()
	}
}