package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.delay
import java.time.LocalDateTime

//Banner Variant
sealed class BannerVariant
{

	object Success : BannerVariant()
	object Error : BannerVariant()
	object Info : BannerVariant()
	object Warning : BannerVariant()
}

@Composable
fun BannerSmall(
	modifier : Modifier = Modifier ,
	variant : BannerVariant = BannerVariant.Info ,
	title : String ,
	message : String? = null ,
	onClick : () -> Unit = {} ,
	showFor : Int = 3000 ,
	paddingValues : PaddingValues? = null ,
	isOpen : MutableState<Boolean> = remember {
		mutableStateOf(true)
	} ,
	dismissable : Boolean = false ,
			   )
{
	val sharedPref = PrivateSharedPreferences(LocalContext.current)
	val colors = mapOf(
			BannerVariant.Success to Color(0xFF388E3C) ,
			BannerVariant.Error to Color(0xFFD50000) ,
			BannerVariant.Info to Color(0xFF3F51B5) ,
			BannerVariant.Warning to Color(0xFFFFA900) ,
					  )
	val textColor = Color(0xFFFFFFFF)

	isOpen.value = sharedPref.getDataBoolean("$title-bannerIsOpen" , true)
	LaunchedEffect(Unit) {
		if (! dismissable)
		{
			if (showFor > 0)
			{
				delay(showFor.toLong())
				isOpen.value = false
				sharedPref.saveDataBoolean("$title-bannerIsOpen" , false)
				sharedPref.saveData("$title-bannerIsOpen-time" , LocalDateTime.now().toString())
			}
		}
	}
	if (isOpen.value)
	{
		//if its a success banner we use the success color and a check icon
		//if its an error banner we use the error color and a cross icon
		//if its an info banner we use the info color and an info icon
		//if its a warning banner we use the warning color and a warning icon
		ElevatedCard(
				shape = MaterialTheme.shapes.extraLarge ,
				modifier = modifier
					.padding(paddingValues ?: PaddingValues(8.dp))
					.clickable(
							interactionSource = remember { MutableInteractionSource() } ,
							enabled = true ,
							indication = if (onClick != {}) rememberRipple() else null ,
							role = Role.Button ,
							onClickLabel = "$title Banner" ,
							onClick = {
								onClick()
								isOpen.value = false
								sharedPref.saveDataBoolean("$title-bannerIsOpen" , false)
								sharedPref.saveData("$title-bannerIsOpen-time" , LocalDateTime.now().toString())
							} ,
							  ) ,
				//cardColors = CardColors(backgroundColor = Color(0xFFE0E0E0)),
				colors = CardDefaults.elevatedCardColors(
						containerColor = colors[variant] ?: Color(0xFFE0E0E0) ,
														) ,
					) {
			Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(8.dp) ,
					verticalAlignment = Alignment.CenterVertically ,
			   ) {
				//show the appropriate icon based on the banner variant
				Icon(
						painter = when (variant)
						{
							is BannerVariant.Success -> painterResource(id = R.drawable.checkbox_icon)
							is BannerVariant.Error -> painterResource(id = R.drawable.cross_circle_icon)
							is BannerVariant.Info -> painterResource(id = R.drawable.info_icon)
							is BannerVariant.Warning -> painterResource(id = R.drawable.warning_icon)
						} ,
						contentDescription = null ,
						modifier = Modifier
							.size(24.dp)
							.weight(0.1f) ,
						tint = textColor ,
					)
				Column(
						modifier = Modifier
							.weight(0.9f)
							.fillMaxWidth()
							.padding(start = 8.dp) ,
						verticalArrangement = Arrangement.Center ,
					  ) {
					//title
					Text(
							text = title ,
							style = MaterialTheme.typography.titleMedium ,
							color = textColor ,
						)
					//message
					Text(
							text = message ?: "" ,
							style = MaterialTheme.typography.bodySmall ,
							color = textColor ,
						)
				}
				if (dismissable)
				{
					IconButton(
							onClick = {
								isOpen.value = false
								sharedPref.saveDataBoolean("$title-bannerIsOpen" , false)
								sharedPref.saveData("$title-bannerIsOpen-time" , LocalDateTime.now().toString())
							} ,
							modifier = Modifier
								.size(32.dp) ,
							  ) {
						Icon(
								painter = painterResource(id = R.drawable.cross_icon) ,
								contentDescription = "Dismiss" ,
								tint = textColor ,
								modifier = Modifier
									.size(32.dp)
									.padding(end = 8.dp , start = 8.dp) ,
							)
					}
				}
			}
		}
	}
}

