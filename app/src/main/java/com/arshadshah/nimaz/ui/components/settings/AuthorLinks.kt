package com.arshadshah.nimaz.ui.components.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun AuthorLinks()
{

	val context = LocalContext.current

	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.padding(8.dp)
				.height(64.dp) ,
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight() ,
				horizontalArrangement = Arrangement.SpaceEvenly ,
				verticalAlignment = Alignment.CenterVertically ,
		   ) {
			//website link
			FilledIconButton(
					modifier = Modifier.size(48.dp) ,
					shape = CircleShape ,
					colors = IconButtonDefaults.filledIconButtonColors(
							containerColor = MaterialTheme.colorScheme.primary ,
							contentColor = MaterialTheme.colorScheme.onPrimary ,
																	  ) ,
					onClick = {
						val intent =
							Intent(Intent.ACTION_VIEW , Uri.parse("https://arshadshah.com"))
						context.startActivity(intent)
					} ,
					content = {
						Icon(
								modifier = Modifier.size(24.dp) ,
								painter = painterResource(id = R.drawable.external_link_icon) ,
								contentDescription = "Portfolio Website Link" ,
							)
					}
							)

			//linkedIn link
			FilledIconButton(
					modifier = Modifier.size(48.dp) ,
					shape = CircleShape ,
					colors = IconButtonDefaults.filledIconButtonColors(
							containerColor = MaterialTheme.colorScheme.primary ,
							contentColor = MaterialTheme.colorScheme.onPrimary ,
																	  ) ,
					onClick = {
						val intent = Intent(
								Intent.ACTION_VIEW ,
								Uri.parse("https://www.linkedin.com/in/arshadshah")
										   )
						context.startActivity(intent)
					} ,
					content = {
						Icon(
								modifier = Modifier.size(24.dp) ,
								painter = painterResource(id = R.drawable.linkedin_icon) ,
								contentDescription = "LinkedIn Link" ,
							)
					}
							)
			//email link
			FilledIconButton(
					modifier = Modifier.size(48.dp) ,
					shape = CircleShape ,
					colors = IconButtonDefaults.filledIconButtonColors(
							containerColor = MaterialTheme.colorScheme.primary ,
							contentColor = MaterialTheme.colorScheme.onPrimary ,
																	  ) ,
					content = {
						Icon(
								modifier = Modifier.size(24.dp) ,
								painter = painterResource(id = R.drawable.mail_icon) ,
								contentDescription = "Email Link" ,
							)
					} ,
					onClick = {
						val intent =
							Intent(Intent.ACTION_SENDTO , Uri.parse("mailto: info@arshadshah.com"))
						context.startActivity(intent)
					}
							)
			FilledIconButton(
					modifier = Modifier.size(48.dp) ,
					shape = CircleShape ,
					colors = IconButtonDefaults.filledIconButtonColors(
							containerColor = MaterialTheme.colorScheme.primary ,
							contentColor = MaterialTheme.colorScheme.onPrimary ,
																	  ) ,
					content = {
						Icon(
								modifier = Modifier.size(24.dp) ,
								painter = painterResource(id = R.drawable.github_icon) ,
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


@Preview
@Composable
fun AuthorLinksPreview()
{
	NimazTheme {
		AuthorLinks()
	}
}