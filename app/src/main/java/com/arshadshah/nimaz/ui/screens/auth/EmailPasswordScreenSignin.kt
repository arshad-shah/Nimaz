package com.arshadshah.nimaz.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.data.remote.viewModel.AuthViewModel
import com.arshadshah.nimaz.ui.components.ui.general.Banner
import com.arshadshah.nimaz.ui.components.ui.general.BannerVariant
import com.arshadshah.nimaz.ui.components.ui.general.EmailTextField
import com.arshadshah.nimaz.ui.components.ui.general.PasswordTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailPasswordScreenSignin(
	paddingValues : PaddingValues ,
	onNavigateToSignup : () -> Unit ,
	navController : NavHostController ,
	onNavigateToPasswordReset : () -> Unit
							 )
{
	val viewmodel = AuthViewModel()
	val loginUiState = remember{ viewmodel.loginUiState }.collectAsState()

	val email = remember { mutableStateOf("") }
	val password = remember { mutableStateOf("") }

	//success and error messages
	val emailErrorMessage = remember { mutableStateOf("") }
	val passwordErrorMessage = remember { mutableStateOf("") }
	val errorMessage = remember { mutableStateOf("") }

	val hidePassword = remember { mutableStateOf(true) }
	val coroutineScope = rememberCoroutineScope()
	val onPasswordVisibilityChanged : () -> Unit = {
		coroutineScope.launch {
			hidePassword.value = !hidePassword.value
		}
	}
	//password focuser when enter is pressed on email
	val passwordFocusRequester = remember { FocusRequester() }
	val onEmailDone : () -> Unit = {
		passwordFocusRequester.requestFocus()
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
			email = email,
			onEmailDone = onEmailDone,
			isError = emailErrorMessage.value.isNotEmpty() ,
					  )
		PasswordTextField(
				label = "Password" ,
				password = password.value ,
				showRequirements = false ,
				onPasswordChange =  { password.value = it } ,
				passwordErrorMessage =  passwordErrorMessage.value ,
				hidePassword =  hidePassword.value ,
				onPasswordVisibilityChanged = { onPasswordVisibilityChanged() } ,
				passwordFocusRequester = passwordFocusRequester ,
				onPasswordDone = { viewmodel.login(email.value , password.value) } ,
				error = passwordErrorMessage.value.isNotEmpty() ,
				signup = false ,
						 )

		//forgot password
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp) ,
				horizontalArrangement = Arrangement.Start ,
			) {
			TextButton(
					onClick = { onNavigateToPasswordReset() } ,
					content = { Text(text = "Forgot Password?" , style = MaterialTheme.typography.titleMedium) } ,
					  )
		}

		//error message
		if (errorMessage.value.isNotEmpty() || emailErrorMessage.value.isNotEmpty() || passwordErrorMessage.value.isNotEmpty()) {
			Banner(
		   variant = BannerVariant.Error ,
		   title= "Error" ,
		   message = errorMessage.value + emailErrorMessage.value + passwordErrorMessage.value ,
				  )
		}
		Button(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp , bottom = 16.dp)
					.size(48.dp) ,
				shape = MaterialTheme.shapes.medium ,
				onClick = {
					//if the email and password are not empty then sign in the user
					if (email.value.isNotEmpty() && password.value.isNotEmpty()){
						viewmodel.login(email.value , password.value)
						navController.navigateUp()
						//finish the activity
					}else if (email.value.isEmpty()){
						//if the email or password is empty then show an error message
						emailErrorMessage.value = "Email cannot be empty"
					}else if (password.value.isEmpty()){
						passwordErrorMessage.value = "Password cannot be empty"
					}else{
						errorMessage.value = "Email and password cannot be empty"
					}
				}
			  ) {
			Row(
					modifier = Modifier.fillMaxSize() ,
					horizontalArrangement = Arrangement.Center ,
					verticalAlignment = Alignment.CenterVertically ,
			   ) {
				//else show the sign in text
				Text(text = "Sign In" , style = MaterialTheme.typography.titleMedium)
			}
		}

		Row(
				modifier = Modifier
					.padding(16.dp)
					.fillMaxWidth(),
				horizontalArrangement = Arrangement.Start ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			Text(text = "Don't have an account? ", style = MaterialTheme.typography.titleMedium)
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