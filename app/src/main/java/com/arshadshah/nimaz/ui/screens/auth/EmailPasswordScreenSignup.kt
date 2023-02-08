package com.arshadshah.nimaz.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.data.remote.viewModel.AuthViewModel
import com.arshadshah.nimaz.ui.components.ui.general.Banner
import com.arshadshah.nimaz.ui.components.ui.general.BannerVariant
import com.arshadshah.nimaz.ui.components.ui.general.EmailTextField
import com.arshadshah.nimaz.ui.components.ui.general.PasswordTextField
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.auth.AuthDataSanitizers
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailPasswordScreenSignup(paddingValues : PaddingValues , navController : NavHostController , )
{

	val viewmodel = AuthViewModel()

	val dataSanitizer = AuthDataSanitizers()

	val context = LocalContext.current

	val email = remember { mutableStateOf("") }
	val role = listOf("Student" , "Teacher")
	val selectedRole = remember { mutableStateOf(0) }
	val menuExpanded = remember { mutableStateOf(false) }

	//name
	val name = remember { mutableStateOf("") }


	val password = remember { mutableStateOf("") }
	//confirm password
	val confirmPassword = remember { mutableStateOf("") }

	//password is strong
	val passwordStrength = remember { mutableStateOf("") }



	//success and error messages
	val errorMessage = remember { mutableStateOf("") }

	val coroutineScope = rememberCoroutineScope()

	//hide and show password
	val hidePassword = remember { mutableStateOf(true) }
	val hideConfirmPassword = remember { mutableStateOf(true) }

	val onPasswordVisibilityChanged : () -> Unit = {
		coroutineScope.launch {
			hidePassword.value = !hidePassword.value
		}
	}

	val onConfirmPasswordVisibilityChanged : () -> Unit = {
		coroutineScope.launch {
			hideConfirmPassword.value = !hideConfirmPassword.value
		}
	}

	//functions to check if password is strong
	val checkPasswordStrength : (String) -> Unit = {
		coroutineScope.launch {
			passwordStrength.value = dataSanitizer.passwordStrength(it)
		}
	}

	//each time the password changes check if it is strong
	LaunchedEffect(password.value) {
		checkPasswordStrength(password.value)
	}

//	password focuser when enter is pressed on email
	val passwordFocusRequester = remember { FocusRequester() }
	val confirmPasswordFocusRequester = remember { FocusRequester() }

	//a function to signup and finish activity
	val signup = {
		viewmodel.createAccount(email.value, password.value, name.value, role[selectedRole.value])
		navController.navigateUp()
	}

	//a form to sign up a user with error handling for signup
	Column(modifier = Modifier
		.padding(paddingValues)
		.padding(8.dp)
		.fillMaxSize() ,
		   horizontalAlignment = Alignment.CenterHorizontally) {
		Card(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp)
					.clickable { menuExpanded.value = !menuExpanded.value } ,
			) {
			Row(modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp) ,
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically) {
				Text(text = role[selectedRole.value] )
				Icon(imageVector = Icons.Default.ArrowDropDown , contentDescription = "Role" )
			}
		}
		//dropdown menu for role
		DropdownMenu(
				modifier = Modifier
					.padding(top = 16.dp) ,
				expanded = menuExpanded.value ,
				onDismissRequest = { } ,
					) {
			role.forEachIndexed { index , role ->
				DropdownMenuItem(onClick = {
					selectedRole.value = index
					menuExpanded.value = false
				}, text = { Text(text = role) })
			}
		}
		EmailTextField(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp) ,
				email = email,
				onEmailDone = { passwordFocusRequester.requestFocus() } ,
				isError = errorMessage.value.isNotEmpty()
					  )
		
		//outlined text field for name
		OutlinedTextField(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp) ,
				value = name.value ,
				onValueChange = { name.value = it } ,
				label = { Text(text = "Name" ) } ,
				singleLine = true ,
				leadingIcon = { Icon(imageVector = Icons.Default.Person , contentDescription = "Name") } ,
						 )
		PasswordTextField(
				label = "Password" ,
				password = password.value,
				showRequirements = true,
				error = false,
				onPasswordChange =  { password.value = it } ,
				passwordErrorMessage =  errorMessage.value ,
				hidePassword =  hidePassword.value ,
				onPasswordVisibilityChanged = { onPasswordVisibilityChanged() } ,
				passwordFocusRequester = passwordFocusRequester ,
				onPasswordDone = { confirmPasswordFocusRequester.requestFocus() } ,
				signup = true,
						 )

		PasswordTextField(
				label = "Confirm Password" ,
				password = confirmPassword.value ,
				showRequirements = false,
				error = password.value != confirmPassword.value && confirmPassword.value.isNotEmpty(),
				helperText = if (password.value == confirmPassword.value && confirmPassword.value.isNotEmpty()) "Passwords match" else if(password.value != confirmPassword.value && confirmPassword.value.isNotEmpty()) "Passwords do not match" else "",
				onPasswordChange =  { confirmPassword.value = it } ,
				passwordErrorMessage =  errorMessage.value ,
				hidePassword = hideConfirmPassword.value ,
				onPasswordVisibilityChanged = { onConfirmPasswordVisibilityChanged() } ,
				passwordFocusRequester = confirmPasswordFocusRequester ,
				onPasswordDone = {
								 if( email.value.isNotEmpty() && password.value.isNotEmpty() && confirmPassword.value.isNotEmpty() && password.value == confirmPassword.value){
									 signup()
								 }else{
									 Toasty.error(context, "Please fill all the fields correctly", Toasty.LENGTH_SHORT).show()
								 }
				} ,
				signup = true,
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
						signup()
		}, enabled = email.value.isNotEmpty() && password.value.isNotEmpty() && confirmPassword.value.isNotEmpty() && password.value == confirmPassword.value
			  ) {
			Text(text = "Sign Up",style = MaterialTheme.typography.titleMedium)
		}
	}
}

//preview
@Preview(showBackground = true)
@Composable
fun DefaultPreviewSignup() {
	NimazTheme {
		EmailPasswordScreenSignup(
				paddingValues = PaddingValues(16.dp) ,
				navController = rememberNavController()
								 )
	}
}