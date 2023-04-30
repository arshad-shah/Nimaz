package com.arshadshah.nimaz.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.settings.internal.SettingsTileIcon
import com.arshadshah.nimaz.ui.components.settings.internal.SettingsTileTexts

class ThemeOption(
	var themeName : String ,
	var themeKey : String ,
	var themeColor : Color ,
	var isSelected : Boolean ,
				 )

@Composable
fun ThemeGrid(
	themeOptions : List<ThemeOption> ,
	onThemeOptionSelected : (ThemeOption) -> Unit ,
			 )
{
	Column(
			modifier = Modifier
				.fillMaxWidth()
		  ) {
		//name of current theme
		Row(
				modifier = Modifier
					.fillMaxWidth() ,
				horizontalArrangement = Arrangement.SpaceBetween
		   ) {
			Row(
					modifier = Modifier ,
					verticalAlignment = Alignment.CenterVertically
			   ) {

				SettingsTileIcon(icon = {
					Icon(
							modifier = Modifier
								.size(24.dp) ,
							painter = painterResource(id = R.drawable.theme_icon) ,
							contentDescription = "Color"
						)
				})
				SettingsTileTexts(title = {
					Text(
							text = "Color" ,
						)
				} ,
								  subtitle = {
									  Text(
											  text = themeOptions.find { it.isSelected }?.themeName
												  ?: "" ,
										  )
								  })
			}
		}
		//the row
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp) ,
				horizontalArrangement = Arrangement.SpaceBetween
		   ) {
			themeOptions.forEachIndexed { index , themeOption ->
				//the theme item
				ToggleableCustomThemeItem(
						checked = themeOption.isSelected ,
						onCheckedChange = {
							onThemeOptionSelected(themeOption)
						} ,
						color = themeOption.themeColor ,
						//if the thtme key is SYSTEM then show the system icon
						icon = if (themeOption.themeKey == "SYSTEM")
						{

							@Composable {
								Icon(
										modifier = Modifier.size(24.dp) ,
										painter = painterResource(id = R.drawable.system_icon) ,
										contentDescription = "System" ,
										tint = MaterialTheme.colorScheme.surface
									)
							}
						} else
						{
							null
						}
										 )
			}
		}
	}
}

//a theme toggle item with a circle that gets highlighted witha  thick border when selected
@Composable
fun ToggleableCustomThemeItem(
	modifier : Modifier = Modifier ,
	checked : Boolean ,
	onCheckedChange : (Boolean) -> Unit ,
	color : Color = MaterialTheme.colorScheme.primary ,
	icon : @Composable (() -> Unit)? = null ,
							 )
{
	Box(
			modifier = modifier
				.border(
						width = 2.dp ,
						color = if (checked) Color.Gray else Color.Transparent ,
						shape = CircleShape
					   ) ,
			contentAlignment = Alignment.Center ,
	   ) {
		//the circle
		Box(

				modifier = modifier
					.padding(6.dp)
					.size(48.dp)
					.indication(
							indication = null ,
							interactionSource = MutableInteractionSource() ,
							   )
					.clickable {
						onCheckedChange(! checked)
					}
					.background(
							color = color ,
							shape = CircleShape
							   ) ,
				contentAlignment = Alignment.Center
		   ) {
			//the icon
			icon?.invoke()
		}
	}
}


@Preview(showBackground = true)
@Composable
//preview of the theme grid
fun ThemeGridPreview()
{
	ThemeGrid(
			themeOptions = listOf(
					ThemeOption("Light" , "light" , Color.White , true) ,
					ThemeOption("Dark" , "dark" , Color.Black , false) ,
					ThemeOption("Red" , "red" , Color.Red , false) ,
					ThemeOption("Green" , "green" , Color.Green , false) ,
					ThemeOption("Blue" , "blue" , Color.Blue , false) ,
								 ) ,
			onThemeOptionSelected = {}
			 )
}

//preview of ToggleableItemRow
@Preview(showBackground = true , name = "Theme Toggle Selected" , group = "Theme Toggle")
@Composable
fun ToggleableItemRowPreview()
{
	ToggleableCustomThemeItem(
			checked = true ,
			onCheckedChange = {} ,
			color = Color.Red ,
			icon = {
				Icon(
						modifier = Modifier
							.size(24.dp) ,
						painter = painterResource(id = R.drawable.settings_icon) ,
						contentDescription = "Selected"
					)
			}
							 )
}

@Preview(showBackground = true , name = "Theme Toggle Unselected" , group = "Theme Toggle")
@Composable
fun ToggleableItemRowPreview2()
{
	ToggleableCustomThemeItem(
			checked = false ,
			onCheckedChange = {} ,
			color = Color.Red
							 )
}