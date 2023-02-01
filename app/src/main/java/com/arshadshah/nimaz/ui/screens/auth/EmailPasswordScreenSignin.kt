package com.arshadshah.nimaz.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.viewModel.AuthViewModel
import com.arshadshah.nimaz.ui.components.ui.general.Banner
import com.arshadshah.nimaz.ui.components.ui.general.BannerVariant
import com.arshadshah.nimaz.ui.components.ui.general.EmailTextField
import compose.icons.FeatherIcons
import compose.icons.feathericons.Eye
import compose.icons.feathericons.EyeOff
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailPasswordScreenSignin(
	paddingValues : PaddingValues , onNavigateToSignup : () -> Unit
							 )
{
	val viewmodel = AuthViewModel()
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

	val context = LocalContext.current

	//function to call the login in viewmodel and finish the activity
	val onLogin : () -> Unit = {
		viewmodel.login(email.value , password.value)
		//finish the activity this component is in
		//finish activity
		val activity = context as Activity
		//set the result to ok
		activity.setResult(Activity.RESULT_OK)
		activity.finish()
	}

	//a form to sign in a user with error handling
	Column(
			modifier = Modifier
				.padding(paddingValues)
				.padding(8.dp)
				.fillMaxSize() ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Center
		  ) {
		//title
		Text(
			text = "Sign in",
			style = MaterialTheme.typography.headlineLarge,
			modifier = Modifier.padding(bottom = 16.dp)
		)
		EmailTextField(
			modifier = Modifier
			.fillMaxWidth()
			.padding(top = 16.dp) ,
			email = email,
			onEmailDone = onEmailDone,
			isError = emailErrorMessage.value.isNotEmpty() ,
					  )
		OutlinedTextField(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp , bottom = 16.dp)
					//this is focused when enter is pressed on the email field
					.focusable(enabled = true)
					.focusRequester(passwordFocusRequester),
				singleLine = true,
				value = password.value ,
				onValueChange = { password.value = it } ,
				label = { Text("Password") } ,
				isError = passwordErrorMessage.value.isNotEmpty() ,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password) ,
				//on done sign in
				keyboardActions = KeyboardActions(onDone = { onLogin() }) ,
				//if the hide password is true then show the password text else show the eye icon and hide the password and vice versa
				visualTransformation = if (hidePassword.value) PasswordVisualTransformation() else VisualTransformation.None ,
				trailingIcon = {
					//an icon to show and hide the password
					IconButton(onClick = {
						onPasswordVisibilityChanged()
					}) {
//					/if the password is hidden show the eye icon but only if the password is not empty
						if (hidePassword.value && password.value.isNotEmpty()) {
							Icon(
									imageVector = FeatherIcons.Eye ,
									contentDescription = "Show Password"
								)
						}
						else {
							//if the password is not hidden show the eye icon but only if the password is not empty
							if (password.value.isNotEmpty() && !hidePassword.value) {
								Icon(
										imageVector = FeatherIcons.EyeOff ,
										contentDescription = "Hide Password"
									)
							}
						}
					}
				}
						 )

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
						onLogin()
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
			Text("Sign In", style = MaterialTheme.typography.titleLarge)
		}

		Row(
				modifier = Modifier
					.padding(16.dp)
					.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			Text(text = "Don't have an account? ")
			Button(
					shape = MaterialTheme.shapes.medium ,
					onClick = {
				onNavigateToSignup()
			}
				  ) {
				Text("Sign Up",style = MaterialTheme.typography.titleLarge)
			}
		}
	}

}