package com.arshadshah.nimaz.ui.components.ui.settings.internal

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable

@Composable
internal fun SettingsTileTitle(title : @Composable () -> Unit)
{
	ProvideTextStyle(value = MaterialTheme.typography.titleMedium) {
		title()
	}
}