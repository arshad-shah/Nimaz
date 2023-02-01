package com.arshadshah.nimaz.ui.components.ui.general

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.*

//Banner Variant
sealed class BannerVariant {
	object Success : BannerVariant()
	object Error : BannerVariant()
	object Info : BannerVariant()
	object Warning : BannerVariant()
}

@Composable
fun Banner(
	modifier : Modifier = Modifier ,
	variant : BannerVariant = BannerVariant.Info ,
	title : String ,
	message : String? = null ,
	onDismiss : (() -> Unit)? = null
	)
{
	val isDismissable = remember {
		mutableStateOf(onDismiss != null)
	}
	//if its a success banner we use the success color and a check icon
	//if its an error banner we use the error color and a cross icon
	//if its an info banner we use the info color and an info icon
	//if its a warning banner we use the warning color and a warning icon
	ElevatedCard(
			modifier = modifier.border(
					width = 1.dp ,
					color = when (variant) {
						is BannerVariant.Success -> Color(0x3300FF0A)
						is BannerVariant.Error -> Color(0x33FF0000)
						is BannerVariant.Info -> Color(0x3300E2FF)
						is BannerVariant.Warning -> Color(0x32FFE500)
					} ,
					shape = RoundedCornerShape(8.dp)
				) ,
		//cardColors = CardColors(backgroundColor = Color(0xFFE0E0E0)),
		colors = CardDefaults.elevatedCardColors(
				containerColor = when (variant) {
					is BannerVariant.Success -> Color(0x3300FF0A)
					is BannerVariant.Error -> Color(0x33FF0000)
					is BannerVariant.Info -> Color(0x3300E2FF)
					is BannerVariant.Warning -> Color(0x32FFE500)
				}
												),
		) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp)
					.background(Color.Transparent),
				verticalAlignment = Alignment.CenterVertically ,
			) {
			//show the appropriate icon based on the banner variant
			Icon(
					imageVector = when (variant) {
						is BannerVariant.Success -> FeatherIcons.CheckCircle
						is BannerVariant.Error -> FeatherIcons.XCircle
						is BannerVariant.Info -> FeatherIcons.Info
						is BannerVariant.Warning -> FeatherIcons.Info
					} ,
					contentDescription = null ,
					modifier = Modifier
						.size(48.dp)
						.padding(end = 8.dp),
					tint = MaterialTheme.colorScheme.onSurface
				)
			Spacer(modifier = Modifier.width(16.dp))
			Column(
				modifier = Modifier
					.weight(1f)
					.padding(start = 8.dp) ,
				verticalArrangement = Arrangement.Center ,
				) {
				//title
				Text(
					text = title ,
					style = MaterialTheme.typography.titleMedium ,
					color = MaterialTheme.colorScheme.onSurface ,
					)
				//message
				Text(
					text = message ?: "" ,
					style = MaterialTheme.typography.bodyMedium ,
					color = MaterialTheme.colorScheme.onSurface ,
					)
			}
		}

		//dismiss button
		if (isDismissable.value) {
			IconButton(
				onClick = {
					isDismissable.value = false
					onDismiss?.invoke()
				} ,
				) {
				Icon(
					imageVector = FeatherIcons.X ,
					contentDescription = "Dismiss" ,
					tint = MaterialTheme.colorScheme.onPrimary ,
					)
			}
		}
	}

}

@Preview(showBackground = true)
@Composable
fun BannerPreviewWarning()
{
	NimazTheme {
		Banner(
			variant = BannerVariant.Warning ,
			title = "Warning" ,
			message = "This is a warning banner" ,
			)
	}
}

@Preview(showBackground = true)
@Composable
fun BannerPreviewError()
{
	NimazTheme {
		Banner(
			variant = BannerVariant.Error ,
			title = "Error" ,
			message = "This is an error banner" ,
			)
	}
}

@Preview(showBackground = true)
@Composable
fun BannerPreviewSuccess()
{
	NimazTheme {
		Banner(
			variant = BannerVariant.Success ,
			title = "Success" ,
			message = "This is a success banner" ,
			)
	}
}

@Preview(showBackground = true)
@Composable
fun BannerPreviewInfo()
{
	NimazTheme {
		Banner(
			variant = BannerVariant.Info ,
			title = "Info" ,
			message = "This is an info banner" ,
			)
	}
}