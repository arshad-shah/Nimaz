package com.arshadshah.nimaz.ui.components.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.LogOut
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Profile(
	user : State<SettingsViewModel.UserState> ,
	logout : () -> Unit
		   )
{
	val context = LocalContext.current

	when (val currentUser = user.value)
	{
		is SettingsViewModel.UserState.Error ->
		{
			Toasty.error(context , "Error" , Toast.LENGTH_SHORT , true).show()
		}
		is SettingsViewModel.UserState.Success ->
		{
			ElevatedCard(
					modifier = Modifier
						.fillMaxWidth()
						.padding(8.dp)
						.shadow(8.dp) ,
					onClick = {
						logout()
					}
						) {
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(8.dp) ,
						verticalAlignment = Alignment.CenterVertically ,
						horizontalArrangement = Arrangement.SpaceBetween
				   ) {
					Text(text = currentUser.data?.email!!, style = MaterialTheme.typography.titleMedium)
					//logout icon button
					IconButton(
							onClick = {
								logout()
								Toasty.success(context , "Logged out successfully" , Toast.LENGTH_SHORT , true).show()
							}
							  ) {
						Icon(
								imageVector = FeatherIcons.LogOut ,
								contentDescription = "Logout"
							)
					}
				}
			}
		}
		is SettingsViewModel.UserState.Loading ->
		{
			Toasty.info(context , "Loading" , Toast.LENGTH_SHORT , true).show()
		}
	}
}