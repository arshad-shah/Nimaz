package com.arshadshah.nimaz.ui.components.ui.prayerTimes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ExternalLink
import compose.icons.feathericons.Github
import compose.icons.feathericons.Linkedin
import compose.icons.feathericons.Mail

@Composable
fun FeatureCardUI()
{

	val context = LocalContext.current

	ElevatedCard() {
		Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceEvenly,
				verticalAlignment = Alignment.CenterVertically) {
			//website link
			LinkButton(
					icon = {
						Icon(
								imageVector = FeatherIcons.ExternalLink ,
								contentDescription = "Portfolio Website Link" ,
							)
					} ,
					onClick = {
						val intent = Intent(Intent.ACTION_VIEW , Uri.parse("https://arshadshah.com"))
						context.startActivity(intent)
					}
					  )

			//linkdIn link
			LinkButton(
					icon = {
						Icon(
								imageVector = FeatherIcons.Linkedin ,
								contentDescription = "LinkedIn Link" ,
							)
					} ,
					onClick = {
						val intent = Intent(Intent.ACTION_VIEW , Uri.parse("https://www.linkedin.com/in/arshadshah"))
						context.startActivity(intent)
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
						val intent = Intent(Intent.ACTION_SENDTO , Uri.parse("mailto: info@arshadshah.com"))
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
					onClick = {
						//open github link
						val urlIntent = Intent(
								Intent.ACTION_VIEW ,
								Uri.parse("https://github.com/arshad-shah")
											  )
						context.startActivity(urlIntent)
					}
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
			modifier = Modifier.padding(4.dp).size(48.dp) ,
			onClick = onClick,
			colors = IconButtonDefaults.filledIconButtonColors())
	{
		icon()
	}
}