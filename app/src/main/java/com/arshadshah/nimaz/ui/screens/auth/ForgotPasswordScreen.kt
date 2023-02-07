package com.arshadshah.nimaz.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.data.remote.viewModel.AuthViewModel
import com.arshadshah.nimaz.ui.components.ui.general.EmailTextField

@Composable
fun ForgotPasswordScreen(
	paddingValues : PaddingValues ,
	onNavigateToSignin : () -> Unit ,
	navController : NavHostController ,
	onNavigateToSignup : () -> Unit
						)
{

	val email = remember { mutableStateOf("") }
	val viewmodel = AuthViewModel()

	//success and error messages
	val emailErrorMessage = remember { mutableStateOf("") }
	val errorMessage = remember { mutableStateOf("") }

	//function to call the login in viewmodel and finish the activity
	val onResetPassword : () -> Unit = {
		viewmodel.sendPasswordResetEmail(email.value)
		navController.navigateUp()
	}

	//a form to sign in a user with error handling
	Column(
			modifier = Modifier
				.padding(paddingValues)
				.padding(8.dp)
				.fillMaxSize() ,
			horizontalAlignment = Alignment.CenterHorizontally ,
		  ) {
		EmailTextField(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp) ,
				email = email ,
				onEmailDone = {
					onResetPassword()
				} ,
				isError = emailErrorMessage.value.isNotEmpty() ,
					  )

		Button(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp , bottom = 16.dp)
					.size(48.dp) ,
				shape = MaterialTheme.shapes.medium ,
				onClick = {
						onResetPassword()
				}
			  ) {
			Text("Submit")
		}

		Row(
				modifier = Modifier
					.padding(16.dp)
					.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			Text(text = "Don't have an account? ")
			TextButton(
					shape = MaterialTheme.shapes.medium ,
					onClick = {
						onNavigateToSignup()
					}
					  ) {
				Text("Sign Up",style = MaterialTheme.typography.titleMedium)
			}
		}

	}

}