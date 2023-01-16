package com.arshadshah.nimaz.ui.components.ui.prayerTimes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.ui.icons.NineNine
import com.arshadshah.nimaz.ui.components.ui.icons.PlusMinusTasbih
import compose.icons.FeatherIcons
import compose.icons.feathericons.Github
import compose.icons.feathericons.Mail


@Composable
fun FeatureCardUI(onNavigateToTasbihScreen : (String) -> Unit , onNavigateToNames : () -> Unit)
{

	val context = LocalContext.current

	ElevatedCard {
		Row(
				modifier = Modifier.fillMaxWidth() ,
				horizontalArrangement = Arrangement.SpaceEvenly ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			//website link
			LinkButton(
					icon = {
						Icon(
								imageVector = Icons.PlusMinusTasbih ,
								contentDescription = "Tasbih" ,
							)
					} ,
					onClick = {
						onNavigateToTasbihScreen(" ")
					}
					  )

			//linkdIn link
			LinkButton(
					icon = {
						Icon(
								modifier = Modifier.size(24.dp) ,
								imageVector = Icons.NineNine ,
								contentDescription = "Names of Allah" ,
							)
					} ,
					onClick = {
						onNavigateToNames()
					}
					  )
			//email link
			LinkButton(
					icon = {
						Icon(
								imageVector = FeatherIcons.Mail ,
								contentDescription = "Email Link" ,
							)
					} ,
					onClick = {
						val intent =
							Intent(Intent.ACTION_SENDTO , Uri.parse("mailto: info@arshadshah.com"))
						context.startActivity(intent)
					}
					  )
			LinkButton(
					icon = {
						Icon(
								imageVector = FeatherIcons.Github ,
								contentDescription = "Github"
							)
					} ,
					onClick = {}
					  )

		}
	}
}

@Composable
fun LinkButton(
	icon : @Composable () -> Unit ,
	onClick : () -> Unit ,
			  )
{
	IconButton(
			modifier = Modifier
				.padding(4.dp)
				.size(48.dp) ,
			onClick = onClick ,
			colors = IconButtonDefaults.filledIconButtonColors()
			  )
	{
		icon()
	}
}