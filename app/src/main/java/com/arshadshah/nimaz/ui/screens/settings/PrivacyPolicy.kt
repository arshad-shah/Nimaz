package com.arshadshah.nimaz.ui.screens.settings

import android.text.Html
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable

@Composable
fun PrivacyPolicy(paddingValues : PaddingValues)
{
	LazyColumn(content = {
		item {
			Html.fromHtml(
					"<a href=\"https://arshadshah.com/privacy-policy\">Privacy Policy</a>" ,
					Html.FROM_HTML_MODE_LEGACY
						 ).toString()
		}
	} , contentPadding = paddingValues)
}