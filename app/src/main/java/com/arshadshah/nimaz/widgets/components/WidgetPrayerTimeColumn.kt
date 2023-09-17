package com.arshadshah.nimaz.widgets.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

@Composable
fun WidgetPrayerTimeColumn(name : String , time : String , modifier : GlanceModifier)
{
	Column(
			 modifier = modifier.fillMaxHeight().padding(top = 14.dp, bottom = 8.dp, start = 4.dp, end = 4.dp).background(colorProvider = GlanceTheme.colors.secondaryContainer) ,
			 verticalAlignment = Alignment.CenterVertically ,
			 horizontalAlignment = Alignment.CenterHorizontally
		  ) {
		val modifierInternal = GlanceModifier.defaultWeight().padding(2.dp)
		Text(text = name,modifier=modifierInternal, style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = TextUnit(
				 16F , TextUnitType.Sp
																																						 ),
																													 fontWeight = FontWeight.Medium))
		Text(text = time,modifier=modifierInternal, style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = TextUnit(
				 16F , TextUnitType.Sp
																											  )))
	}
}