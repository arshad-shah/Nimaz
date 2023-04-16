package com.arshadshah.nimaz.ui.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun SettingsGroup(
	modifier : Modifier = Modifier ,
	title : @Composable (() -> Unit)? = null ,
	content : @Composable ColumnScope.() -> Unit ,
				 )
{
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = modifier
				.padding(8.dp)
				) {
		Column(
				modifier = modifier.fillMaxWidth() ,
			  ) {
			if (title != null)
			{
				SettingsGroupTitle(title)
				Divider(
						modifier = Modifier.fillMaxWidth() ,
						color = MaterialTheme.colorScheme.outline
					   )
			}
			content()
		}
	}
}

@Composable
internal fun SettingsGroupTitle(title : @Composable () -> Unit)
{
	Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(64.dp)
				.padding(horizontal = 16.dp) ,
			contentAlignment = Alignment.CenterStart
	   ) {
		val primary = MaterialTheme.colorScheme.secondary
		val titleStyle = MaterialTheme.typography.titleLarge.copy(color = primary)
		ProvideTextStyle(value = titleStyle) { title() }
	}
}

@Preview
@Composable
internal fun SettingsGroupPreview()
{
	NimazTheme {
		SettingsGroup(
				title = { Text(text = "Title") }
					 ) {
			Box(
					modifier = Modifier
						.height(64.dp)
						.fillMaxWidth() ,
					contentAlignment = Alignment.Center ,
			   ) {
				Text(text = "Settings group")
			}
		}
	}
}