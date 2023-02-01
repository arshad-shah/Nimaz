package com.arshadshah.nimaz.ui.components.ui.general

import android.util.Patterns
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import compose.icons.FeatherIcons
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.Mail
import kotlinx.coroutines.launch


/**
 * A text field for email input with error handling and validation,
 * it provides a [onEmailDone] callback to be called when the enter key is pressed on the keyboard
 * @param email the email to be validated
 * @param onEmailDone the callback to be called when the enter key is pressed on the keyboard
 * @param isError a custom error flag to be used to show error messages
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailTextField(
	modifier : Modifier ,
	email : MutableState<String> ,
	onEmailDone : () -> Unit ,
	isError : Boolean,
				  )
{
	val coroutineScope = rememberCoroutineScope()
	val isEmailValid = remember { mutableStateOf(true) }
	val onEmailChanged : (String) -> Unit = {
		email.value = it
		coroutineScope.launch {
			isEmailValid.value = emailIsValid(it)
		}
	}
	OutlinedTextField(
			singleLine = true ,
			modifier = modifier,
			isError = isError || !isEmailValid.value,
			value = email.value ,
			onValueChange = { onEmailChanged(it) } ,
			label = { Text("Email") } ,
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email) ,
			//on done focus on password
			keyboardActions = KeyboardActions(onDone = { onEmailDone() }) ,
			leadingIcon = {
						  //email icon
						  Icon(
								  imageVector = FeatherIcons.Mail ,
								  contentDescription = "Email" ,
								  )
			},
			trailingIcon = {
				if (isEmailValid.value && email.value.isNotEmpty())
				{
					//if email is valid show a check icon
					Icon(
							imageVector = FeatherIcons.CheckCircle ,
							contentDescription = "Email is valid" ,
							tint = Color.Green ,
							)
				}
			}
					 )
}

fun emailIsValid(email : String) : Boolean
{
	return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
