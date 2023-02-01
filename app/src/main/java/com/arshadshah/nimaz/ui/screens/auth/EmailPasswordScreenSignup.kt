package com.arshadshah.nimaz.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.viewModel.AuthViewModel
import com.arshadshah.nimaz.ui.components.ui.general.Banner
import com.arshadshah.nimaz.ui.components.ui.general.BannerVariant
import com.arshadshah.nimaz.ui.components.ui.general.EmailTextField
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.auth.AuthDataSanitizers
import compose.icons.FeatherIcons
import compose.icons.feathericons.Eye
import compose.icons.feathericons.EyeOff
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailPasswordScreenSignup(paddingValues : PaddingValues,)
{

	val viewmodel = AuthViewModel()

	val dataSanitizer = AuthDataSanitizers()

	val email = remember { mutableStateOf("") }
	val password = remember { mutableStateOf("") }
	//confirm password
	val confirmPassword = remember { mutableStateOf("") }

	val isEmailValid = remember { mutableStateOf(true) }

	//passwords match
	val passwordsMatch = remember { mutableStateOf(true) }

	//password is strong
	val passwordStrength = remember { mutableStateOf("") }

	//success and error messages
	val errorMessage = remember { mutableStateOf("") }

	val coroutineScope = rememberCoroutineScope()
	val onConfirmPasswordChanged : (String) -> Unit = {
		confirmPassword.value = it
		coroutineScope.launch {
			passwordsMatch.value = dataSanitizer.passwordsMatch(password.value, it)
		}
	}

	val onPasswordChanged : (String) -> Unit = {
		password.value = it
		coroutineScope.launch {
			passwordStrength.value = dataSanitizer.passwordStrength(it)
		}
	}

	//hide and show password
	val hidePassword = remember { mutableStateOf(true) }
	val hideConfirmPassword = remember { mutableStateOf(true) }

//	password focuser when enter is pressed on email
	val passwordFocusRequester = remember { FocusRequester() }
	val confirmPasswordFocusRequester = remember { FocusRequester() }

	val context = LocalContext.current

	//a function to signup and finish activity
	val signup = {
		viewmodel.createAccount(email.value, password.value)
		//finish activity
		val activity = context as Activity
		//set the result to ok
		activity.setResult(Activity.RESULT_OK)
		activity.finish()
	}

	//a form to sign up a user with error handling for signup
	Column(modifier = Modifier
		.padding(paddingValues)
		.padding(8.dp)
		.fillMaxSize() ,
		   horizontalAlignment = Alignment.CenterHorizontally ,
		   verticalArrangement = Arrangement.Center) {
		//title
		Text(
			text = "Sign Up",
			style = MaterialTheme.typography.headlineLarge,
			modifier = Modifier.padding(bottom = 16.dp)
		)

		EmailTextField(
				modifier = Modifier
			.fillMaxWidth()
			.padding(top = 16.dp) ,
				email = email,
				onEmailDone = { passwordFocusRequester.requestFocus() } ,
				isError =!isEmailValid.value)
		OutlinedTextField(
				singleLine = true,
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp , bottom = 16.dp)
					.focusable(enabled = true)
					.focusRequester(passwordFocusRequester),
				value = password.value ,
				onValueChange = { onPasswordChanged(it) } ,
				label = { Text("Password") } ,
				isError = errorMessage.value.isNotEmpty() ,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password) ,
				//on done focus on password
				keyboardActions = KeyboardActions(onDone = { confirmPasswordFocusRequester.requestFocus() }) ,
				visualTransformation = if (hidePassword.value) PasswordVisualTransformation() else VisualTransformation.None ,
				trailingIcon = {
				//an icon to show and hide the password
				IconButton(onClick = {
					hidePassword.value = !hidePassword.value
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

		//show the password strength
		if (passwordStrength.value.isNotEmpty()) {
			Banner(title = passwordStrength.value, variant = BannerVariant.Info)
		}

		OutlinedTextField(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp , bottom = 16.dp)
					//this is focused when enter is pressed on the email field
					.focusable(enabled = true)
					.focusRequester(confirmPasswordFocusRequester),
				singleLine = true,
			value = confirmPassword.value,
			onValueChange = { onConfirmPasswordChanged(it) },
			label = { Text("Confirm Password") },
			isError = errorMessage.value.isNotEmpty() || !passwordsMatch.value,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password) ,
				//on done focus on password
				keyboardActions = KeyboardActions(onDone = { signup() }) ,
			visualTransformation = if (hideConfirmPassword.value) PasswordVisualTransformation() else VisualTransformation.None,
			trailingIcon = {
				//an icon to show and hide the password
				IconButton(onClick = {
					hideConfirmPassword.value = !hideConfirmPassword.value
				}) {
					//if the password is hidden show the eye icon but only if the password is not empty
					if (hideConfirmPassword.value && confirmPassword.value.isNotEmpty()) {
						Icon(
							imageVector = FeatherIcons.Eye,
							contentDescription = "Show Password"
						)
					}else{
						//if the password is not hidden show the eye icon but only if the password is not empty
						if (!hideConfirmPassword.value && confirmPassword.value.isNotEmpty()) {
							Icon(
								imageVector = FeatherIcons.EyeOff,
								contentDescription = "Show Password"
							)
						}
					}
				}
			}
		)

		if (errorMessage.value.isNotEmpty()) {
			Banner(title = errorMessage.value, variant = BannerVariant.Error)
		}
		//sign up button
		Button(modifier = Modifier
			.fillMaxWidth()
			.padding(top = 16.dp , bottom = 16.dp)
			.size(48.dp) ,
			   shape = MaterialTheme.shapes.medium ,
				onClick = {
					if(email.value.isNotEmpty() && password.value.isNotEmpty() && confirmPassword.value.isNotEmpty() && passwordsMatch.value){
						//use signup function
						signup()
					}else if(email.value.isEmpty()){
						errorMessage.value = "Email cannot be empty"
					}else if(password.value.isEmpty()){
						errorMessage.value = "Password cannot be empty"
					}else if(confirmPassword.value.isEmpty()){
						errorMessage.value = "Confirm Password cannot be empty"
					}else if(!passwordsMatch.value){
						errorMessage.value = "Passwords do not match"
					}else{
						errorMessage.value = "Something went wrong"
					}
		}) {
			Text(text = "Sign Up")
		}
	}
}

//preview
@Preview(showBackground = true)
@Composable
fun DefaultPreviewSignup() {
	NimazTheme {
		EmailPasswordScreenSignup(paddingValues = PaddingValues(16.dp))
	}
}