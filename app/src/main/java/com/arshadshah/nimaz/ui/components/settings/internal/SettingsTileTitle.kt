package com.arshadshah.nimaz.ui.components.settings.internal

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

@Composable
internal fun SettingsTileTitle(title : @Composable () -> Unit)
{
	ProvideTextStyle(value = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp)) {
		title()
	}
}