@Composable
fun BannerLarge(
	modifier : Modifier = Modifier ,
	variant : BannerVariant = BannerVariant.Info ,
	title : String ,
	message : String? = null ,
	onClick : () -> Unit = {} ,
	showFor : Int = 3000 ,
	isOpen : MutableState<Boolean> ,
	onDismiss : () -> Unit ,
			   )
{
	val sharedPref = PrivateSharedPreferences(LocalContext.current)
	val colors = mapOf(
			BannerVariant.Success to Color(0xFF388E3C) ,
			BannerVariant.Error to Color(0xFFD50000) ,
			BannerVariant.Info to Color(0xFF3F51B5) ,
			BannerVariant.Warning to Color(0xFFFFA900) ,
					  )
	val textColor = Color(0xFFFFFFFF)

	isOpen.value = sharedPref.getDataBoolean("$title-bannerOpen" , true)

	LaunchedEffect(Unit) {
		if (showFor > 0)
		{
			delay(showFor.toLong())
			isOpen.value = false
			sharedPref.saveDataBoolean("$title-bannerOpen" , false)
			sharedPref.saveData("$title-bannerIsOpen-time" , LocalDateTime.now().toString())
		}
	}
	if (isOpen.value)
	{
		//if its a success banner we use the success color and a check icon
		//if its an error banner we use the error color and a cross icon
		//if its an info banner we use the info color and an info icon
		//if its a warning banner we use the warning color and a warning icon
		ElevatedCard(
				shape = MaterialTheme.shapes.extraLarge ,
				modifier = modifier
					.padding(top = 8.dp , bottom = 0.dp , start = 8.dp , end = 8.dp)
					.clickable(
							interactionSource = remember { MutableInteractionSource() } ,
							enabled = true ,
							indication = if (onClick != {}) rememberRipple() else null ,
							role = Role.Button ,
							onClickLabel = "$title Banner" ,
							onClick = {
								onClick()
								isOpen.value = false
								sharedPref.saveDataBoolean("$title-bannerOpen" , false)
								sharedPref.saveData("$title-bannerIsOpen-time" , LocalDateTime.now().toString())
							} ,
							  ) ,
				//cardColors = CardColors(backgroundColor = Color(0xFFE0E0E0)),
				colors = CardDefaults.elevatedCardColors(
						containerColor = colors[variant] ?: Color(0xFFE0E0E0) ,
														) ,
					) {
			Column(
					modifier = Modifier.fillMaxWidth() ,
					verticalArrangement = Arrangement.Center ,
					horizontalAlignment = Alignment.CenterHorizontally ,
				  ) {
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(8.dp) ,
						verticalAlignment = Alignment.CenterVertically ,
						horizontalArrangement = Arrangement.SpaceBetween ,
				   ) {
					Row(
							modifier = Modifier.padding(
									top = 8.dp ,
									bottom = 0.dp ,
									start = 0.dp ,
									end = 8.dp
													   ) ,
							verticalAlignment = Alignment.CenterVertically ,
							horizontalArrangement = Arrangement.Start ,
					   ) {
						Icon(
								painter = when (variant)
								{
									is BannerVariant.Success -> painterResource(id = R.drawable.checkbox_icon)
									is BannerVariant.Error -> painterResource(id = R.drawable.cross_circle_icon)
									is BannerVariant.Info -> painterResource(id = R.drawable.info_icon)
									is BannerVariant.Warning -> painterResource(id = R.drawable.warning_icon)
								} ,
								contentDescription = null ,
								modifier = Modifier
									.size(24.dp) ,
								tint = textColor ,
							)
						//title
						Text(
								text = title ,
								style = MaterialTheme.typography.titleMedium ,
								color = textColor ,
								modifier = Modifier.padding(start = 8.dp)
							)
					}
					//dismiss button
					IconButton(
							onClick = {
								onDismiss.invoke()
								isOpen.value = false
								sharedPref.saveDataBoolean("$title-bannerOpen" , false)
								sharedPref.saveData("$title-bannerIsOpen-time" , LocalDateTime.now().toString())
							} ,
							modifier = Modifier
								.size(32.dp) ,
							  ) {
						Icon(
								painter = painterResource(id = R.drawable.cross_icon) ,
								contentDescription = "Dismiss" ,
								tint = textColor ,
								modifier = Modifier
									.size(32.dp)
									.padding(end = 8.dp , start = 8.dp) ,
							)
					}
				}
				//message
				Text(
						text = message ?: "" ,
						style = MaterialTheme.typography.bodyMedium ,
						color = textColor ,
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp , vertical = 8.dp)
					)
			}
		}
	}
}


@Preview(
		showBackground = true ,
		)
@Composable
fun BannerPreviewWarning()
{
	BannerSmall(
			variant = BannerVariant.Warning ,
			title = "Warning" ,
			message = "This is a warning banner" ,
			   )
}

@Preview(showBackground = true)
@Composable
fun BannerPreviewError()
{
	BannerSmall(
			variant = BannerVariant.Error ,
			title = "Error" ,
			message = "This is an error banner" ,
			   )
}

@Preview(showBackground = true)
@Composable
fun BannerPreviewSuccess()
{
	BannerSmall(
			variant = BannerVariant.Success ,
			title = "Success" ,
			message = "This is a success banner" ,
			   )
}

@Preview(showBackground = true)
@Composable
fun BannerPreviewInfo()
{
	BannerSmall(
			variant = BannerVariant.Info ,
			title = "Info" ,
			message = "This is an info banner" ,
			   )
}

@Preview(showBackground = true)
@Composable
fun BannerPreviewInfoDismiss()
{
	BannerSmall(
			variant = BannerVariant.Info ,
			title = "Info" ,
			message = "This is an info banner" ,
			dismissable = true ,
			   )
}

//a dismissable banner
@Preview(showBackground = true)
@Composable
fun BannerPreviewDismissable()
{
	val isOpen = remember {
		mutableStateOf(true)
	}
	BannerLarge(
			variant = BannerVariant.Info ,
			title = "Info" ,
			message = "This is an info banner with a dismiss button and a lot of text to show how it looks when the text is too long" ,
			isOpen = isOpen ,
			onDismiss = {
				isOpen.value = false
			} ,
			   )
}