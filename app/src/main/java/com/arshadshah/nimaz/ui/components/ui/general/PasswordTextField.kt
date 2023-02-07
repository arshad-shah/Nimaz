package com.arshadshah.nimaz.ui.components.ui.general

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordTextField(
	label : String ,
	password : String ,
	helperText : String? = null ,
	showRequirements : Boolean ,
	onPasswordChange : (String) -> Unit ,
	passwordErrorMessage : String ,
	hidePassword : Boolean ,
	onPasswordVisibilityChanged : () -> Unit ,
	passwordFocusRequester : FocusRequester ,
	onPasswordDone : () -> Unit ,
	error : Boolean,
	signup : Boolean
					 )
{

	if(signup){

		val isError = remember { mutableStateOf(false) }

		OutlinedTextField(
				colors = if(!isError.value && password.isNotEmpty())
				{
					TextFieldDefaults.outlinedTextFieldColors(
							focusedBorderColor = Color(0xFF025314) ,
							unfocusedBorderColor = Color(0xFF025314) ,
															 )
				}
				else
				{
					TextFieldDefaults.outlinedTextFieldColors()
				} ,
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp)
					//this is focused when enter is pressed on the email field
					.focusable(enabled = true)
					.focusRequester(passwordFocusRequester) ,
				leadingIcon = { Icon(imageVector = FeatherIcons.Lock, contentDescription = "Password",
									 tint = if (isError.value && password.isNotEmpty() || error) MaterialTheme.colorScheme.error else if(!isError.value && password.isEmpty() || error) MaterialTheme.colorScheme.onSurface else Color(0xFF025314))

				} ,
				singleLine = true ,
				value = password ,
				onValueChange = { onPasswordChange(it) } ,
				label = { Text(label, color = if (isError.value && password.isNotEmpty() || error) MaterialTheme.colorScheme.error else if(!isError.value && password.isEmpty() || error) MaterialTheme.colorScheme.onSurface else Color(0xFF025314)) } ,
				isError = isError.value  || error ,
				supportingText = {
					if (passwordErrorMessage.isNotEmpty()) {
						Text(
								text = passwordErrorMessage ,
								color = MaterialTheme.colorScheme.error
							)
					} else {
						if (helperText != null)
						{
							Text(text = helperText ,color = if (isError.value && password.isNotEmpty() || error) MaterialTheme.colorScheme.error else if(!isError.value && password.isEmpty() || error) MaterialTheme.colorScheme.onSurface else Color(0xFF025314))
						}
					}
				} ,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password) ,
				//on done sign in
				keyboardActions = KeyboardActions(onDone = { onPasswordDone() }) ,
				//if the hide password is true then show the password text else show the eye icon and hide the password and vice versa
				visualTransformation = if (hidePassword) PasswordVisualTransformation() else VisualTransformation.None ,
				trailingIcon = {
					//an icon to show and hide the password
					IconButton(onClick = {
						onPasswordVisibilityChanged()
					}) {
//					/if the password is hidden show the eye icon but only if the password is not empty
						if (hidePassword && password.isNotEmpty()) {
							Icon(
									imageVector = FeatherIcons.Eye ,
									contentDescription = "Show Password",
									tint = if (isError.value && password.isNotEmpty() || error) MaterialTheme.colorScheme.error else Color(0xFF025314)
								)
						}
						else {
							//if the password is not hidden show the eye icon but only if the password is not empty
							if (password.isNotEmpty() && !hidePassword) {
								Icon(
										imageVector = FeatherIcons.EyeOff ,
										contentDescription = "Hide Password",
										tint = if (isError.value && password.isNotEmpty() || error) MaterialTheme.colorScheme.error else Color(0xFF025314)
									)
							}
						}
					}
				}
						 )

		//showRequirements
		if (showRequirements && password.isNotEmpty())
		{
			PasswordRequirements(password = password, isError = isError)
		}
	}else{
		OutlinedTextField(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 16.dp)
					//this is focused when enter is pressed on the email field
					.focusable(enabled = true)
					.focusRequester(passwordFocusRequester) ,
				leadingIcon = { Icon(imageVector = FeatherIcons.Lock, contentDescription = "Password") } ,
				singleLine = true ,
				value = password ,
				onValueChange = { onPasswordChange(it) } ,
				label = { Text(label)} ,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password) ,
				//on done sign in
				keyboardActions = KeyboardActions(onDone = { onPasswordDone() }) ,
				//if the hide password is true then show the password text else show the eye icon and hide the password and vice versa
				visualTransformation = if (hidePassword) PasswordVisualTransformation() else VisualTransformation.None ,
				trailingIcon = {
					//an icon to show and hide the password
					IconButton(onClick = {
						onPasswordVisibilityChanged()
					}) {
//					/if the password is hidden show the eye icon but only if the password is not empty
						if (hidePassword && password.isNotEmpty()) {
							Icon(
									imageVector = FeatherIcons.Eye ,
									contentDescription = "Show Password",
								)
						}
						else {
							//if the password is not hidden show the eye icon but only if the password is not empty
							if (password.isNotEmpty() && !hidePassword) {
								Icon(
										imageVector = FeatherIcons.EyeOff ,
										contentDescription = "Hide Password",
									)
							}
						}
					}
				}
						 )
	}
}

//showRequirements
//a composable to show the password requirements and check if the password meets the requirements then show a tick else show a cross
@Composable
fun PasswordRequirements(password : String , isError : MutableState<Boolean>)
{
	//password requirements
	val passwordRequirements = listOf(
			"Password must be at least 8 characters" ,
			"Password must contain at least one number" ,
			"Password must contain at least one special character" ,
			"Password must contain at least one uppercase letter" ,
			"Password must contain at least one lowercase letter"
									 )

	//cehck if meet requirements is true fro all the requirements
	val meetsAllRequirements = passwordRequirements.all { meetsPasswordRequirement(it, password) }
	isError.value = !meetsAllRequirements
	Column(
			modifier = Modifier.fillMaxWidth()
		  ) {
		//loop through the password requirements
		passwordRequirements.forEach { requirement ->
			//check if the password meets the requirement
			val meetsRequirement = meetsPasswordRequirement(requirement, password)

			//show the requirement and a tick or cross depending on if the password meets the requirement
			Row(
					modifier = Modifier.fillMaxWidth() ,
					verticalAlignment = Alignment.CenterVertically ,
					horizontalArrangement = Arrangement.Start
			   ) {
				Icon(
						imageVector = if (meetsRequirement) FeatherIcons.CheckCircle else FeatherIcons.XCircle ,
						contentDescription = if (meetsRequirement) "Meets Requirement" else "Does not meet requirement",
						tint = if (meetsRequirement) Color(0xFF025314)else MaterialTheme.colorScheme.error,
						modifier = Modifier.size(16.dp)
					)
				Spacer(modifier = Modifier.padding(start = 8.dp))
				Text(text = requirement, color = if (meetsRequirement) Color(0xFF025314) else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
			}
		}
	}
}

//function to check if the password meets the requirement
fun meetsPasswordRequirement(
	requirement : String ,
	password : String ,
							) : Boolean
{
	//check if the password meets the requirement
	return when (requirement) {
		"Password must be at least 8 characters" -> password.length >= 8
		"Password must contain at least one number" -> password.matches(Regex(".*\\d.*"))
		"Password must contain at least one special character" -> password.matches(Regex(".*[!@#\$%^&*()_+].*"))
		"Password must contain at least one uppercase letter" -> password.matches(Regex(".*[A-Z].*"))
		"Password must contain at least one lowercase letter" -> password.matches(Regex(".*[a-z].*"))
		else -> false
	}
}